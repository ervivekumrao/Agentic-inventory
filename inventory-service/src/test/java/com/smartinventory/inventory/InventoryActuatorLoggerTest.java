/*
 * Copyright (c) 2026 Vivek Umrao.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
