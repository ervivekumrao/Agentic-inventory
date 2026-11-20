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

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the outcome of a bulk database import operation powered by high-performance JDBC batching.
 */
public class BulkImportResult {

    private boolean success;
    private int totalRecords;
    private int importedRecords;
    private int failedRecords;
    private long durationMs;
    private String message;
    private List<String> errors = new ArrayList<>();

    public BulkImportResult() {}

    public BulkImportResult(boolean success, int totalRecords, int importedRecords, int failedRecords, long durationMs, String message) {
        this.success = success;
        this.totalRecords = totalRecords;
        this.importedRecords = importedRecords;
        this.failedRecords = failedRecords;
        this.durationMs = durationMs;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getImportedRecords() {
        return importedRecords;
    }

    public void setImportedRecords(int importedRecords) {
        this.importedRecords = importedRecords;
    }

    public int getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(int failedRecords) {
        this.failedRecords = failedRecords;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }
}
