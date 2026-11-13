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

package com.smartinventory.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_feedback_logs")
public class UserFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_query", nullable = false, length = 1000)
    private String originalQuery;

    @Column(name = "user_correction", length = 1000)
    private String userCorrection;

    @Column(name = "corrected_intent", nullable = false, length = 100)
    private String correctedIntent;

    @Column(name = "sort_field", length = 50)
    private String sortField;

    @Column(name = "sort_order", length = 10)
    private String sortOrder;

    @Column(name = "rating", length = 20)
    private String rating;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserFeedbackEntity() {}

    public UserFeedbackEntity(String originalQuery, String userCorrection, String correctedIntent, String sortField, String sortOrder, String rating) {
        this.originalQuery = originalQuery;
        this.userCorrection = userCorrection;
        this.correctedIntent = correctedIntent;
        this.sortField = sortField;
        this.sortOrder = sortOrder;
        this.rating = rating;
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
