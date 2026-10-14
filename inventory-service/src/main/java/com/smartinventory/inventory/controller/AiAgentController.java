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

import com.smartinventory.inventory.dto.AiQueryRequest;
import com.smartinventory.inventory.dto.AiQueryResponse;
import com.smartinventory.inventory.service.AiAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Agent", description = "AI Agent Assistant for Natural Language Inventory Queries & Operations")
public class AiAgentController {

    private static final Logger log = LoggerFactory.getLogger(AiAgentController.class);

    private final AiAgentService aiAgentService;

    public AiAgentController(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @PostMapping("/agent/chat")
    @Operation(summary = "Chat with AI Inventory Agent", description = "Answers complex inventory questions, provides valuation, reorder recommendations, and human-like operational advice.")
    public ResponseEntity<AiQueryResponse> chatWithAgent(@Valid @RequestBody AiQueryRequest request) {
        log.info("AI Agent chat query received: '{}'", request.getQuery());
        AiQueryResponse response = aiAgentService.processQuery(request);
        log.info("AI Agent responded with risk level: {}", response.getRiskLevel());
        log.debug("AI Agent response answer snippet: {}", response.getAnswer() != null && response.getAnswer().length() > 80 ? response.getAnswer().substring(0, 80) + "..." : response.getAnswer());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory-assistant")
    @Operation(summary = "Legacy Inventory Assistant Endpoint", description = "Delegates to AI Agent for RAG inventory insights.")
    public ResponseEntity<AiQueryResponse> inventoryAssistant(@Valid @RequestBody AiQueryRequest request) {
        log.debug("Legacy /inventory-assistant endpoint called, delegating to chatWithAgent");
        return chatWithAgent(request);
    }
}
