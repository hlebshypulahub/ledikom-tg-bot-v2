package com.ledikom.service;

import com.ledikom.bot.LedikomBot;
import com.ledikom.callback.DeleteMessageCallback;
import com.ledikom.callback.EditMessageCallback;
import com.ledikom.callback.SendMessageCallback;
import com.ledikom.model.MessageIdInChat;
import com.ledikom.model.UserCouponRecord;
import com.ledikom.repository.EventCollectorRepository;
import com.ledikom.utils.BotResponses;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// TODO: add logs
@Service
public class ScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleService.class);

    private static final int DELETION_EPSILON_SECONDS = 5;

    @Value("${admin.id}")
    private Long adminId;

    private final BotUtilityService botUtilityService;
    private final PollService pollService;
    private final CouponService couponService;
    private final UserService userService;
    private final LedikomBot ledikomBot;
    private final EventCollectorRepository eventCollectorRepository;

    public ScheduleService(final BotUtilityService botUtilityService, final PollService pollService, final CouponService couponService, final UserService userService, final LedikomBot ledikomBot, final EventCollectorRepository eventCollectorRepository) {
        this.botUtilityService = botUtilityService;
        this.pollService = pollService;
        this.couponService = couponService;
        this.userService = userService;
        this.ledikomBot = ledikomBot;
        this.eventCollectorRepository = eventCollectorRepository;
    }

    private SendMessageCallback sendMessageCallback;
    private EditMessageCallback editMessageWithPhotoCallback;
    private DeleteMessageCallback deleteMessageCallback;

    @PostConstruct
    public void initCallbacks() {
        this.sendMessageCallback = ledikomBot.getSendMessageCallback();
        this.editMessageWithPhotoCallback = ledikomBot.getEditMessageWithPhotoCallback();
        this.deleteMessageCallback = ledikomBot.getDeleteMessageCallback();
    }

    @Scheduled(cron = "0 0 8-20 * * *", zone = "GMT+3")
    public void sendEventsToAdmin() {
        BotService.eventCollector.setTimestamp(LocalDateTime.now());
        eventCollectorRepository.save(BotService.eventCollector);
        sendMessageCallback.execute(botUtilityService.buildSendMessage(BotService.eventCollector.messageToAdmin(), adminId));
        BotService.eventCollector.reset();
    }

    @Scheduled(fixedRate = 1000)
    public void processCouponsInMap() {
        CouponService.userCoupons.entrySet().removeIf(userCoupon -> {
            long timeLeftInSeconds = (userCoupon.getValue().getExpiryTimestamp() - System.currentTimeMillis()) / 1000;
            if (timeLeftInSeconds > 0) {
                updateCouponTimerAndMessage(userCoupon.getKey(), userCoupon.getValue(), timeLeftInSeconds);
                return false;
            }
            deleteCouponTimerAndMessage(userCoupon.getKey());
            return true;
        });
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "GMT+3")
    public void deleteExpiredStaleCouponsFromUser() {
        userService.deleteExpiredStaleCouponsFromUser();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "GMT+3")
    public void deleteExpiredCoupons() {
        couponService.deleteExpiredCoupons();
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "GMT+3")
    public void addFreshCoupons() {
        couponService.addCouponsToUsersOnFirstActiveDay();
    }

    @Scheduled(cron = "0 15 8 * * *", zone = "GMT+3")
    public void addDateCoupons() {
        couponService.addDateCouponToUsers();
    }

    @Scheduled(fixedRate = 1000 * 10)
    public void processMessagesToDeleteInMap() {
        try {
            LocalDateTime checkpointTimestamp = LocalDateTime.now().plusSeconds(DELETION_EPSILON_SECONDS);
            BotService.messagesToDeleteMap.entrySet().removeIf(entry -> {
                if (entry.getValue().isBefore(checkpointTimestamp)) {
                    DeleteMessage deleteMessage = DeleteMessage.builder()
                            .chatId(entry.getKey().getChatId())
                            .messageId(entry.getKey().getMessageId())
                            .build();
                    deleteMessageCallback.execute(deleteMessage);
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 1000 * 10)
    public void resetUserStateIfNoResponseAfterTime() {
        try {
            LocalDateTime checkpointTimestamp = LocalDateTime.now();

            List<Long> keysToRemove = UserService.userStatesToReset.entrySet().stream()
                    .filter(entry -> entry.getValue().isBefore(checkpointTimestamp))
                    .map(Map.Entry::getKey)
                    .toList();

            keysToRemove.forEach(key -> {
                var sm = botUtilityService.buildSendMessage(BotResponses.responseTimeExceeded(), key);
                botUtilityService.addRepeatConsultationButton(sm);
                sendMessageCallback.execute(sm);
                userService.resetUserState(key);
            });

            keysToRemove.forEach(UserService.userStatesToReset::remove);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 0 8-19 * * *", zone = "GMT+3")
    public void sendPollInfoToAdmin() {
        SendMessage sm = botUtilityService.buildSendMessage(pollService.getPollsInfoForAdmin(), adminId);
        sendMessageCallback.execute(sm);
    }

    private void updateCouponTimerAndMessage(final MessageIdInChat messageIdInChat, final UserCouponRecord userCouponRecord, final long timeLeftInSeconds) {
        editMessageWithPhotoCallback.execute(messageIdInChat.getChatId(), messageIdInChat.getMessageId(), BotResponses.updatedCouponText(userCouponRecord, timeLeftInSeconds));
    }

    private void deleteCouponTimerAndMessage(final MessageIdInChat messageIdInChat) {
        deleteMessageCallback.execute(DeleteMessage.builder()
                .chatId(messageIdInChat.getChatId())
                .messageId(messageIdInChat.getMessageId())
                .build());
        sendMessageCallback.execute(botUtilityService.buildSendMessage(BotResponses.couponExpiredMessage(), messageIdInChat.getChatId()));
    }
}
