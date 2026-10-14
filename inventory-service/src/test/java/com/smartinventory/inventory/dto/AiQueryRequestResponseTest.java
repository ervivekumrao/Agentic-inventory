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

package com.smartinventory.inventory.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiQueryRequestResponseTest {

    @Test
    void testAiQueryRequestGettersAndSetters() {
        AiQueryRequest request = new AiQueryRequest();
        request.setQuery("Test Query");
        List<Map<String, Object>> invCtx = List.of(Map.of("sku", "SKU-1"));
        List<Map<String, Object>> catCtx = List.of(Map.of("name", "Product 1"));
        request.setInventoryContext(invCtx);
        request.setCatalogContext(catCtx);
        request.setSystemInstruction("Instruction");

        assertEquals("Test Query", request.getQuery());
        assertEquals(invCtx, request.getInventoryContext());
        assertEquals(catCtx, request.getCatalogContext());
        assertEquals("Instruction", request.getSystemInstruction());
    }

    @Test
    void testAiQueryRequestConstructors() {
        AiQueryRequest req1 = new AiQueryRequest("Query 1");
        assertEquals("Query 1", req1.getQuery());

        List<Map<String, Object>> invCtx = List.of(Map.of("sku", "SKU-1"));
        AiQueryRequest req2 = new AiQueryRequest("Query 2", invCtx);
        assertEquals("Query 2", req2.getQuery());
        assertEquals(invCtx, req2.getInventoryContext());
    }

    @Test
    void testAiQueryResponseGettersAndSetters() {
        AiQueryResponse response = new AiQueryResponse();
        response.setAnswer("Answer text");
        List<String> recs = List.of("Rec 1");
        response.setRecommendations(recs);
        response.setRiskLevel("HIGH");

        assertEquals("Answer text", response.getAnswer());
        assertEquals(recs, response.getRecommendations());
        assertEquals("HIGH", response.getRiskLevel());
    }

    @Test
    void testAiQueryResponseConstructor() {
        List<String> recs = List.of("Rec 1");
        AiQueryResponse response = new AiQueryResponse("Answer", recs, "LOW");

        assertEquals("Answer", response.getAnswer());
        assertEquals(recs, response.getRecommendations());
        assertEquals("LOW", response.getRiskLevel());
    }
}
