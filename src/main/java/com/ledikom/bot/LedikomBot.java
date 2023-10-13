package com.ledikom.bot;

import com.ledikom.callback.*;
import com.ledikom.model.MessageIdInChat;
import com.ledikom.service.*;
import com.ledikom.utils.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.function.Predicate;


@Component
public class LedikomBot extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;
    @Value("${bot.token}")
    private String botToken;
    @Value("${admin.id}")
    private long adminId;

    private final BotService botService;
    private final UserService userService;
    private final AdminService adminService;
    private final BotUtilityService botUtilityService;

    private static final Logger LOGGER = LoggerFactory.getLogger(LedikomBot.class);
    private static final Map<Predicate<String>, ChatIdCallback> chatIdActions = new HashMap<>();
    private static final Map<Predicate<String>, CommandWithChatIdCallback> commandWithChatIdActions = new HashMap<>();

    public LedikomBot(@Lazy final BotService botService, @Lazy final UserService userService, @Lazy final AdminService adminService, final BotUtilityService botUtilityService) {
        this.botService = botService;
        this.userService = userService;
        this.adminService = adminService;
        this.botUtilityService = botUtilityService;
    }

    @PostConstruct
    public void fillActionsMap() {
        commandWithChatIdActions.put(cmd -> cmd.startsWith(CouponService.COUPON_PREVIEW_BUTTON_CALLBACK_STRING), this.botService::sendCouponAcceptMessage);
        commandWithChatIdActions.put(cmd -> cmd.startsWith(CouponService.COUPON_ACCEPT_BUTTON_CALLBACK_STRING), this.botService::sendActivatedCouponIfCanBeUsed);
        commandWithChatIdActions.put(cmd -> cmd.startsWith(BotCommand.START.label), this.botService::processStartOrRefLinkFollow);
        commandWithChatIdActions.put(cmd -> cmd.startsWith(MusicMenuItem.MUSIC_BUTTON_CALLBACK_STRING), this.botService::processMusicRequest);
        commandWithChatIdActions.put(cmd -> cmd.startsWith(PharmacyService.PHARMACIES_BUTTON_CALLBACK_STRING), this.botService::sendPharmaciesInfo);
        commandWithChatIdActions.put(cmd -> cmd.startsWith(WorkOutMenuItem.WORK_OUT_BUTTON_CALLBACK_STRING), this.botService::processWorkOutRequest);
        commandWithChatIdActions.put(cmd -> cmd.startsWith(GymnasticsMenuItem.GYMNASTICS_BUTTON_CALLBACK_STRING), this.botService::processGymnasticsRequest);
        commandWithChatIdActions.put(cmd -> Arrays.stream(City.values()).map(Enum::name).toList().contains(cmd), this.userService::setCityToUserAndAddCoupons);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.COUPONS.label), this.userService::sendAllCouponsList);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.DESCRIPTION.label), this.botService::sendBotDescription);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.EDIT_NOTES.label), this.userService::setSendingNoteStateToUser);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.REF_LINK.label), this.userService::sendReferralLinkForUser);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.TRIGGER_NEWS.label), this.userService::triggerReceiveNewsMessage);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.NOTES.label), this.userService::processNoteRequestAndBuildSendMessageList);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.PHARMACIES.label), this.botService::sendPharmaciesMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.DATE.label), this.userService::sendDateAndSetUserResponseState);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.MUSIC.label), this.botService::sendMusicMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.HEALTH_BEING.label), this.botService::sendHealthBeingMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.WORK_OUT.label), this.botService::sendWorkOutMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.GYMNASTICS.label), this.botService::sendGymnasticsMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.CITY.label), this.botService::sendCityMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.PROMOTION_ACCEPT.label), this.botService::sendPromotionAcceptedMessage);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.CONSULTATION_WIKI.label), this.userService::sendConsultationWiki);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.CONSULTATION_ASK.label), this.userService::sendConsultationShortWikiAndSetUserResponseState);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.CONSULTATION_MENU.label), this.userService::sendConsultationMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.SETTINGS.label), this.botService::sendSettingsMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.INFO.label), this.botService::sendInfoMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommand.ADMIN_EVENTS.label), this.adminService::sendEventsToAdmin);
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            var msg = update.getMessage();
            long chatId = msg.getChatId();
            boolean messageProcessed = false;
            if (msg.hasText()) {
                messageProcessed = processMessage(msg.getText(), chatId);
            }
            if (chatId == adminId && !messageProcessed) {
                adminService.processAdminRequest(update);
            }

        } else if (update.hasCallbackQuery()) {
            var qry = update.getCallbackQuery();
            var chatId = qry.getMessage().getChatId();
            processMessage(qry.getData(), chatId);

        } else if (update.hasPoll()) {
            userService.processPoll(update.getPoll());
        }
    }

    private boolean processChatIdActions(final String command, final long chatId) {
        return chatIdActions.entrySet().stream()
                .filter(entry -> entry.getKey().test(command))
                .findFirst()
                .map(entry -> {
                    entry.getValue().execute(chatId);
                    return true;
                })
                .orElse(false);
    }

    private boolean processCommandWithChatIdActions(final String command, final long chatId) {
        return commandWithChatIdActions.entrySet().stream()
                .filter(entry -> entry.getKey().test(command))
                .findFirst()
                .map(entry -> {
                    entry.getValue().execute(command, chatId);
                    return true;
                })
                .orElse(false);
    }

    private boolean processMessage(String command, Long chatId) {
        if (processChatIdActions(command, chatId) || processCommandWithChatIdActions(command, chatId)) {
            return true;
        }

        if (userService.userIsInActiveState(chatId)) {
            return userService.processStatefulUserResponse(command, chatId);
        }

        return false;
    }

    public SendMessageWithPhotoCallback getSendMessageWithPhotoCallback() {
        return this::sendImageWithCaption;
    }

    public GetFileFromBotCallback getGetFileFromBotCallback() {
        return this::getFileFromBot;
    }

    public SendMessageCallback getSendMessageCallback() {
        return this::sendMessage;
    }

    public EditMessageCallback getEditMessageCallback() {
        return this::editMessage;
    }

    public SendMusicFileCallback getSendMusicFileCallback() {
        return this::sendMusicFile;
    }

    public DeleteMessageCallback getDeleteMessageCallback() {
        return this::deleteMessage;
    }

    public EditMessageCallback getEditMessageWithPhotoCallback() {
        return this::editImageCaptionByMessageId;
    }

    public SendMessageWithInputFileCallback getSendMessageWithInputFileCallback() {
        return this::sendImageWithCaption;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private File getFileFromBot(final GetFile getFileRequest) throws TelegramApiException {
        return execute(getFileRequest);
    }

    private MessageIdInChat sendImageWithCaption(final String photoPath, final String caption, final Long chatId) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(botUtilityService.getPhotoInputFile(photoPath));
            sendPhoto.setCaption(caption);
            sendPhoto.setParseMode("Markdown");
            Message sentMessage = execute(sendPhoto);
            return new MessageIdInChat(sentMessage.getChatId(), sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            LOGGER.trace(e.getMessage());
            return null;
        }
    }

    private MessageIdInChat sendImageWithCaption(final InputFile inputFile, final String caption, final Long chatId) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setCaption(caption);
            sendPhoto.setParseMode("Markdown");
            Message sentMessage = execute(sendPhoto);
            return new MessageIdInChat(sentMessage.getChatId(), sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            LOGGER.trace(e.getMessage());
            return null;
        }
    }

    private void sendMessage(BotApiMethodMessage message) {
        try {
            if (message != null) {
                execute(message);
            }
        } catch (Exception e) {
            LOGGER.trace(e.getMessage());
        }
    }

    private MessageIdInChat sendMusicFile(final SendAudio sendAudio) {
        try {
            Message sentMessage = execute(sendAudio);
            return new MessageIdInChat(sentMessage.getChatId(), sentMessage.getMessageId());
        } catch (Exception e) {
            LOGGER.trace(e.getMessage());
            return null;
        }
    }

    private void deleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            LOGGER.trace(e.getMessage());
        }
    }

    private void editMessage(final Long chatId, final Integer messageId, final String editedMessage) {
        var editMessageText = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(editedMessage)
                .parseMode("Markdown")
                .build();

        try {
            execute(editMessageText);
        } catch (Exception e) {
            LOGGER.trace(e.getMessage());
        }
    }

    public void editImageCaptionByMessageId(final Long chatId, final int messageId, final String newCaption) {
        EditMessageCaption editMessageCaption = EditMessageCaption.builder()
                .chatId(chatId)
                .messageId(messageId)
                .caption(newCaption)
                .parseMode("Markdown")
                .build();

        try {
            execute(editMessageCaption);
        } catch (Exception e) {
            LOGGER.trace(e.getMessage());
        }
    }
}
