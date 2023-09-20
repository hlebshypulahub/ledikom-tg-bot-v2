package com.ledikom.callback;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;

@FunctionalInterface
public interface DeleteMessageCallback {
    void execute(DeleteMessage deleteMessage);
}
