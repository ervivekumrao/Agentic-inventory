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
