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

import com.smartinventory.inventory.dto.RuntimeConfigDto;
import com.smartinventory.inventory.security.hash.HashAlgorithm;
import com.smartinventory.inventory.security.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class RuntimeConfigService {

    private static final Logger log = LoggerFactory.getLogger(RuntimeConfigService.class);

    private final AtomicReference<RuntimeConfigDto> currentConfig = new AtomicReference<>(new RuntimeConfigDto());

    public RuntimeConfigDto getConfig() {
        return currentConfig.get();
    }

    public RuntimeConfigDto updateConfig(RuntimeConfigDto newConfig) {
        if (newConfig == null) return currentConfig.get();
        currentConfig.set(newConfig);
        if (newConfig.getDefaultHashAlgorithm() != null) {
            Hashing.setDefaultAlgorithm(HashAlgorithm.fromString(newConfig.getDefaultHashAlgorithm()));
        }
        log.info("Runtime configuration updated dynamically: maxContextLimit={}, safetyStockZScore={}, defaultReorderMultiplier={}, connectTimeoutMs={}, readTimeoutMs={}, defaultHashAlgorithm={}",
                newConfig.getMaxContextLimit(), newConfig.getSafetyStockZScore(), newConfig.getDefaultReorderMultiplier(), newConfig.getConnectTimeoutMs(), newConfig.getReadTimeoutMs(), newConfig.getDefaultHashAlgorithm());
        return newConfig;
    }
}
