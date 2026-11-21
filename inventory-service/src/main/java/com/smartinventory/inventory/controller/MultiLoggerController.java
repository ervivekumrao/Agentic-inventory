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

package com.smartinventory.inventory.controller;

import com.smartinventory.inventory.dto.LoggerUpdateRequest;
import com.smartinventory.inventory.dto.MultiServiceLoggerDto;
import com.smartinventory.inventory.dto.MultiServiceLoggerResponseDto;
import com.smartinventory.inventory.dto.ServiceStatusDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/multi-loggers")
@Tag(name = "Multi-Service Logger Governance", description = "Centralized dynamic log level governance across all microservices")
public class MultiLoggerController {

    private static final Logger log = LoggerFactory.getLogger(MultiLoggerController.class);

    private final String catalogServiceUrl;
    private final String forecastingServiceUrl;
    private final RestClient restClient;
    private final LoggingSystem loggingSystem;

    @org.springframework.beans.factory.annotation.Autowired
    public MultiLoggerController(
            @Value("${catalog.service.url:http://localhost:8082}") String catalogServiceUrl,
            @Value("${forecasting.service.url:http://localhost:8083}") String forecastingServiceUrl) {
        this.catalogServiceUrl = catalogServiceUrl;
        this.forecastingServiceUrl = forecastingServiceUrl;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.loggingSystem = LoggingSystem.get(MultiLoggerController.class.getClassLoader());
    }

