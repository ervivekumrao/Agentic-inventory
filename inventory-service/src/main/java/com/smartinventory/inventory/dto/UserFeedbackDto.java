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
import jakarta.validation.constraints.Size;

public class UserFeedbackDto {

    @NotBlank
    @Size(max = 1000)
    private String originalQuery;

    @Size(max = 1000)
    private String userCorrection;

    @NotBlank
    @Size(max = 100)
    private String correctedIntent = "CUSTOM_SORT_FILTER";

    @Size(max = 50)
    private String sortField;

    @Size(max = 10)
    private String sortOrder;

    @Size(max = 20)
    private String rating = "THUMBS_DOWN";

    public UserFeedbackDto() {}

    public UserFeedbackDto(String originalQuery, String userCorrection, String correctedIntent, String sortField, String sortOrder, String rating) {
        this.originalQuery = originalQuery;
        this.userCorrection = userCorrection;
        this.correctedIntent = correctedIntent != null ? correctedIntent : "CUSTOM_SORT_FILTER";
        this.sortField = sortField;
        this.sortOrder = sortOrder;
        this.rating = rating != null ? rating : "THUMBS_DOWN";
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }

    public String getUserCorrection() {
        return userCorrection;
    }

    public void setUserCorrection(String userCorrection) {
        this.userCorrection = userCorrection;
    }

    public String getCorrectedIntent() {
        return correctedIntent;
    }

    public void setCorrectedIntent(String correctedIntent) {
        this.correctedIntent = correctedIntent;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }
}
