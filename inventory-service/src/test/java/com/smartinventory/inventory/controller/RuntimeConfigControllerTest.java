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

package com.smartinventory.inventory.controller;

import com.smartinventory.inventory.dto.RuntimeConfigDto;
import com.smartinventory.inventory.service.RuntimeConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeConfigControllerTest {

    @Test
    void testGetAndUpdateConfiguration() {
        RuntimeConfigService service = new RuntimeConfigService();
        RuntimeConfigController controller = new RuntimeConfigController(service);

        RuntimeConfigDto initial = controller.getConfiguration().getBody();
        assertNotNull(initial);
        assertEquals(250, initial.getMaxContextLimit());
        assertEquals(1.65, initial.getSafetyStockZScore());

        RuntimeConfigDto updatedInput = new RuntimeConfigDto(500, 2.0, 3.0, 6000, 20000);
        RuntimeConfigDto result = controller.updateConfiguration(updatedInput).getBody();

        assertNotNull(result);
        assertEquals(500, result.getMaxContextLimit());
        assertEquals(2.0, result.getSafetyStockZScore());
        assertEquals(3.0, result.getDefaultReorderMultiplier());

        RuntimeConfigDto verified = controller.getConfiguration().getBody();
        assertNotNull(verified);
        assertEquals(500, verified.getMaxContextLimit());
    }
}
