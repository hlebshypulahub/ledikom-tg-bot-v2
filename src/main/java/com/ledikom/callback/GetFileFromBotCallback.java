package com.ledikom.callback;

import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@FunctionalInterface
public interface GetFileFromBotCallback {
    File execute(GetFile getFileRequest) throws TelegramApiException;
}
