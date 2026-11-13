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

import com.smartinventory.inventory.dto.UserFeedbackDto;
import com.smartinventory.inventory.model.UserFeedbackEntity;
import com.smartinventory.inventory.repository.UserFeedbackRepository;
import com.smartinventory.inventory.security.pii.PiiLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class UserFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(UserFeedbackService.class);

    private final UserFeedbackRepository userFeedbackRepository;
    private final RestClient restClient;

    @Value("${forecasting.service.url:http://localhost:8083}")
    private String forecastingServiceUrl;

    @Autowired
    public UserFeedbackService(UserFeedbackRepository userFeedbackRepository) {
        this.userFeedbackRepository = userFeedbackRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public UserFeedbackService(UserFeedbackRepository userFeedbackRepository, RestClient restClient, String forecastingServiceUrl) {
        this.userFeedbackRepository = userFeedbackRepository;
        this.restClient = restClient;
        this.forecastingServiceUrl = forecastingServiceUrl;
    }

    public UserFeedbackEntity saveFeedback(UserFeedbackDto dto) {
        log.info("Saving user feedback for query: '{}', rating: '{}', correction: '{}'",
                PiiLogSanitizer.mask(dto.getOriginalQuery()), dto.getRating(), PiiLogSanitizer.mask(dto.getUserCorrection()));

        UserFeedbackEntity entity = new UserFeedbackEntity(
                dto.getOriginalQuery(),
                dto.getUserCorrection(),
                dto.getCorrectedIntent(),
                dto.getSortField(),
                dto.getSortOrder(),
                dto.getRating()
        );
        UserFeedbackEntity saved = userFeedbackRepository.save(entity);

        // Forward feedback payload to forecasting service for active learning vector index update
        try {
            log.debug("Forwarding feedback payload to forecasting service at {}", forecastingServiceUrl);
            restClient.post()
                    .uri(forecastingServiceUrl + "/api/ai/feedback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Successfully forwarded active learning feedback payload");
        } catch (Exception e) {
            log.warn("Unable to forward feedback to forecasting service at {}: {}", forecastingServiceUrl, e.getMessage());
        }

        return saved;
    }

    public List<UserFeedbackEntity> getRecentFeedback() {
        return userFeedbackRepository.findTop100ByOrderByIdDesc();
    }
}
