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

package com.smartinventory.inventory.service;

import com.smartinventory.inventory.dto.AiQueryRequest;
import com.smartinventory.inventory.dto.AiQueryResponse;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.security.pii.PiiLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private final InventoryRepository inventoryRepository;
    private final RestClient restClient;

    @Value("${forecasting.service.url:http://localhost:8083}")
    private String forecastingServiceUrl;

    @Value("${catalog.service.url:http://localhost:8082}")
    private String catalogServiceUrl;

    @Autowired
    public AiAgentService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public AiAgentService(InventoryRepository inventoryRepository, RestClient restClient, String forecastingServiceUrl, String catalogServiceUrl) {
        this.inventoryRepository = inventoryRepository;
        this.restClient = restClient;
        this.forecastingServiceUrl = forecastingServiceUrl;
        this.catalogServiceUrl = catalogServiceUrl;
    }

    public AiQueryResponse processQuery(AiQueryRequest request) {
        log.debug("Processing AI query: '{}'", PiiLogSanitizer.mask(request.getQuery()));

        // 1. Hydrate inventory context if missing
        if (request.getInventoryContext() == null || request.getInventoryContext().isEmpty()) {
            log.trace("Inventory context missing in request, querying database repository");
            List<InventoryItem> items = inventoryRepository.findAll();
            List<Map<String, Object>> contextList = new ArrayList<>();
            for (InventoryItem item : items) {
                Map<String, Object> map = new HashMap<>();
                map.put("sku", item.getSku());
                map.put("quantity", item.getQuantity());
                map.put("reorderThreshold", item.getReorderThreshold());
                map.put("warehouseLocation", item.getWarehouseLocation());
                contextList.add(map);
            }
            request.setInventoryContext(contextList);
            log.debug("Hydrated inventory context with {} items", contextList.size());
        }

        // 2. Hydrate catalog context from catalog-service if missing
        if (request.getCatalogContext() == null || request.getCatalogContext().isEmpty()) {
            log.trace("Catalog context missing, calling catalog-service at {}", catalogServiceUrl);
            try {
                List<Map<String, Object>> catalogList = restClient.get()
                        .uri(catalogServiceUrl + "/api/catalog")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                if (catalogList != null) {
                    request.setCatalogContext(catalogList);
                    log.debug("Hydrated catalog context with {} products from catalog-service", catalogList.size());
                }
            } catch (Exception e) {
                log.warn("Unable to fetch catalog context from catalog-service at {}: {}", catalogServiceUrl, e.getMessage());
            }
        }

        // 3. Delegate to Forecasting AI Agent service
        try {
            log.debug("Delegating query to forecasting AI service at {}", forecastingServiceUrl);
            AiQueryResponse response = restClient.post()
                    .uri(forecastingServiceUrl + "/api/ai/agent/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiQueryResponse.class);
            log.debug("Successfully received response from forecasting service with risk level: {}", response != null ? response.getRiskLevel() : "N/A");
            return response;
        } catch (Exception e) {
            log.warn("Call to forecasting service failed at {}: {}. Generating local fallback response.", forecastingServiceUrl, e.getMessage());
            return generateFallbackResponse(request);
        }
    }

    private AiQueryResponse generateFallbackResponse(AiQueryRequest request) {
        log.info("Generating local fallback response for query: '{}'", PiiLogSanitizer.mask(request.getQuery()));
        List<Map<String, Object>> invContext = request.getInventoryContext() != null ? request.getInventoryContext() : List.of();
        List<Map<String, Object>> catContext = request.getCatalogContext() != null ? request.getCatalogContext() : List.of();

        Map<String, Map<String, Object>> catalogBySku = new HashMap<>();
        for (Map<String, Object> prod : catContext) {
            if (prod.get("sku") != null) {
                catalogBySku.put(String.valueOf(prod.get("sku")), prod);
            }
        }

        int totalSkus = invContext.size();
        int totalQuantity = 0;
        double totalValuation = 0.0;

        List<String> lowStockSkus = new ArrayList<>();
        List<String> outOfStockSkus = new ArrayList<>();
        List<String> overstockedSkus = new ArrayList<>();

        Map<String, Integer> warehouseQty = new HashMap<>();
        Map<String, Integer> categoryQty = new HashMap<>();

        for (Map<String, Object> item : invContext) {
            String sku = String.valueOf(item.get("sku"));
            int qty = item.get("quantity") instanceof Number ? ((Number) item.get("quantity")).intValue() : 0;
            int threshold = item.get("reorderThreshold") instanceof Number ? ((Number) item.get("reorderThreshold")).intValue() : 10;
            String warehouse = item.get("warehouseLocation") != null ? String.valueOf(item.get("warehouseLocation")) : "Unassigned";

            Map<String, Object> prod = catalogBySku.get(sku);
            double price = 0.0;
            String category = "General";

            if (prod != null) {
                if (prod.get("price") instanceof Number) {
                    price = ((Number) prod.get("price")).doubleValue();
                }
                if (prod.get("category") != null) {
                    category = String.valueOf(prod.get("category"));
                }
            }

            totalQuantity += qty;
            totalValuation += (qty * price);

            warehouseQty.put(warehouse, warehouseQty.getOrDefault(warehouse, 0) + qty);
            categoryQty.put(category, categoryQty.getOrDefault(category, 0) + qty);

            if (qty == 0) {
                outOfStockSkus.add(sku);
            } else if (qty <= threshold) {
                lowStockSkus.add(sku);
            } else if (qty > 500) {
                overstockedSkus.add(sku);
            }
        }

        String lowerQuery = request.getQuery() != null ? request.getQuery().toLowerCase() : "";

        // Custom Top-N / Sorted / Superlative Fallback Handler
        if ((lowerQuery.contains("top") || lowerQuery.contains("sort") || lowerQuery.contains("lowest") ||
            lowerQuery.contains("highest") || lowerQuery.contains("order by") || lowerQuery.contains("most expensive") ||
            lowerQuery.contains("cheapest") || lowerQuery.contains("highest value") || lowerQuery.contains("lowest value")) &&
            !lowerQuery.contains("low in stock") && !lowerQuery.contains("low stock") && !lowerQuery.contains("reorder")) {

            int limit = 10;
            Matcher limitMatcher = Pattern.compile("(?:top|lowest|highest|first|limit)\\s+(\\d+)").matcher(lowerQuery);
            if (limitMatcher.find()) {
                try {
                    limit = Integer.parseInt(limitMatcher.group(1));
                } catch (NumberFormatException ignored) {}
            } else if (lowerQuery.contains("which item") || lowerQuery.contains("what item")) {
                limit = 5;
            }

            boolean ascending = lowerQuery.contains("lowest") || lowerQuery.contains("ascending") || lowerQuery.contains(" asc") || lowerQuery.contains("cheapest");
            List<Map<String, Object>> sortedList = new ArrayList<>(invContext);

            if (lowerQuery.contains("value") || lowerQuery.contains("valuation")) {
                sortedList.sort((a, b) -> {
                    String skuA = String.valueOf(a.get("sku"));
                    String skuB = String.valueOf(b.get("sku"));
                    int qA = a.get("quantity") instanceof Number ? ((Number) a.get("quantity")).intValue() : 0;
                    int qB = b.get("quantity") instanceof Number ? ((Number) b.get("quantity")).intValue() : 0;
                    double pA = catalogBySku.containsKey(skuA) && catalogBySku.get(skuA).get("price") instanceof Number
                            ? ((Number) catalogBySku.get(skuA).get("price")).doubleValue() : 0.0;
                    double pB = catalogBySku.containsKey(skuB) && catalogBySku.get(skuB).get("price") instanceof Number
                            ? ((Number) catalogBySku.get(skuB).get("price")).doubleValue() : 0.0;
                    double vA = qA * pA;
                    double vB = qB * pB;
                    return ascending ? Double.compare(vA, vB) : Double.compare(vB, vA);
                });
            } else if (lowerQuery.contains("price") || lowerQuery.contains("cost") || lowerQuery.contains("expensive") || lowerQuery.contains("cheapest")) {
                sortedList.sort((a, b) -> {
                    String skuA = String.valueOf(a.get("sku"));
                    String skuB = String.valueOf(b.get("sku"));
                    double pA = catalogBySku.containsKey(skuA) && catalogBySku.get(skuA).get("price") instanceof Number
                            ? ((Number) catalogBySku.get(skuA).get("price")).doubleValue() : 0.0;
                    double pB = catalogBySku.containsKey(skuB) && catalogBySku.get(skuB).get("price") instanceof Number
                            ? ((Number) catalogBySku.get(skuB).get("price")).doubleValue() : 0.0;
                    return ascending ? Double.compare(pA, pB) : Double.compare(pB, pA);
                });
            } else {
                // Default sort by quantity
                sortedList.sort((a, b) -> {
                    int qA = a.get("quantity") instanceof Number ? ((Number) a.get("quantity")).intValue() : 0;
                    int qB = b.get("quantity") instanceof Number ? ((Number) b.get("quantity")).intValue() : 0;
                    return ascending ? Integer.compare(qA, qB) : Integer.compare(qB, qA);
                });
            }

            if (sortedList.size() > limit) {
                sortedList = sortedList.subList(0, limit);
            }

            StringBuilder sb = new StringBuilder();
            String metricName = (lowerQuery.contains("value") || lowerQuery.contains("valuation")) ? "Valuation" :
                    ((lowerQuery.contains("price") || lowerQuery.contains("expensive") || lowerQuery.contains("cheapest")) ? "Price" : "Quantity");
            sb.append(String.format("### Top %d Inventory Items (Sorted by %s)%n%n",
                    sortedList.size(),
                    metricName + (ascending ? " Ascending" : " Descending")));
            sb.append("| SKU | Category | Warehouse | Quantity | Unit Price | Total Valuation |\n");
            sb.append("|---|---|---|---|---|---|\n");
            for (Map<String, Object> item : sortedList) {
                String sku = String.valueOf(item.get("sku"));
                int qty = item.get("quantity") instanceof Number ? ((Number) item.get("quantity")).intValue() : 0;
                String warehouse = item.get("warehouseLocation") != null ? String.valueOf(item.get("warehouseLocation")) : "Unassigned";
                Map<String, Object> prod = catalogBySku.get(sku);
                String category = prod != null && prod.get("category") != null ? String.valueOf(prod.get("category")) : "General";
                double price = prod != null && prod.get("price") instanceof Number ? ((Number) prod.get("price")).doubleValue() : 0.0;
                double val = qty * price;
                sb.append(String.format("| %s | %s | %s | %d | $%.2f | $%.2f |%n", sku, category, warehouse, qty, price, val));
            }
            return new AiQueryResponse(sb.toString(), List.of("Sorted inventory dataset generated."), "LOW");
        }

        if (lowerQuery.contains("tabular") || lowerQuery.contains("table") || lowerQuery.contains("detail")) {
            StringBuilder sb = new StringBuilder();
            sb.append("### Detailed Inventory Table\n\n");
            sb.append("| SKU | Category | Warehouse | Quantity | Reorder Threshold |\n");
            sb.append("|---|---|---|---|---|\n");
            for (Map<String, Object> item : invContext) {
                String sku = String.valueOf(item.get("sku"));
                int qty = item.get("quantity") instanceof Number ? ((Number) item.get("quantity")).intValue() : 0;
                int threshold = item.get("reorderThreshold") instanceof Number ? ((Number) item.get("reorderThreshold")).intValue() : 10;
                String warehouse = item.get("warehouseLocation") != null ? String.valueOf(item.get("warehouseLocation")) : "Unassigned";
                Map<String, Object> prod = catalogBySku.get(sku);
                String category = prod != null && prod.get("category") != null ? String.valueOf(prod.get("category")) : "General";
                sb.append(String.format("| %s | %s | %s | %d | %d |\n", sku, category, warehouse, qty, threshold));
            }
            return new AiQueryResponse(sb.toString(), List.of("Review detailed stock distribution across warehouses."), "LOW");
        }

        if (lowerQuery.contains("how many") || lowerQuery.contains("distinct") || lowerQuery.contains("skus")) {
            String answer = String.format("The inventory currently tracks **%d distinct SKUs** across all active warehouses.", totalSkus);
            return new AiQueryResponse(answer, List.of("Maintain optimal SKU catalog tracking."), "LOW");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Inventory Fallback Summary\n\n");
        sb.append(String.format("- **Total SKUs Tracked**: %d\n", totalSkus));
        sb.append(String.format("- **Total Stock Quantity**: %d units\n", totalQuantity));
        sb.append(String.format("- **Estimated Portfolio Valuation**: $%,.2f\n\n", totalValuation));

        if (!outOfStockSkus.isEmpty()) {
            sb.append(String.format("⚠️ **Out of Stock SKUs (%d)**: %s\n", outOfStockSkus.size(), String.join(", ", outOfStockSkus)));
        }
        if (!lowStockSkus.isEmpty()) {
            sb.append(String.format("🔔 **Low Stock SKUs (%d)**: %s\n", lowStockSkus.size(), String.join(", ", lowStockSkus)));
        }
        if (!overstockedSkus.isEmpty()) {
            sb.append(String.format("📦 **Overstocked SKUs (%d)**: %s\n", overstockedSkus.size(), String.join(", ", overstockedSkus)));
        }

        List<String> recommendations = new ArrayList<>();
        if (!outOfStockSkus.isEmpty()) {
            recommendations.add("Urgent: Restock out-of-stock items immediately to prevent lost sales.");
        }
        if (!lowStockSkus.isEmpty()) {
            recommendations.add("Warning: Place purchase orders for low-stock items approaching reorder thresholds.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Inventory levels are healthy across all tracked warehouses.");
        }

        String riskLevel = "LOW";
        if (!outOfStockSkus.isEmpty()) {
            riskLevel = "CRITICAL";
        } else if (!lowStockSkus.isEmpty()) {
            riskLevel = "HIGH";
        }

        return new AiQueryResponse(sb.toString(), recommendations, riskLevel);
    }
}
