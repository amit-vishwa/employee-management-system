#!/usr/bin/env python3
"""CI-only end-to-end smoke check for the disposable Docker Compose stack."""

import json
import os
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

AUTH = "http://localhost:8082"
EMPLOYEE = "http://localhost:8081"
NOTIFICATION = "http://localhost:8083"


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def api(base, path, *, method="GET", body=None, token=None, expected=200):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"} if body is not None else {}

    if token is not None:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(
        base + path,
        data=data,
        headers=headers,
        method=method,
    )

    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            status = response.status
            content_type = response.headers.get("Content-Type", "")
            response_body = response.read()
    except urllib.error.HTTPError as error:
        status = error.code
        content_type = error.headers.get("Content-Type", "")
        response_body = error.read()

    require(
        status == expected,
        f"{method} {path}: expected HTTP {expected}, received {status}",
    )

    if response_body and (
        "application/json" in content_type or "+json" in content_type
    ):
        return json.loads(response_body)

    return None


def mysql_query(sql):
    # SQL is passed as an argument, not interpolated into the shell command.
    # The container supplies its own database username and password.
    result = subprocess.run(
        [
            "docker", "compose", "exec", "-T", "mysql-auth",
            "sh", "-c",
            'MYSQL_PWD="$MYSQL_PASSWORD" mysql '
            '-u "$MYSQL_USER" "$MYSQL_DATABASE" '
            '--batch --skip-column-names -e "$1"',
            "sh", sql,
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    require(result.returncode == 0, "Could not update the disposable CI user")
    return result.stdout.strip()


def login(username, password):
    response = api(
        AUTH,
        "/api/v1/auth/login",
        method="POST",
        body={"username": username, "password": password},
    )
    token = response.get("token") if isinstance(response, dict) else None
    require(isinstance(token, str) and bool(token), "Login returned no JWT")
    return token


def wait_for_notification(email):
    deadline = time.monotonic() + 30

    while time.monotonic() < deadline:
        result = subprocess.run(
            [
                "docker", "compose", "logs",
                "--no-color", "--since=5m", "notification-service",
            ],
            capture_output=True,
            text=True,
            check=False,
        )
        require(result.returncode == 0, "Could not inspect notification logs")

        if "Welcome email would be sent" in result.stdout and email in result.stdout:
            return

        time.sleep(2)

    raise RuntimeError("Employee-created notification was not observed")


def main():
    require(
        os.environ.get("CI", "").lower() == "true",
        "This script is CI-only; it promotes a disposable test user to HR",
    )

    suffix = uuid.uuid4().hex[:12]
    hr_username = f"smokehr{suffix}"
    employee_username = f"smokeemp{suffix}"
    hr_password = secrets.token_urlsafe(24)
    employee_password = secrets.token_urlsafe(24)
    email = f"smoke-{suffix}@example.invalid"
    department_name = f"Smoke-{suffix}"

    for base in (AUTH, EMPLOYEE, NOTIFICATION):
        health = api(base, "/actuator/health")
        require(health.get("status") == "UP", "An application is not healthy")
    print("PASS: application health")

    for username, password in (
        (hr_username, hr_password),
        (employee_username, employee_password),
    ):
        api(
            AUTH,
            "/api/v1/auth/register",
            method="POST",
            body={"username": username, "password": password},
        )

    role = mysql_query(
        "UPDATE users SET role='HR' "
        f"WHERE username='{hr_username}'; "
        "SELECT role FROM users "
        f"WHERE username='{hr_username}';"
    )
    require(role.splitlines()[-1:] == ["HR"], "HR test-user setup failed")

    hr_token = login(hr_username, hr_password)
    employee_token = login(employee_username, employee_password)
    print("PASS: registration and login")

    api(EMPLOYEE, "/api/v1/departments", expected=401)
    api(
        EMPLOYEE,
        "/api/v1/departments",
        method="POST",
        body={"name": department_name},
        token=employee_token,
        expected=403,
    )
    print("PASS: authentication and role restrictions")

    department = api(
        EMPLOYEE,
        "/api/v1/departments",
        method="POST",
        body={"name": department_name},
        token=hr_token,
        expected=201,
    )
    department_id = department["id"]
    require(department["name"] == department_name, "Department mismatch")

    employee_body = {
        "firstName": "Smoke",
        "lastName": "Employee",
        "email": email,
        "authUsername": employee_username,
        "designation": "Smoke Engineer",
        "dateOfJoining": "2026-01-01",
        "departmentId": department_id,
    }
    created = api(
        EMPLOYEE,
        "/api/v1/employees",
        method="POST",
        body=employee_body,
        token=hr_token,
        expected=201,
    )
    employee_id = created["id"]

    fetched = api(
        EMPLOYEE,
        f"/api/v1/employees/{employee_id}",
        token=hr_token,
    )
    require(fetched["email"] == email, "Employee read mismatch")

    mine = api(
        EMPLOYEE,
        "/api/v1/employees/me",
        token=employee_token,
    )
    require(mine["id"] == employee_id, "Self-service link mismatch")

    query = urllib.parse.urlencode(
        {"criteria": "designation", "value": "Smoke Engineer"}
    )
    matches = api(
        EMPLOYEE,
        f"/api/v1/employees/search?{query}",
        token=hr_token,
    )
    require(
        any(item["id"] == employee_id for item in matches),
        "Employee search did not find the created record",
    )

    updated_body = dict(employee_body)
    updated_body["designation"] = "Smoke Lead"
    updated = api(
        EMPLOYEE,
        f"/api/v1/employees/{employee_id}",
        method="PUT",
        body=updated_body,
        token=hr_token,
    )
    require(updated["designation"] == "Smoke Lead", "Employee update failed")
    print("PASS: department, employee, self-service, search, and update")

    wait_for_notification(email)
    print("PASS: log-only employee-created notification")

    api(
        EMPLOYEE,
        f"/api/v1/employees/{employee_id}",
        method="DELETE",
        token=hr_token,
        expected=204,
    )
    api(
        EMPLOYEE,
        f"/api/v1/departments/{department_id}",
        method="DELETE",
        token=hr_token,
        expected=204,
    )
    print("PASS: delete operations")
    print("PASS: Compose smoke check")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        # Do not print HTTP bodies, JWTs, passwords, or database credentials.
        print(f"FAIL: {error}", file=sys.stderr)
        sys.exit(1)
