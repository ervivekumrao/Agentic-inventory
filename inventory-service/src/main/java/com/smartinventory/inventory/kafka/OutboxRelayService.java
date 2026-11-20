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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background Transactional Outbox Relay:
 * Periodically polls unpublished or failed OutboxEvent records from the database
 * and reliably publishes them to Apache Kafka with at-least-once delivery semantics.
 */
@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
    private static final String TOPIC = "inventory-stock-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, StockUpdateEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Autowired
    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, StockUpdateEvent> kafkaTemplate,
            ObjectMapper objectMapper,
            @Autowired(required = false) io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper != null ? objectMapper.copy() : new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.meterRegistry = meterRegistry;
    }

    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, StockUpdateEvent> kafkaTemplate,
            ObjectMapper objectMapper) {
        this(outboxEventRepository, kafkaTemplate, objectMapper, null);
    }

    /**
     * Polls PENDING or FAILED events in batches and dispatches them to Kafka.
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void relayPendingEvents() {
        if (outboxEventRepository == null) {
            return;
        }

        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.STATUS_PENDING);
        if (pending.isEmpty()) {
            pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.STATUS_FAILED);
        }

        if (pending.isEmpty()) {
            return;
        }

        log.debug("OutboxRelayService processing {} pending outbox events", pending.size());
        for (OutboxEvent event : pending) {
            try {
                StockUpdateEvent stockEvent = objectMapper.readValue(event.getPayload(), StockUpdateEvent.class);
                kafkaTemplate.send(TOPIC, event.getAggregateId(), stockEvent).whenComplete((result, ex) -> {
                    if (ex == null) {
                        event.markSent();
                        outboxEventRepository.save(event);
                        recordMetric("SENT");
                        log.debug("Outbox event {} successfully relayed to Kafka for SKU: {}", event.getId(), event.getAggregateId());
                    } else {
                        event.markFailed();
                        outboxEventRepository.save(event);
                        recordMetric("FAILED");
                        log.warn("Failed to relay outbox event {}: {}", event.getId(), ex.getMessage());
                    }
                });
            } catch (Exception e) {
                log.warn("Could not deserialize outbox event {}: {}", event.getId(), e.getMessage());
                event.markFailed();
                outboxEventRepository.save(event);
                recordMetric("FAILED");
            }
        }
    }

    private void recordMetric(String status) {
        if (meterRegistry != null) {
            try {
                meterRegistry.counter("inventory.outbox.events.processed", "status", status).increment();
            } catch (Exception ignored) {}
        }
    }
}
