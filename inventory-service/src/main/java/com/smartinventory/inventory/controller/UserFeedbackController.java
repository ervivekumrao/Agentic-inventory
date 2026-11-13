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

import com.smartinventory.inventory.dto.UserFeedbackDto;
import com.smartinventory.inventory.model.UserFeedbackEntity;
import com.smartinventory.inventory.service.UserFeedbackService;
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

import java.util.List;

@RestController
@RequestMapping("/api/ai/feedback")
@Tag(name = "AI Feedback & Active Learning", description = "Endpoints for submitting AI interaction corrections and retrieving persistent feedback logs")
public class UserFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(UserFeedbackController.class);

    private final UserFeedbackService userFeedbackService;

    public UserFeedbackController(UserFeedbackService userFeedbackService) {
        this.userFeedbackService = userFeedbackService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR', 'USER')")
    @Operation(summary = "Submit AI Query Feedback / Correction", description = "Persists user interaction feedback and triggers active learning vector index updates.")
    public ResponseEntity<UserFeedbackEntity> submitFeedback(@Valid @RequestBody UserFeedbackDto dto) {
        log.info("Feedback submission received for query: '{}'", dto.getOriginalQuery());
        UserFeedbackEntity saved = userFeedbackService.saveFeedback(dto);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Retrieve Recent AI Interaction Feedback Logs", description = "Retrieves recent feedback logs for audit and model evaluation.")
    public ResponseEntity<List<UserFeedbackEntity>> getRecentFeedback() {
        log.debug("GET /api/ai/feedback requested");
        return ResponseEntity.ok(userFeedbackService.getRecentFeedback());
    }
}
