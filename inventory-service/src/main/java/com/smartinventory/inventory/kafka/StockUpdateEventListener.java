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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Event consumer listening to real-time stock change events from Apache Kafka,
 * closing the distributed event-driven messaging loop.
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.listener.auto-startup", havingValue = "true", matchIfMissing = true)
public class StockUpdateEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockUpdateEventListener.class);
    private final AtomicLong consumedEventsCount = new AtomicLong(0);

    @KafkaListener(
            topics = "inventory-stock-events",
            groupId = "${spring.kafka.consumer.group-id:inventory-service-audit-group}"
    )
    public void handleStockUpdateEvent(StockUpdateEvent event) {
        if (event == null) {
            return;
        }
        consumedEventsCount.incrementAndGet();
        log.info("Kafka consumer received StockUpdateEvent [SKU: {}, Quantity: {}, EventType: {}]",
                event.getSku(), event.getQuantity(), event.getEventType());
    }

    public long getConsumedEventsCount() {
        return consumedEventsCount.get();
    }
}
