package com.ledikom.callback;

import com.ledikom.model.MessageIdInChat;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;

@FunctionalInterface
public interface SendMusicFileCallback {
    MessageIdInChat execute(SendAudio sendAudio);
}
