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

package com.smartinventory.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventory.model.OutboxEvent;
import com.smartinventory.inventory.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "inventory-stock-events";
    private final KafkaTemplate<String, StockUpdateEvent> kafkaTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaProducerService(
            KafkaTemplate<String, StockUpdateEvent> kafkaTemplate,
            @Autowired(required = false) OutboxEventRepository outboxEventRepository,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper != null ? objectMapper.copy().findAndRegisterModules() : new ObjectMapper().findAndRegisterModules();
    }

    public KafkaProducerService(KafkaTemplate<String, StockUpdateEvent> kafkaTemplate) {
        this(kafkaTemplate, null, null);
    }

    public void sendStockUpdateEvent(StockUpdateEvent event) {
        sendStockUpdateEvent(event, null);
    }

    public void sendStockUpdateEvent(StockUpdateEvent event, OutboxEvent existingOutbox) {
        log.debug("Persisting Transactional Outbox event for SKU={}, Quantity={}, EventType={}",
                event.getSku(), event.getQuantity(), event.getEventType());

        if (existingOutbox == null && outboxEventRepository != null) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                outboxEventRepository.save(new OutboxEvent("INVENTORY", event.getSku(), event.getEventType(), payload));
                log.debug("OutboxEvent successfully persisted for SKU: {}", event.getSku());
            } catch (Exception e) {
                log.warn("Failed to persist Transactional Outbox record for SKU {}: {}", event.getSku(), e.getMessage());
            }
        }
    }
}
