package com.ledikom.model;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;

@Getter
@Setter
public class RequestFromAdmin {
    private String message;
    private String photoPath;
    private Poll poll;

    public boolean isPoll() {
        return poll != null;
    }
}

