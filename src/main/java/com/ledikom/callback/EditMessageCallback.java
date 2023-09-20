package com.ledikom.callback;

@FunctionalInterface
public interface EditMessageCallback {
    void execute(Long chatId, Integer messageId, String editedMessage);
}
