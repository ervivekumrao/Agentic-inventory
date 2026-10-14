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

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class AiQueryRequest {

    @NotBlank(message = "Query string cannot be blank")
    private String query;

    private List<Map<String, Object>> inventoryContext;
    private List<Map<String, Object>> catalogContext;
    private String systemInstruction;

    public AiQueryRequest() {
    }

    public AiQueryRequest(String query) {
        this.query = query;
    }

    public AiQueryRequest(String query, List<Map<String, Object>> inventoryContext) {
        this.query = query;
        this.inventoryContext = inventoryContext;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<Map<String, Object>> getInventoryContext() {
        return inventoryContext;
    }

    public void setInventoryContext(List<Map<String, Object>> inventoryContext) {
        this.inventoryContext = inventoryContext;
    }

    public List<Map<String, Object>> getCatalogContext() {
        return catalogContext;
    }

    public void setCatalogContext(List<Map<String, Object>> catalogContext) {
        this.catalogContext = catalogContext;
    }

    public String getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(String systemInstruction) {
        this.systemInstruction = systemInstruction;
    }
}
