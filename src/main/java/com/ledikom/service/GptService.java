package com.ledikom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledikom.model.GptMessage;
import com.ledikom.model.GptRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GptService {

    @Value("${GPT_TOKEN}")
    private String gptToken;

    private final RestTemplate restTemplate;

    public GptService(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getResponse(final String text) throws JsonProcessingException {
        GptRequest gptRequest = new GptRequest();
        gptRequest.getMessages().add(new GptMessage("user", text));

        return getResponse(gptRequest);
    }

    private String getResponse(final GptRequest gptRequest) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + gptToken);

        HttpEntity<GptRequest> requestEntity = new HttpEntity<>(gptRequest, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity("https://api.openai.com/v1/chat/completions", requestEntity, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody());

        return jsonNode
                .at("/choices/0/message/content")
                .asText();
    }
}
