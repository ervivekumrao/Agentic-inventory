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

import com.smartinventory.inventory.dto.RuntimeConfigDto;
import com.smartinventory.inventory.service.RuntimeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/config")
@Tag(name = "Runtime Configuration", description = "Zero-Downtime Dynamic Administrative Configuration Management")
public class RuntimeConfigController {

    private static final Logger log = LoggerFactory.getLogger(RuntimeConfigController.class);

    private final RuntimeConfigService runtimeConfigService;

    public RuntimeConfigController(RuntimeConfigService runtimeConfigService) {
        this.runtimeConfigService = runtimeConfigService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get current runtime configuration", description = "Retrieves active system configuration parameters without restarting containers.")
    public ResponseEntity<RuntimeConfigDto> getConfiguration() {
        log.debug("GET /api/admin/config requested");
        return ResponseEntity.ok(runtimeConfigService.getConfig());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update runtime configuration dynamically", description = "Applies zero-downtime configuration updates across microservice components.")
    public ResponseEntity<RuntimeConfigDto> updateConfiguration(@Valid @RequestBody RuntimeConfigDto newConfig) {
        log.info("POST /api/admin/config requested with new maxContextLimit: {}", newConfig.getMaxContextLimit());
        RuntimeConfigDto updated = runtimeConfigService.updateConfig(newConfig);
        return ResponseEntity.ok(updated);
    }
}
