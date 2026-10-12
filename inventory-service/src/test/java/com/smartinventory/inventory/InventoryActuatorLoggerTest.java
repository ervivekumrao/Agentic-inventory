package com.smartinventory.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryActuatorLoggerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getHealth_shouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void getLoggers_unauthenticated_shouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLoggers_asAdmin_shouldReturnLoggersList() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggers").exists())
                .andExpect(jsonPath("$.loggers['com.smartinventory.inventory']").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateLoggerLevel_shouldModifyLogLevelAtRuntime() throws Exception {
        String loggerName = "com.smartinventory.inventory.service.AiAgentService";

        mockMvc.perform(post("/actuator/loggers/" + loggerName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configuredLevel\":\"DEBUG\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/actuator/loggers/" + loggerName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredLevel").value("DEBUG"));
    }
}
