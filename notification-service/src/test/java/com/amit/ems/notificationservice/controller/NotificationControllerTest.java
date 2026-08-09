package com.amit.ems.notificationservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notifyEmployeeCreated_shouldReturnOkForValidEvent()
            throws Exception {

        mockMvc.perform(post(
                        "/api/v1/notifications/employee-created"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeEmail": "amit@example.com",
                                  "employeeName": "Amit Vishwa"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void notifyEmployeeCreated_shouldRejectMalformedJson()
            throws Exception {

        mockMvc.perform(post(
                        "/api/v1/notifications/employee-created"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeEmail": "amit@example.com",
                                  "employeeName":
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notifyEmployeeCreated_shouldRejectUnsupportedHttpMethod()
            throws Exception {

        mockMvc.perform(get(
                        "/api/v1/notifications/employee-created"
                ))
                .andExpect(status().isMethodNotAllowed());
    }
}