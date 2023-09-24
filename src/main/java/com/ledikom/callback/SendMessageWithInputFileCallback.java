package com.ledikom.callback;

import com.ledikom.model.MessageIdInChat;
import org.telegram.telegrambots.meta.api.objects.InputFile;

@FunctionalInterface
public interface SendMessageWithInputFileCallback {
    MessageIdInChat execute(InputFile inputFile, String caption, Long chatId);
}
