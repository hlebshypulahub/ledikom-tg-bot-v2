package com.ledikom.callback;

import java.io.IOException;

@FunctionalInterface
public interface CommandWithChatIdCallback {
    void execute(String command, Long chatId) throws IOException;
}
