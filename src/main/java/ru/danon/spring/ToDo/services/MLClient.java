package ru.danon.spring.ToDo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collections;
import java.util.List;

@Component
public class MLClient {

    private static final Logger logger = LoggerFactory.getLogger(MLClient.class);

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MLClient(@Value("${ml.service.url:http://localhost:8000}") String mlServiceUrl) {
        this.mlServiceUrl = mlServiceUrl;
        this.restTemplate = new RestTemplate();

        logger.info("MLClient initialized with URL: {}", mlServiceUrl);
    }

    /**
     * Получить предсказанные теги от ML сервиса
     */
    public List<String> predictTags(String title, String description) {
        // Если ML сервис не настроен, возвращаем пустой список
        if (mlServiceUrl == null || mlServiceUrl.isEmpty()) {
            logger.debug("ML service URL not configured, skipping tag prediction");
            return Collections.emptyList();
        }

        try {
            MLRequest request = new MLRequest(title, description);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MLRequest> entity = new HttpEntity<>(request, headers);

            logger.debug("Sending ML prediction request for title: {}", title);

            ResponseEntity<MLResponse> response = restTemplate.exchange(
                    mlServiceUrl + "/api/predict-tags",
                    HttpMethod.POST,
                    entity,
                    MLResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<String> suggestedTags = response.getBody().getSuggestedTags();
                logger.info("ML service returned {} suggested tags: {}", suggestedTags.size(), suggestedTags);
                return suggestedTags;
            } else {
                logger.warn("ML service returned non-OK response: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (ResourceAccessException e) {
            logger.warn("ML service is unavailable at {}. Proceeding without tag predictions.", mlServiceUrl);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error calling ML service: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Проверить доступность ML сервиса
     */
    public boolean isServiceAvailable() {
        if (mlServiceUrl == null || mlServiceUrl.isEmpty()) {
            return false;
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(mlServiceUrl + "/health", String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.debug("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }

    // DTO классы для ML сервиса
    public static class MLRequest {
        private String title;
        private String description;

        public MLRequest() {}

        public MLRequest(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class MLResponse {
        private List<String> suggestedTags;
        private List<Double> confidenceScores;

        public MLResponse() {}

        public List<String> getSuggestedTags() {
            return suggestedTags != null ? suggestedTags : Collections.emptyList();
        }

        public void setSuggestedTags(List<String> suggestedTags) {
            this.suggestedTags = suggestedTags;
        }

        public List<Double> getConfidenceScores() {
            return confidenceScores;
        }

        public void setConfidenceScores(List<Double> confidenceScores) {
            this.confidenceScores = confidenceScores;
        }
    }
}