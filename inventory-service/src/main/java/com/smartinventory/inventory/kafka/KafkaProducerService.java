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

    @Autowired
    public KafkaProducerService(
            KafkaTemplate<String, StockUpdateEvent> kafkaTemplate,
            @Autowired(required = false) OutboxEventRepository outboxEventRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxEventRepository = outboxEventRepository;
    }

    public KafkaProducerService(KafkaTemplate<String, StockUpdateEvent> kafkaTemplate) {
        this(kafkaTemplate, null);
    }

    public void sendStockUpdateEvent(StockUpdateEvent event) {
        sendStockUpdateEvent(event, null);
    }

    public void sendStockUpdateEvent(StockUpdateEvent event, OutboxEvent existingOutbox) {
        log.debug("Publishing StockUpdateEvent to Kafka topic '{}': SKU={}, Quantity={}, EventType={}",
                TOPIC, event.getSku(), event.getQuantity(), event.getEventType());

        OutboxEvent outbox = existingOutbox;
        if (outbox == null && outboxEventRepository != null) {
            try {
                String payload = String.format("{\"sku\":\"%s\",\"quantity\":%d,\"eventType\":\"%s\"}",
                        event.getSku(), event.getQuantity(), event.getEventType());
                outbox = outboxEventRepository.save(new OutboxEvent("INVENTORY", event.getSku(), event.getEventType(), payload));
            } catch (Exception e) {
                log.warn("Failed to persist Transactional Outbox record for SKU {}: {}", event.getSku(), e.getMessage());
            }
        }

        final OutboxEvent trackedOutbox = outbox;
        try {
            kafkaTemplate.send(TOPIC, event.getSku(), event).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("StockUpdateEvent successfully published for SKU: {}", event.getSku());
                    if (trackedOutbox != null && outboxEventRepository != null) {
                        try {
                            trackedOutbox.markSent();
                            outboxEventRepository.save(trackedOutbox);
                        } catch (Exception ignored) {}
                    }
                } else {
                    log.warn("Failed to publish StockUpdateEvent for SKU {}: {}", event.getSku(), ex.getMessage());
                    if (trackedOutbox != null && outboxEventRepository != null) {
                        try {
                            trackedOutbox.markFailed();
                            outboxEventRepository.save(trackedOutbox);
                        } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Exception during Kafka event publishing for SKU {}: {}", event.getSku(), e.getMessage());
            if (trackedOutbox != null && outboxEventRepository != null) {
                try {
                    trackedOutbox.markFailed();
                    outboxEventRepository.save(trackedOutbox);
                } catch (Exception ignored) {}
            }
        }
    }
}
