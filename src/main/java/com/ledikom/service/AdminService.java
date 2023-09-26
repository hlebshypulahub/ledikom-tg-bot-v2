package com.ledikom.service;

import com.ledikom.bot.LedikomBot;
import com.ledikom.callback.GetFileFromBotCallback;
import com.ledikom.callback.SendMessageCallback;
import com.ledikom.model.NewsFromAdmin;
import com.ledikom.model.Pharmacy;
import com.ledikom.model.PromotionFromAdmin;
import com.ledikom.utils.AdminMessageToken;
import com.ledikom.utils.BotCommand;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class AdminService {

    public static final String DELIMITER = "&";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);

    @Value("${admin.id}")
    private Long adminId;
    @Value("${admin.tech-id}")
    private Long techAdminId;
    @Value("${hello-coupon.barcode}")
    private String helloCouponBarcode;

    private final BotUtilityService botUtilityService;
    private final PollService pollService;
    private final UserService userService;
    private final LedikomBot ledikomBot;
    private final CouponService couponService;
    private final PharmacyService pharmacyService;

    private SendMessageCallback sendMessageCallback;
    private GetFileFromBotCallback getFileFromBotCallback;

    public AdminService(final BotUtilityService botUtilityService, final PollService pollService, final UserService userService, final LedikomBot ledikomBot, final CouponService couponService, final PharmacyService pharmacyService) {
        this.botUtilityService = botUtilityService;
        this.pollService = pollService;
        this.userService = userService;
        this.ledikomBot = ledikomBot;
        this.couponService = couponService;
        this.pharmacyService = pharmacyService;
    }

    @PostConstruct
    public void initCallbacks() {
        this.sendMessageCallback = ledikomBot.getSendMessageCallback();
        this.getFileFromBotCallback = ledikomBot.getGetFileFromBotCallback();
    }

    public void processAdminRequest(final Update update) {
        if (update.hasMessage() && update.getMessage().hasPoll()) {
            executeAdminActionOnPollReceived(update.getMessage().getPoll());
        } else if (update.hasMessage()) {
            executeAdminActionOnMessageReceived(update.getMessage());
        }
    }

    public void executeAdminActionOnPollReceived(final Poll poll) {
        LOGGER.info("Processing admin poll request...");
        com.ledikom.model.Poll entityPoll = pollService.tgPollToLedikomPoll(poll);
        entityPoll.setLastVoteTimestamp(LocalDateTime.now());
        entityPoll = pollService.savePoll(entityPoll);
        LOGGER.info("Saved a poll:\n {}", entityPoll.toString());
        userService.sendPollToUsers(poll);
    }

    public void executeAdminActionOnMessageReceived(final Message message) {
        LOGGER.info("Processing admin message request...");

        String photoPath = botUtilityService.getPhotoFromUpdate(message, getFileFromBotCallback);
        List<String> splitStringsFromAdminMessage = getSplitStringsFromAdminMessage(getTextFromAdminMessage(message));
        String label = splitStringsFromAdminMessage.get(0);
        Optional<AdminMessageToken> tokenOptional = AdminMessageToken.getByLabel(label);

        if (tokenOptional.isPresent() && adminCommandIsValid(tokenOptional.get(), splitStringsFromAdminMessage.size())) {
            switch (tokenOptional.get()) {
                case NEWS -> userService.sendNewsToUsers(getNewsByAdmin(splitStringsFromAdminMessage, photoPath));
                case PROMOTION -> userService.sendPromotionToUsers(getPromotionFromAdmin(splitStringsFromAdminMessage, photoPath));
                case COUPON -> couponService.createAndSendNewCoupon(splitStringsFromAdminMessage, photoPath);
            }
        } else {
            sendMessageCallback.execute(botUtilityService.buildSendMessage("Неверный формат команды! Нет такого действия: " + label, adminId));
            LOGGER.error("Invalid command, no such actions: " + label);
        }
    }

    private String getTextFromAdminMessage(final Message message) {
        String text = null;

        if (botUtilityService.messageHasPhoto(message)) {
            text = message.getCaption();
        } else if (message.hasText()) {
            text = message.getText();
        }

        if (text != null && !text.isBlank()) {
            LOGGER.info("Text received from message:\n{}", text);
            return text;
        }

        sendMessageCallback.execute(botUtilityService.buildSendMessage("Неверный формат команды! Сообщение не может быть пустым!", adminId));
        throw new RuntimeException("Invalid command from admin, message cannot be blank");
    }

    private boolean adminCommandIsValid(final AdminMessageToken adminMessageToken, final int splitStringsSize) {
        if (splitStringsSize == adminMessageToken.commandSize) {
            return true;
        } else {
            sendMessageCallback.execute(botUtilityService.buildSendMessage("Неверный формат команды! Количество аргументов для *" + adminMessageToken.label + "* должно быть равно *" + adminMessageToken.commandSize + "*", adminId));
            throw new RuntimeException("Invalid command from admin, arguments list size not equal to " + adminMessageToken.commandSize + ", was calling command: " + adminMessageToken.label);
        }
    }

    private List<String> getSplitStringsFromAdminMessage(final String messageFromAdmin) {
        List<String> splitStringsFromAdminMessage = new ArrayList<>(Arrays.stream(messageFromAdmin.split(DELIMITER)).map(String::trim).toList());

        if (splitStringsFromAdminMessage.size() == 1) {
            sendMessageCallback.execute(botUtilityService.buildSendMessage("Неверный формат команды! Не обнаруженно разделителя: *" + DELIMITER + "*", adminId));
            throw new RuntimeException("Invalid command format, no delimiter detected: " + DELIMITER);
        }

        LOGGER.info("Split strings generated:\n{}", splitStringsFromAdminMessage);

        return splitStringsFromAdminMessage;
    }

    private NewsFromAdmin getNewsByAdmin(final List<String> splitStringsFromAdminMessage, final String photoPath) {
        return new NewsFromAdmin(splitStringsFromAdminMessage.get(1), photoPath);
    }

    private PromotionFromAdmin getPromotionFromAdmin(final List<String> splitStringsFromAdminMessage, final String photoPath) {
        List<Pharmacy> pharmacies = pharmacyService.getPharmaciesFromIdsString(splitStringsFromAdminMessage.get(1));
        return new PromotionFromAdmin(pharmacies, splitStringsFromAdminMessage.get(2), photoPath);
    }
}
