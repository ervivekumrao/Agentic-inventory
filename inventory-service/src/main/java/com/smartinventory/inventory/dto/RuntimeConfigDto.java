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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class RuntimeConfigDto {

    @Min(10)
    @Max(10000)
    private int maxContextLimit = 250;

    @Min(0)
    @Max(5)
    private double safetyStockZScore = 1.65;

    @Min(1)
    @Max(10)
    private double defaultReorderMultiplier = 2.0;

    @Min(1000)
    @Max(60000)
    private int connectTimeoutMs = 5000;

    @Min(1000)
    @Max(120000)
    private int readTimeoutMs = 15000;

    private String defaultHashAlgorithm = "SHA-256";

    public RuntimeConfigDto() {}

    public RuntimeConfigDto(int maxContextLimit, double safetyStockZScore, double defaultReorderMultiplier, int connectTimeoutMs, int readTimeoutMs) {
        this.maxContextLimit = maxContextLimit;
        this.safetyStockZScore = safetyStockZScore;
        this.defaultReorderMultiplier = defaultReorderMultiplier;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.defaultHashAlgorithm = "SHA-256";
    }

    public RuntimeConfigDto(int maxContextLimit, double safetyStockZScore, double defaultReorderMultiplier, int connectTimeoutMs, int readTimeoutMs, String defaultHashAlgorithm) {
        this.maxContextLimit = maxContextLimit;
        this.safetyStockZScore = safetyStockZScore;
        this.defaultReorderMultiplier = defaultReorderMultiplier;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.defaultHashAlgorithm = defaultHashAlgorithm != null ? defaultHashAlgorithm : "SHA-256";
    }

    public int getMaxContextLimit() {
        return maxContextLimit;
    }

    public void setMaxContextLimit(int maxContextLimit) {
        this.maxContextLimit = maxContextLimit;
    }

    public double getSafetyStockZScore() {
        return safetyStockZScore;
    }

    public void setSafetyStockZScore(double safetyStockZScore) {
        this.safetyStockZScore = safetyStockZScore;
    }

    public double getDefaultReorderMultiplier() {
        return defaultReorderMultiplier;
    }

    public void setDefaultReorderMultiplier(double defaultReorderMultiplier) {
        this.defaultReorderMultiplier = defaultReorderMultiplier;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getDefaultHashAlgorithm() {
        return defaultHashAlgorithm;
    }

    public void setDefaultHashAlgorithm(String defaultHashAlgorithm) {
        this.defaultHashAlgorithm = defaultHashAlgorithm;
    }
}
