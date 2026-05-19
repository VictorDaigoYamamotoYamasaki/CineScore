package com.cinescore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cinescore.exception.ContentModerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class ModerationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${huggingface.api.key}")
    private String apiKey;

    @Value("${huggingface.api.url}")
    private String apiUrl;

    private static final double THRESHOLD = 0.75;

    // Timeout alto para suportar cold start do modelo (pode levar ~30s)
    public ModerationService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    public boolean isOffensive(String text) {
        if (text == null || text.isBlank() || text.length() < 3) return false;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // wait_for_model: true faz a API aguardar o modelo carregar (cold start)
            // em vez de retornar 503 imediatamente
            Map<String, Object> body = Map.of(
                    "inputs", text,
                    "options", Map.of("wait_for_model", true)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, request, String.class);

            String json = response.getBody();
            log.debug("HuggingFace response: {}", json);

            if (json == null || json.isBlank()) return false;

            // Resposta: [[{"label":"offensive","score":0.95},{"label":"not-offensive","score":0.05}]]
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.isArray() && root.size() > 0 ? root.get(0) : root;

            if (results.isArray()) {
                for (JsonNode item : results) {
                    String label = item.path("label").asText("");
                    double score = item.path("score").asDouble(0);
                    if ("offensive".equalsIgnoreCase(label) && score >= THRESHOLD) {
                        log.info("Conteúdo ofensivo (score={:.2f}): {}",
                                score, text.substring(0, Math.min(60, text.length())));
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Moderação indisponível ({}), conteúdo permitido.", e.getMessage());
        }
        return false;
    }

    public void verificar(String texto, String contexto) {
        if (isOffensive(texto)) {
            throw new ContentModerationException(contexto);
        }
    }
}
