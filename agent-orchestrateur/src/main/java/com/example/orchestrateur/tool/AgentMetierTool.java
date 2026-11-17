package com.example.orchestrateur.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AgentMetierTool {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AgentMetierTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("http://agent-metier:8081")
                .build();
    }

    @Tool(description = "Délègue une question sur un fournisseur à l'agent métier spécialisé via le protocole A2A")
    public String verifierFournisseur(String question) {
        String taskId = UUID.randomUUID().toString();

        Map<String, Object> part = new LinkedHashMap<>();
        part.put("type", "text");
        part.put("text", question);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("parts", List.of(part));

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", taskId);
        params.put("message", message);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("method", "tasks/send");
        request.put("params", params);
        request.put("id", UUID.randomUUID().toString());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return "Erreur: réponse vide de l'agent métier";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            if (result == null) {
                return "Erreur: résultat absent dans la réponse";
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> artifacts = (List<Map<String, Object>>) result.get("artifacts");
            if (artifacts == null || artifacts.isEmpty()) {
                return "Erreur: aucun artifact dans la réponse";
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) artifacts.get(0).get("parts");
            if (parts == null || parts.isEmpty()) {
                return "Erreur: aucune partie dans l'artifact";
            }

            Object text = parts.get(0).get("text");
            return text != null ? text.toString() : "Erreur: texte absent dans la réponse";

        } catch (Exception e) {
            return "Erreur lors de la communication avec l'agent métier: " + e.getMessage();
        }
    }
}
