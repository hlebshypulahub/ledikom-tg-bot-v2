package com.ledikom.callback;

import com.ledikom.model.MessageIdInChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@FunctionalInterface
public interface SendCouponCallback {
    MessageIdInChat execute(SendMessage sm);
}
