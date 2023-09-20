package com.ledikom.callback;

@FunctionalInterface
public interface ChatIdCallback {
    void execute(long chatId);
}
