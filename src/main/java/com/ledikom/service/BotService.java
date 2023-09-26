package com.ledikom.service;

import com.ledikom.bot.LedikomBot;
import com.ledikom.callback.SendMessageCallback;
import com.ledikom.callback.SendMessageWithInputFileCallback;
import com.ledikom.callback.SendMusicFileCallback;
import com.ledikom.model.*;
import com.ledikom.utils.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BotService {

    public static final Map<MessageIdInChat, LocalDateTime> messagesToDeleteMap = new HashMap<>();
    public static final EventCollector eventCollector = new EventCollector();

    private static final Logger LOGGER = LoggerFactory.getLogger(BotService.class);

    @Value("${bot.username}")
    private String botUsername;
    @Value("${hello-coupon.barcode}")
    private String helloCouponBarcode;
    @Value("${date-coupon.barcode}")
    private String dateCouponBarcode;
    @Value("${ref-coupon.barcode}")
    private String refCouponBarcode;
    @Value("${coupon.duration-minutes}")
    private int couponDurationInMinutes;
    @Value("${admin.id}")
    private Long adminId;
    @Value("${admin.tech-id}")
    private Long techAdminId;

    private final UserService userService;
    private final CouponService couponService;
    private final BotUtilityService botUtilityService;
    private final PharmacyService pharmacyService;
    private final LedikomBot ledikomBot;

    private SendMessageCallback sendMessageCallback;
    private SendMusicFileCallback sendMusicFileCallback;
    private SendMessageWithInputFileCallback sendMessageWithInputFileCallback;

    public BotService(final UserService userService, final CouponService couponService, final BotUtilityService botUtilityService, final PharmacyService pharmacyService, @Lazy final LedikomBot ledikomBot) {
        this.userService = userService;
        this.couponService = couponService;
        this.botUtilityService = botUtilityService;
        this.pharmacyService = pharmacyService;
        this.ledikomBot = ledikomBot;
    }

    @PostConstruct
    public void initCallbacks() {
        this.sendMessageCallback = ledikomBot.getSendMessageCallback();
        this.sendMusicFileCallback = ledikomBot.getSendMusicFileCallback();
        this.sendMessageWithInputFileCallback = ledikomBot.getSendMessageWithInputFileCallback();
        this.sendMessageCallback.execute(botUtilityService.buildSendMessage("Application started.", techAdminId));
    }

    public void processStartOrRefLinkFollow(final String command, final Long chatId) {
        boolean isRefLink = !command.endsWith(BotCommand.START.label);
        if (isRefLink) {
            LOGGER.info("Processing ref link following: {}", command);
            String refCode = command.substring(7);
            userService.incrementUserRefCounter(Long.parseLong(refCode), chatId);
        }
        addUserAndSendHelloMessage(chatId);
    }

    public void processMusicRequest(final String command, final long chatId) {
        LOGGER.info("Processing music request: {}", command);

        MusicCallbackRequest musicCallbackRequest = UtilityHelper.getMusicCallbackRequest(command);

        if (musicCallbackRequest.readyToPlay()) {
            String audioFileName = command + ".mp3";
            InputStream audioInputStream = getClass().getResourceAsStream("/" + audioFileName);
            InputFile audioInputFile = new InputFile(audioInputStream, audioFileName);
            SendAudio sendAudio = new SendAudio(String.valueOf(chatId), audioInputFile);
            LocalDateTime toDeleteTimestamp = LocalDateTime.now().plusMinutes(musicCallbackRequest.getDuration()).plusSeconds(30);
            MessageIdInChat messageIdInChatMusic = sendMusicFileCallback.execute(sendAudio);
            LOGGER.info("Message to delete put to map: {}, {}", messageIdInChatMusic, toDeleteTimestamp);
            messagesToDeleteMap.put(messageIdInChatMusic, toDeleteTimestamp);
            eventCollector.incrementMusic();
        } else {
            String imageName = musicCallbackRequest.getStyleString() + ".jpg";
            InputStream inputStream = getClass().getResourceAsStream("/" + imageName);
            InputFile inputFile = new InputFile(inputStream, imageName);
            sendMessageWithInputFileCallback.execute(inputFile, BotResponses.goodNight(), chatId);
            var sm = botUtilityService.buildSendMessage(BotResponses.musicDurationMenu(), chatId);
            botUtilityService.addMusicDurationButtonsToSendMessage(sm, command);
            sendMessageCallback.execute(sm);
        }
    }

    public void processWorkOutRequest(final String command, final Long chatId) {
        LOGGER.info("Processing work out request: {}", command);

        WorkOutMenuItem workOutMenuItem = Arrays.stream(WorkOutMenuItem.values())
                .filter(value -> value.callbackDataString.equals(command))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No work out menu item found: " + command));

        sendMessageCallback.execute(botUtilityService.buildSendMessage(BotResponses.video(workOutMenuItem.videoLink), chatId));

        eventCollector.incrementWorkOut();
    }

    public void processGymnasticsRequest(final String command, final Long chatId) {
        LOGGER.info("Processing gymnastics request: {}", command);

        GymnasticsMenuItem gymnasticsMenuItem = Arrays.stream(GymnasticsMenuItem.values())
                .filter(value -> value.callbackDataString.equals(command))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No gymnastics menu found: " + command));

        sendMessageCallback.execute(botUtilityService.buildSendMessage(BotResponses.video(gymnasticsMenuItem.videoLink), chatId));

        eventCollector.incrementGymnastics();
    }

    public void sendMusicMenu(final long chatId) {
        var sm = botUtilityService.buildSendMessage(BotResponses.musicMenu(), chatId);
        botUtilityService.addMusicMenuButtonsToSendMessage(sm);
        sendMessageCallback.execute(sm);
    }

    public void sendWorkOutMenu(final long chatId) {
        var sm = botUtilityService.buildSendMessage(BotResponses.workOutMenu(), chatId);
        botUtilityService.addWorkOutMenuButtonsToSendMessage(sm);
        sendMessageCallback.execute(sm);
    }

    public void sendGymnasticsMenu(final long chatId) {
        var sm = botUtilityService.buildSendMessage(BotResponses.gymnasticsMenu(), chatId);
        botUtilityService.addGymnasticsMenuButtonsToSendMessage(sm);
        sendMessageCallback.execute(sm);
    }

    private void addUserAndSendHelloMessage(final long chatId) {
        if (!userService.userExistsByChatId(chatId)) {
            userService.addNewUser(chatId);
            var sm = botUtilityService.buildSendMessage(BotResponses.startMessage(), chatId);
            botUtilityService.addHelloMessageButtons(sm, couponService.getHelloCoupon());
            sendMessageCallback.execute(sm);

            sm = botUtilityService.buildSendMessage(BotResponses.chooseYourCity(), chatId);
            pharmacyService.addCitiesButtons(sm);
            sendMessageCallback.execute(sm);

            eventCollector.incrementNewUser();
        }
    }

    public void sendCouponToAcceptMessage(final String couponCommand, final long chatId) {
        User user = userService.findByChatId(chatId);
        Coupon coupon = couponService.findCouponForUserByCommand(user, couponCommand);

        SendMessage sm;
        if (couponService.couponCanBeUsedNow(coupon)) {
            boolean inAllPharmacies = pharmacyService.findAll().size() == coupon.getPharmacies().size();
            sm = botUtilityService.buildSendMessage(BotResponses.couponAcceptMessage(coupon, inAllPharmacies, couponDurationInMinutes), chatId);
            botUtilityService.addAcceptCouponButton(sm, coupon);
        } else {
            LOGGER.error("Coupon is not active for user: {}", chatId);
            sm = botUtilityService.buildSendMessage(BotResponses.couponIsNotActive(), chatId);
        }
        sendMessageCallback.execute(sm);
    }

    public void sendActivatedCouponIfCanBeUsed(final String couponCommand, final Long chatId) {
        User user = userService.findByChatId(chatId);
        Coupon coupon = couponService.findCouponForUserByCommand(user, couponCommand);

        byte[] barcodeImageByteArray = coupon.getBarcodeImageByteArray();
        InputFile barcodeInputFile = new InputFile(new ByteArrayInputStream(barcodeImageByteArray), "barcode.jpg");

        String couponTextWithBarcodeAndTimeSign = "Действителен до: *" + couponService.getTimeSign() + "*" + "\n\n" + coupon.getBarcode() + "\n\n" + coupon.getText();

        MessageIdInChat messageIdInChat = sendMessageWithInputFileCallback.execute(barcodeInputFile, BotResponses.initialCouponText(couponTextWithBarcodeAndTimeSign, couponDurationInMinutes), chatId);
        LOGGER.info("Adding coupon to deletion map: {}, {}", messageIdInChat, coupon.getName());
        couponService.addCouponToDeletionMap(messageIdInChat, couponTextWithBarcodeAndTimeSign);
        userService.markCouponAsUsedForUser(user, coupon);

        eventCollector.incrementCoupon();
        if (coupon.getBarcode().equals(helloCouponBarcode)) {
            eventCollector.incrementHelloCoupon();
        }
        if (coupon.getBarcode().equals(dateCouponBarcode)) {
            eventCollector.incrementDateCoupon();
        }
        if (coupon.getBarcode().equalsIgnoreCase(refCouponBarcode)) {
            eventCollector.incrementRefCoupon();
        }
    }

    public void sendCityMenu(final long chatId) {
        User user = userService.findByChatId(chatId);
        var sm = botUtilityService.buildSendMessage(BotResponses.yourCityAndCanUpdate(user.getCity()), chatId);
        pharmacyService.addCitiesButtons(sm);
        sendMessageCallback.execute(sm);
    }

    public void sendPromotionAcceptedMessage(final long chatId) {
        sendMessageCallback.execute(botUtilityService.buildSendMessage(BotResponses.promotionAccepted(), chatId));
        eventCollector.incrementPromotion();
    }

    public void sendPharmaciesMenu(final long chatId) {
        User user = userService.findByChatId(chatId);
        SendMessage sm;
        if (user.getCity() == null) {
            sm = botUtilityService.buildSendMessage(BotResponses.chooseCityToFilterPharmacies(), chatId);
            pharmacyService.addPharmaciesFilterCitiesButtons(sm);
            sendMessageCallback.execute(sm);
        } else {
            sendPharmaciesInfo(user);
        }
    }

    private void sendPharmaciesInfo(final User user) {
        List<Pharmacy> pharmacies = pharmacyService.findAll().stream().filter(pharmacy -> pharmacy.getCity() == user.getCity()).toList();
        sendMessageCallback.execute(botUtilityService.buildSendMessage(BotResponses.pharmaciesInfo(pharmacies), user.getChatId()));
    }

    public void sendPharmaciesInfoOnUserCityIsNull(final String command, final long chatId) {
        List<Pharmacy> pharmacies = pharmacyService.findAll().stream().filter(pharmacy -> pharmacy.getCity() == City.valueOf(command.split("_")[1])).toList();
        var sm = botUtilityService.buildSendMessage(BotResponses.pharmaciesInfo(pharmacies), chatId);
        sendMessageCallback.execute(sm);
        sendCityMenu(chatId);
    }

    public void sendBotDescription(final long chatId) {
        sendMessageCallback.execute(botUtilityService.buildSendMessage(BotResponses.botDescription(), chatId));
    }

    public void sendHealthBeingMenu(final long chatId) {
        var sm = botUtilityService.buildSendMessage("Проводите время с пользой для здоровья \uD83D\uDC9A\uD83E\uDD0D\uD83D\uDC9A", chatId);
        botUtilityService.addHealthBeingButtons(sm);
        sendMessageCallback.execute(sm);
    }

    public void sendSettingsMenu(final long chatId) {
        var sm = botUtilityService.buildSendMessage("Настройте полезные функции \uD83D\uDC9A\uD83E\uDD0D\uD83D\uDC9A", chatId);
        botUtilityService.addSettingsButtons(sm);
        sendMessageCallback.execute(sm);
    }

    public void sendInfoMenu(final long chatId) {
        var sm = botUtilityService.buildSendMessage("Информация ❕", chatId);
        botUtilityService.addInfoButtons(sm);
        sendMessageCallback.execute(sm);
    }
}
