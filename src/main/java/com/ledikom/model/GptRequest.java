package com.ledikom.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GptRequest {
    private final String model = "gpt-3.5-turbo";
    List<GptMessage> messages = new ArrayList<>(List.of(new GptMessage("system", "Если вопрос не связан с медициной или здоровьем, отвечай '" + GptMessage.NON_RELATED_RESPONSE_TOKEN + "'. Если вопрос слишкой общий, то предлагай задать более развернутый вопрос. Ограничь ответ до 200 слов.")));
    private final double temperature = 0.1;
    @JsonProperty("max_tokens")
    private final int maxTokens = 512;
}

