package com.ledikom.bot;

import com.ledikom.callback.*;
import com.ledikom.model.MessageIdInChat;
import com.ledikom.service.AdminService;
import com.ledikom.service.BotService;
import com.ledikom.service.UserService;
import com.ledikom.utils.BotCommands;
import com.ledikom.utils.City;
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

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;


@Component
public class LedikomBot extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;
    @Value("${bot.token}")
    private String botToken;
    @Value("${admin.id}")
    private Long adminId;

    private final BotService botService;
    private final UserService userService;
    private final AdminService adminService;

    private static final Logger LOGGER = LoggerFactory.getLogger(LedikomBot.class);
    private static final Map<Predicate<String>, ChatIdCallback> chatIdActions = new HashMap<>();
    private static final Map<Predicate<String>, CommandWithChatIdCallback> commandWithChatIdActions = new HashMap<>();

    public LedikomBot(@Lazy final BotService botService, @Lazy final UserService userService, @Lazy final AdminService adminService) {
        this.botService = botService;
        this.userService = userService;
        this.adminService = adminService;
    }

    @PostConstruct
    public void fillActionsMap() {
        commandWithChatIdActions.put(cmd -> cmd.startsWith("couponPreview_"),
                this.botService::sendCouponAcceptMessage);
        commandWithChatIdActions.put(cmd -> cmd.startsWith("couponAccept_"),
                this.botService::sendActivatedCouponIfCanBeUsed);
        commandWithChatIdActions.put(cmd -> cmd.startsWith(BotCommands.START.label),
                this.botService::processStartOrRefLinkFollow);
        commandWithChatIdActions.put(cmd -> cmd.startsWith("music_"),
                this.botService::processMusicRequest);
        commandWithChatIdActions.put(cmd -> cmd.startsWith("pharmacies_"),
                this.botService::sendPharmaciesInfo);
        commandWithChatIdActions.put(cmd -> cmd.startsWith("workout_"),
                this.botService::processWorkOutRequest);
        commandWithChatIdActions.put(cmd -> cmd.startsWith("gymnastics_"),
                this.botService::processGymnasticsRequest);
        commandWithChatIdActions.put(cmd -> Arrays.stream(City.values()).map(Enum::name).toList().contains(cmd),
                this.userService::setCityToUserAndAddCoupons);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.COUPONS.label),
                this.userService::sendAllCouponsList);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.DESCRIPTION.label),
                this.botService::sendBotDescription);
        chatIdActions.put(cmd -> cmd.equals("note_edit"),
                this.userService::setSendingNoteStateToUser);
        chatIdActions.put(cmd -> cmd.equals("consultation_repeat"),
                this.userService::sendConsultationShortWikiAndSetUserResponseState);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.REF_LINK.label),
                this.userService::sendReferralLinkForUser);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.TRIGGER_NEWS.label),
                this.userService::triggerReceiveNewsMessage);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.NOTES.label),
                this.userService::processNoteRequestAndBuildSendMessageList);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.PHARMACIES.label),
                this.botService::sendPharmaciesMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.DATE.label),
                this.userService::sendDateAndSetUserResponseState);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.MUSIC.label),
                this.botService::sendMusicMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.WORK_OUT.label),
                this.botService::sendWorkOutMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.GYMNASTICS.label),
                this.botService::sendGymnasticsMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.CITY.label),
                this.botService::sendCityMenu);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.PROMOTION_ACCEPT.label),
                this.botService::sendPromotionAcceptedMessage);
        chatIdActions.put(cmd -> cmd.equals(BotCommands.CONSULTATION.label),
                this.userService::sendConsultationWikiAndSetUserResponseState);
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            var msg = update.getMessage();
            var chatId = msg.getChatId();
            boolean userIsInActiveState = false;
            if (msg.hasText()) {
                try {
                    userIsInActiveState = processMessage(msg.getText(), chatId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (Objects.equals(chatId, adminId) && !userIsInActiveState) {
                try {
                    adminService.processAdminRequest(update);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (update.hasCallbackQuery()) {
            var qry = update.getCallbackQuery();
            var chatId = qry.getMessage().getChatId();
            try {
                processMessage(qry.getData(), chatId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (update.hasPoll()) {
            userService.processPoll(update.getPoll());
        }
    }

    private boolean processMessage(String command, Long chatId) throws IOException {
        boolean processed = false;

        Optional<ChatIdCallback> chatIdCallback = chatIdActions.entrySet().stream()
                .filter(entry -> entry.getKey().test(command))
                .map(Map.Entry::getValue)
                .findFirst();
        boolean isChatIdAction = chatIdCallback.isPresent();
        if (isChatIdAction) {
            chatIdCallback.get().execute(chatId);
            processed = true;
        } else {
            Optional<CommandWithChatIdCallback> commandWithChatIdCallback = commandWithChatIdActions.entrySet().stream()
                    .filter(entry -> entry.getKey().test(command))
                    .map(Map.Entry::getValue)
                    .findFirst();
            boolean isCommandWithChatIdAction = commandWithChatIdCallback.isPresent();

            if (isCommandWithChatIdAction) {
                commandWithChatIdCallback.get().execute(command, chatId);
                processed = true;
            }
        }

        boolean userIsInActiveState = userService.userIsInActiveState(chatId);

        if (!processed && userIsInActiveState) {
            userService.processStatefulUserResponse(command, chatId);
        }

        return userIsInActiveState;
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
