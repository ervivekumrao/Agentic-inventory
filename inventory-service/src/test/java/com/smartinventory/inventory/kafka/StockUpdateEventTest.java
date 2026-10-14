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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class StockUpdateEventTest {

    @Test
    void testStockUpdateEventGettersAndSetters() {
        StockUpdateEvent event = new StockUpdateEvent();
        event.setSku("SKU-100");
        event.setQuantity(50);
        event.setEventType("ADJUSTMENT");
        Instant now = Instant.now();
        event.setTimestamp(now);

        assertEquals("SKU-100", event.getSku());
        assertEquals(50, event.getQuantity());
        assertEquals("ADJUSTMENT", event.getEventType());
        assertEquals(now, event.getTimestamp());
    }

    @Test
    void testStockUpdateEventConstructor() {
        StockUpdateEvent event = new StockUpdateEvent("SKU-200", 150, "IN");
        assertEquals("SKU-200", event.getSku());
        assertEquals(150, event.getQuantity());
        assertEquals("IN", event.getEventType());
        assertNotNull(event.getTimestamp());
    }
}
