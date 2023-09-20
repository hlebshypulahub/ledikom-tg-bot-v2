package com.ledikom.utils;

import com.ledikom.bot.LedikomBot;
import com.ledikom.callback.SendMessageCallback;
import com.ledikom.service.BotUtilityService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;


@Component
public class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

    @Value("${admin.tech-id}")
    private Long techAdminId;

    private final LedikomBot ledikomBot;
    private final BotUtilityService botUtilityService;
    private SendMessageCallback sendMessageCallback;

    public ShutdownListener(final LedikomBot ledikomBot, final BotUtilityService botUtilityService) {
        this.ledikomBot = ledikomBot;
        this.botUtilityService = botUtilityService;
    }

    @PostConstruct
    public void initCallbacks() {
        this.sendMessageCallback = ledikomBot.getSendMessageCallback();
    }

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
        sendMessageCallback.execute(botUtilityService.buildSendMessage("Application is shutting down!", techAdminId));
    }

}
