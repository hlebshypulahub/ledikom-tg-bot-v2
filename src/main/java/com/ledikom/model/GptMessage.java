package com.ledikom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GptMessage {

    public static final String NON_RELATED_RESPONSE_TOKEN = "нет";
    public static final int MAX_USER_CONTENT_LENGTH = 300;

    private String role;
    private String content;
}