    public MultiLoggerController(String catalogServiceUrl, String forecastingServiceUrl, RestClient restClient, LoggingSystem loggingSystem) {
        this.catalogServiceUrl = catalogServiceUrl;
        this.forecastingServiceUrl = forecastingServiceUrl;
        this.restClient = restClient;
        this.loggingSystem = loggingSystem != null ? loggingSystem : LoggingSystem.get(MultiLoggerController.class.getClassLoader());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get loggers across all running microservices", description = "Aggregates loggers from inventory-service, catalog-service, and forecasting-service.")
    public ResponseEntity<MultiServiceLoggerResponseDto> getAllLoggers() {
        List<ServiceStatusDto> services = new ArrayList<>();
        List<MultiServiceLoggerDto> loggersList = new ArrayList<>();

        // 1. Local Inventory Service
        try {
            services.add(new ServiceStatusDto("inventory-service", "Inventory Service", "UP", "local:8081"));
            for (LoggerConfiguration config : loggingSystem.getLoggerConfigurations()) {
                loggersList.add(new MultiServiceLoggerDto(
                        "inventory-service",
                        "Inventory Service",
                        config.getName(),
                        config.getEffectiveLevel() != null ? config.getEffectiveLevel().name() : "INFO",
                        config.getConfiguredLevel() != null ? config.getConfiguredLevel().name() : null
                ));
            }
        } catch (Exception e) {
            log.error("Error fetching inventory service loggers: {}", e.getMessage());
            services.add(new ServiceStatusDto("inventory-service", "Inventory Service", "DOWN", "local:8081"));
        }

        // 2. Catalog Service
        fetchCatalogLoggers(services, loggersList);

        // 3. Forecasting Service
        fetchForecastingLoggers(services, loggersList);

        return ResponseEntity.ok(new MultiServiceLoggerResponseDto(services, loggersList));
    }

    private void fetchCatalogLoggers(List<ServiceStatusDto> services, List<MultiServiceLoggerDto> loggersList) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(catalogServiceUrl + "/actuator/loggers")
                    .retrieve()
                    .body(Map.class);

            services.add(new ServiceStatusDto("catalog-service", "Catalog Service", "UP", catalogServiceUrl));

            if (response != null && response.containsKey("loggers")) {
                Map<?, ?> loggers = (Map<?, ?>) response.get("loggers");
                for (Map.Entry<?, ?> entry : loggers.entrySet()) {
                    String name = entry.getKey().toString();
                    Map<?, ?> details = (Map<?, ?>) entry.getValue();
                    String effectiveLevel = details.get("effectiveLevel") != null ? details.get("effectiveLevel").toString() : "INFO";
                    String configuredLevel = details.get("configuredLevel") != null ? details.get("configuredLevel").toString() : null;

                    loggersList.add(new MultiServiceLoggerDto(
                            "catalog-service",
                            "Catalog Service",
                            name,
                            effectiveLevel,
                            configuredLevel
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Catalog service is offline or unreachable at {}: {}", catalogServiceUrl, e.getMessage());
            services.add(new ServiceStatusDto("catalog-service", "Catalog Service", "DOWN", catalogServiceUrl));
        }
    }

    private void fetchForecastingLoggers(List<ServiceStatusDto> services, List<MultiServiceLoggerDto> loggersList) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(forecastingServiceUrl + "/api/admin/loggers")
                    .retrieve()
                    .body(Map.class);

            services.add(new ServiceStatusDto("forecasting-service", "Forecasting Service", "UP", forecastingServiceUrl));

            if (response != null && response.containsKey("loggers")) {
                Map<?, ?> loggers = (Map<?, ?>) response.get("loggers");
                for (Map.Entry<?, ?> entry : loggers.entrySet()) {
                    String name = entry.getKey().toString();
                    Map<?, ?> details = (Map<?, ?>) entry.getValue();
                    String effectiveLevel = details.get("effectiveLevel") != null ? details.get("effectiveLevel").toString() : "INFO";
                    String configuredLevel = details.get("configuredLevel") != null ? details.get("configuredLevel").toString() : null;
                    if ("NOTSET".equalsIgnoreCase(configuredLevel)) {
                        configuredLevel = null;
                    }

                    loggersList.add(new MultiServiceLoggerDto(
                            "forecasting-service",
                            "Forecasting Service",
                            name,
                            effectiveLevel,
                            configuredLevel
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Forecasting service is offline or unreachable at {}: {}", forecastingServiceUrl, e.getMessage());
            services.add(new ServiceStatusDto("forecasting-service", "Forecasting Service", "DOWN", forecastingServiceUrl));
        }
    }

    @PostMapping("/{serviceId}/{loggerName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update log level for a specific logger in a target microservice", description = "Updates logger level dynamically across inventory-service, catalog-service, or forecasting-service.")
    public ResponseEntity<?> updateLoggerLevel(
            @PathVariable String serviceId,
            @PathVariable String loggerName,
            @RequestBody LoggerUpdateRequest request) {

        String newLevel = request != null ? request.getConfiguredLevel() : null;
        log.info("Updating logger '{}' on service '{}' to level '{}'", loggerName, serviceId, newLevel);

        if ("inventory-service".equalsIgnoreCase(serviceId)) {
            LogLevel levelEnum = (newLevel != null && !newLevel.isBlank()) ? LogLevel.valueOf(newLevel.toUpperCase()) : null;
            loggingSystem.setLogLevel(loggerName, levelEnum);
            return ResponseEntity.ok(Map.of("message", "Updated inventory-service logger level", "name", loggerName, "configuredLevel", newLevel != null ? newLevel : "DEFAULT"));
        } else if ("catalog-service".equalsIgnoreCase(serviceId)) {
            try {
                restClient.post()
                        .uri(catalogServiceUrl + "/actuator/loggers/" + loggerName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("configuredLevel", newLevel != null ? newLevel : "null"))
                        .retrieve()
                        .toBodilessEntity();
                return ResponseEntity.ok(Map.of("message", "Updated catalog-service logger level", "name", loggerName, "configuredLevel", newLevel != null ? newLevel : "DEFAULT"));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update catalog-service logger: " + e.getMessage()));
            }
        } else if ("forecasting-service".equalsIgnoreCase(serviceId)) {
            try {
                restClient.post()
                        .uri(forecastingServiceUrl + "/api/admin/loggers/" + loggerName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("level", newLevel != null ? newLevel : "NOTSET"))
                        .retrieve()
                        .toBodilessEntity();
                return ResponseEntity.ok(Map.of("message", "Updated forecasting-service logger level", "name", loggerName, "configuredLevel", newLevel != null ? newLevel : "DEFAULT"));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update forecasting-service logger: " + e.getMessage()));
            }
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Unknown serviceId: " + serviceId));
    }
}
