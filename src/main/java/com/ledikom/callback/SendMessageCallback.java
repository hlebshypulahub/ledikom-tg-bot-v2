package com.ledikom.callback;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;

@FunctionalInterface
public interface SendMessageCallback {
    void execute(BotApiMethodMessage message);
}
