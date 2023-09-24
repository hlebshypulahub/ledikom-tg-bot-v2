package com.ledikom.callback;

import com.ledikom.model.MessageIdInChat;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@FunctionalInterface
public interface SendMessageWithPhotoCallback {
    MessageIdInChat execute(String photoPath, String caption, Long chatId);
}
