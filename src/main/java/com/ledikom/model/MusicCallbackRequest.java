package com.ledikom.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MusicCallbackRequest {
    private String command;
    private int duration;
    private String styleString;

    public MusicCallbackRequest(final String command) {
        this.command = command;
    }

    public boolean readyToPlay() {
        return command != null && duration != 0;
    }
}
