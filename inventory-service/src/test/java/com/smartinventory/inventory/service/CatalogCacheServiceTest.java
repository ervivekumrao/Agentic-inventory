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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CatalogCacheServiceTest {

    private CatalogCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CatalogCacheService();
    }

    @Test
    void testCacheUpdateAndOptimisticRead() {
        Map<String, Object> p1 = new HashMap<>();
        p1.put("sku", "LAPTOP-PRO-15");
        p1.put("name", "Pro Laptop");
        p1.put("price", 1299.99);

        cacheService.updateCache(List.of(p1));

        assertTrue(cacheService.isCacheValid());
        assertEquals(1, cacheService.getCachedListOptimistic().size());

        Map<String, Object> cachedProd = cacheService.getCatalogProductBySku("LAPTOP-PRO-15");
        assertNotNull(cachedProd);
        assertEquals("Pro Laptop", cachedProd.get("name"));
        assertEquals(1299.99, cachedProd.get("price"));
    }

    @Test
    void testClearCache() {
        Map<String, Object> p1 = Map.of("sku", "IPHONE-15", "price", 999.0);
        cacheService.updateCache(List.of(p1));
        assertTrue(cacheService.isCacheValid());

        cacheService.clearCache();
        assertFalse(cacheService.isCacheValid());
        assertTrue(cacheService.getCachedListOptimistic().isEmpty());
        assertNull(cacheService.getCatalogProductBySku("IPHONE-15"));
    }

    @Test
    void testMultiThreadedConcurrentStampedLockReads() throws InterruptedException {
        Map<String, Object> p1 = Map.of("sku", "TEST-SKU", "price", 100.0);
        cacheService.updateCache(List.of(p1));

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Map<String, Object> item = cacheService.getCatalogProductBySku("TEST-SKU");
                    if (item != null && Double.valueOf(100.0).equals(item.get("price"))) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount, successCount.get());
    }
}
