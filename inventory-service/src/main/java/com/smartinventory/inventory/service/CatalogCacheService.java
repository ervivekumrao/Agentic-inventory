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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

@Service
public class CatalogCacheService {

    private static final Logger log = LoggerFactory.getLogger(CatalogCacheService.class);
    private static final long DEFAULT_TTL_MS = 300_000L; // 5 minutes cache TTL

    private final StampedLock lock = new StampedLock();
    private Map<String, Map<String, Object>> cacheBySku = new HashMap<>();
    private List<Map<String, Object>> cacheList = new ArrayList<>();
    private long lastFetchTimeMs = 0L;

    public List<Map<String, Object>> getCatalogList(RestClient restClient, String catalogServiceUrl) {
        if (isCacheValid()) {
            return getCachedListOptimistic();
        }

        // Fetch fresh data from catalog-service
        try {
            log.debug("Catalog cache expired or empty. Querying catalog-service at {}", catalogServiceUrl);
            List<Map<String, Object>> freshList = restClient.get()
                    .uri(catalogServiceUrl + "/api/catalog")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (freshList != null && !freshList.isEmpty()) {
                updateCache(freshList);
                log.debug("Successfully updated catalog cache with {} products", freshList.size());
                return freshList;
            }
        } catch (Exception e) {
            log.warn("Catalog service fetch failed at {}: {}. Serving cached catalog metadata.", catalogServiceUrl, e.getMessage());
        }

        return getCachedListOptimistic();
    }

    public boolean isCacheValid() {
        long stamp = lock.tryOptimisticRead();
        long lastTime = this.lastFetchTimeMs;
        boolean hasData = !this.cacheList.isEmpty();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                lastTime = this.lastFetchTimeMs;
                hasData = !this.cacheList.isEmpty();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return hasData && (System.currentTimeMillis() - lastTime < DEFAULT_TTL_MS);
    }

    public List<Map<String, Object>> getCachedListOptimistic() {
        long stamp = lock.tryOptimisticRead();
        List<Map<String, Object>> result = this.cacheList;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = this.cacheList;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result != null ? Collections.unmodifiableList(result) : List.of();
    }

    public Map<String, Object> getCatalogProductBySku(String sku) {
        if (sku == null) return null;
        long stamp = lock.tryOptimisticRead();
        Map<String, Object> prod = this.cacheBySku.get(sku.toUpperCase());
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                prod = this.cacheBySku.get(sku.toUpperCase());
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return prod;
    }

    public void updateCache(List<Map<String, Object>> freshList) {
        if (freshList == null) return;
        Map<String, Map<String, Object>> newMap = new HashMap<>();
        for (Map<String, Object> item : freshList) {
            Object skuObj = item.get("sku");
            if (skuObj != null) {
                newMap.put(String.valueOf(skuObj).toUpperCase(), item);
            }
        }

        long stamp = lock.writeLock();
        try {
            this.cacheList = new ArrayList<>(freshList);
            this.cacheBySku = newMap;
            this.lastFetchTimeMs = System.currentTimeMillis();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void clearCache() {
        long stamp = lock.writeLock();
        try {
            this.cacheList = new ArrayList<>();
            this.cacheBySku = new HashMap<>();
            this.lastFetchTimeMs = 0L;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
