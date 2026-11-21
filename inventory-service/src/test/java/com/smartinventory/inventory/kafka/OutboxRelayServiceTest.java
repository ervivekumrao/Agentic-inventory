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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, StockUpdateEvent> kafkaTemplate;

    private ObjectMapper objectMapper;
    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxRelayService = new OutboxRelayService(outboxEventRepository, kafkaTemplate, objectMapper);
    }

    @Test
    void relayPendingEvents_shouldSendToKafkaAndMarkSent() {
        String payload = "{\"sku\":\"SKU-TEST\",\"quantity\":10,\"eventType\":\"CREATED\",\"timestamp\":\"2026-11-19T10:00:00Z\"}";
        OutboxEvent event = new OutboxEvent("INVENTORY", "SKU-TEST", "CREATED", payload);
        event.setId(1L);

        when(outboxEventRepository.fetchPendingEventsForUpdateSkipLocked(eq(OutboxEvent.STATUS_PENDING), eq(50)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("inventory-stock-events"), eq("SKU-TEST"), any(StockUpdateEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxRelayService.relayPendingEvents();

        verify(kafkaTemplate, times(1)).send(eq("inventory-stock-events"), eq("SKU-TEST"), any(StockUpdateEvent.class));
        verify(outboxEventRepository, atLeastOnce()).save(event);
    }

    @Test
    void relayPendingEvents_whenEmpty_shouldDoNothing() {
        when(outboxEventRepository.fetchPendingEventsForUpdateSkipLocked(eq(OutboxEvent.STATUS_PENDING), eq(50)))
                .thenReturn(Collections.emptyList());
        when(outboxEventRepository.fetchPendingEventsForUpdateSkipLocked(eq(OutboxEvent.STATUS_FAILED), eq(50)))
                .thenReturn(Collections.emptyList());

        outboxRelayService.relayPendingEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
