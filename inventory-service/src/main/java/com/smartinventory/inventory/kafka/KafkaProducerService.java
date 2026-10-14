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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "inventory-stock-events";
    private final KafkaTemplate<String, StockUpdateEvent> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, StockUpdateEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendStockUpdateEvent(StockUpdateEvent event) {
        log.debug("Publishing StockUpdateEvent to Kafka topic '{}': SKU={}, Quantity={}, EventType={}",
                TOPIC, event.getSku(), event.getQuantity(), event.getEventType());
        try {
            kafkaTemplate.send(TOPIC, event.getSku(), event).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("StockUpdateEvent successfully published for SKU: {}", event.getSku());
                } else {
                    log.warn("Failed to publish StockUpdateEvent for SKU {}: {}", event.getSku(), ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Exception during Kafka event publishing for SKU {}: {}", event.getSku(), e.getMessage());
        }
    }
}
