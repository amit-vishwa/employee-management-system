CREATE TABLE departments
(
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,

    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uk_departments_name UNIQUE (name)
);

CREATE TABLE employees
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    first_name       VARCHAR(255) NOT NULL,
    last_name        VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    auth_username    VARCHAR(255),
    designation      VARCHAR(255),
    date_of_joining  DATE,
    department_id    BIGINT,

    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT uk_employees_auth_username UNIQUE (auth_username),
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id)
        REFERENCES departments (id)
);

CREATE INDEX idx_employees_department_id
    ON employees (department_id);