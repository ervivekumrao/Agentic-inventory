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

package com.smartinventory.catalog;

import com.smartinventory.catalog.repository.ProductRepository;
import com.smartinventory.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class CatalogActuatorLoggerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CatalogService catalogService;

    @Test
    void getLoggers_shouldReturnLoggersList() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggers").exists())
                .andExpect(jsonPath("$.loggers['com.smartinventory.catalog']").exists());
    }

    @Test
    void updateLoggerLevel_shouldModifyLogLevelAtRuntime() throws Exception {
        String loggerName = "com.smartinventory.catalog.controller.CatalogController";

        mockMvc.perform(post("/actuator/loggers/" + loggerName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configuredLevel\":\"DEBUG\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/actuator/loggers/" + loggerName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredLevel").value("DEBUG"));
    }
}
