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

import com.smartinventory.inventory.dto.UserFeedbackDto;
import com.smartinventory.inventory.model.UserFeedbackEntity;
import com.smartinventory.inventory.repository.UserFeedbackRepository;
import com.smartinventory.inventory.service.UserFeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFeedbackControllerTest {

    @Mock
    private UserFeedbackRepository userFeedbackRepository;

    private UserFeedbackService userFeedbackService;
    private UserFeedbackController controller;

    @BeforeEach
    void setUp() {
        userFeedbackService = new UserFeedbackService(userFeedbackRepository);
        controller = new UserFeedbackController(userFeedbackService);
    }

    @Test
    void testSubmitAndRetrieveFeedback() {
        UserFeedbackEntity mockSaved = new UserFeedbackEntity("lowes per value item", "show lowest unit price items", "CUSTOM_SORT_FILTER", "price", "asc", "THUMBS_DOWN");
        when(userFeedbackRepository.save(any(UserFeedbackEntity.class))).thenReturn(mockSaved);
        when(userFeedbackRepository.findTop100ByOrderByIdDesc()).thenReturn(List.of(mockSaved));

        UserFeedbackDto dto = new UserFeedbackDto("lowes per value item", "show lowest unit price items", "CUSTOM_SORT_FILTER", "price", "asc", "THUMBS_DOWN");
        ResponseEntity<UserFeedbackEntity> submitResponse = controller.submitFeedback(dto);

        assertNotNull(submitResponse.getBody());
        assertEquals("lowes per value item", submitResponse.getBody().getOriginalQuery());
        assertEquals("price", submitResponse.getBody().getSortField());
        assertEquals("asc", submitResponse.getBody().getSortOrder());

        ResponseEntity<List<UserFeedbackEntity>> listResponse = controller.getRecentFeedback();
        assertNotNull(listResponse.getBody());
        assertEquals(1, listResponse.getBody().size());
        assertEquals("lowes per value item", listResponse.getBody().get(0).getOriginalQuery());
    }
}
