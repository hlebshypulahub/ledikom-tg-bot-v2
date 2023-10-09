package com.ledikom.service;

import com.ledikom.callback.GetFileFromBotCallback;
import com.ledikom.model.Coupon;
import com.ledikom.model.User;
import com.ledikom.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BotUtilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotUtilityService.class);

    @Value("${bot.token}")
    private String botToken;

    @Value("${hello-coupon.barcode}")
    private String helloCouponBarcode;
    @Value("${date-coupon.barcode}")
    private String dateCouponBarcode;
    @Value("${ref-coupon.barcode}")
    private String refCouponBarcode;

    public String getPhotoFromUpdate(final Message msg, final GetFileFromBotCallback getFileFromBotCallback) {
        PhotoSize photo = null;
        if (msg.hasPhoto()) {
            photo = msg.getPhoto().stream()
                    .max(Comparator.comparingInt(PhotoSize::getWidth))
                    .orElse(null);
        } else if (msg.hasDocument()) {
            photo = msg.getDocument().getThumbnail();
        }

        LOGGER.info("Photo got from message: {}", photo);

        if (photo != null) {
            GetFile getFileRequest = new GetFile();
            getFileRequest.setFileId(photo.getFileId());
            try {
                File file = getFileFromBotCallback.execute(getFileRequest);
                String filePath = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
                LOGGER.info("Photo file path: {}", filePath);
                return filePath;
            } catch (TelegramApiException e) {
                throw new RuntimeException("Error in getting photo file path");
            }
        }

        return null;
    }

    public InputFile getPhotoInputFile(final String photoPath) {
        try {
            InputStream imageStream = new URL(photoPath).openStream();
            return new InputFile(imageStream, "image.jpg");
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException("Something wrong with image processing", e);
        }
    }

    public boolean messageHasPhoto(final Message message) {
        return message.hasPhoto() || message.hasDocument();
    }

    public SendMessage buildSendMessage(String text, long chatId) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build();
    }

    public SendPoll buildSendPoll(Poll poll, long chatId) {
        return SendPoll.builder()
                .chatId(chatId)
                .question(poll.getQuestion())
                .options(poll.getOptions().stream().map(PollOption::getText).collect(Collectors.toList()))
                .isAnonymous(com.ledikom.model.Poll.IS_ANONYMOUS)
                .type(poll.getType())
                .allowMultipleAnswers(poll.getAllowMultipleAnswers())
                .correctOptionId(poll.getCorrectOptionId())
                .explanation(poll.getExplanation())
                .build();
    }

    public void addMusicMenuButtonsToSendMessage(final SendMessage sm) {
        addButtonsToMessage(sm, 2,
                Arrays.stream(MusicMenuItem.values()).map(value -> value.buttonText).collect(Collectors.toList()),
                Arrays.stream(MusicMenuItem.values()).map(value -> value.callbackDataString).collect(Collectors.toList()));
    }

    public void addWorkOutMenuButtonsToSendMessage(final SendMessage sm) {
        addButtonsToMessage(sm, 2,
                Arrays.stream(WorkOutMenuItem.values()).map(value -> value.buttonText).collect(Collectors.toList()),
                Arrays.stream(WorkOutMenuItem.values()).map(value -> value.callbackDataString).collect(Collectors.toList()));
    }

    public void addGymnasticsMenuButtonsToSendMessage(final SendMessage sm) {
        addButtonsToMessage(sm, 2,
                Arrays.stream(GymnasticsMenuItem.values()).map(value -> value.buttonText).collect(Collectors.toList()),
                Arrays.stream(GymnasticsMenuItem.values()).map(value -> value.callbackDataString).collect(Collectors.toList()));
    }

    public void addSetSpecialDateButton(final SendMessage sm) {
        addButtonToMessage(sm, "Настроить особенную дату", BotCommand.DATE.label);
    }

    public void addMusicDurationButtonsToSendMessage(final SendMessage sm, String musicString) {
        addButtonsToMessage(sm, 2,
                List.of("5 мин \uD83D\uDD51", "10 мин \uD83D\uDD53", "15 мин \uD83D\uDD56", "20 мин \uD83D\uDD59"),
                List.of(musicString + "_5", musicString + "_10", musicString + "_15", musicString + "_20"));
    }

    public void addCitiesButtons(final SendMessage sm, final Set<City> cities) {
        addButtonsToMessage(sm, 2,
                cities.stream().map(city -> city.label + " " + city.logo).collect(Collectors.toList()),
                cities.stream().map(Enum::name).collect(Collectors.toList()));
    }

    public void addPharmaciesFilterCitiesButtons(final SendMessage sm, final Set<City> cities) {
        addButtonsToMessage(sm, 2,
                cities.stream().map(city -> city.label + " " + city.logo).collect(Collectors.toList()),
                cities.stream().map(city -> PharmacyService.PHARMACIES_BUTTON_CALLBACK_STRING + city.name()).collect(Collectors.toList()));
    }

    public void addConsultationMenuButtons(final SendMessage sm) {
        addButtonsToMessage(sm, 1,
                List.of("\uD83D\uDCC3 Инструкция", "❓ Задать вопрос"),
                List.of(BotCommand.CONSULTATION_WIKI.label, BotCommand.CONSULTATION_ASK.label));
    }

    public void addHealthBeingButtons(final SendMessage sm) {
        addButtonsToMessage(sm, 1,
                List.of("\uD83C\uDFB6 Музыка для сна", "\uD83C\uDFCB Утренняя зарядка", "\uD83D\uDE34 Вечерняя гимнастика"),
                List.of(BotCommand.MUSIC.label, BotCommand.WORK_OUT.label, BotCommand.GYMNASTICS.label));
    }

    public void addSettingsButtons(final SendMessage sm) {
        addButtonsToMessage(sm, 1,
                List.of("\uD83C\uDFE5 Ваш город", "\uD83C\uDF81 Подарок в особенный день", "\uD83D\uDDD2 Вкл/Откл рассылку новостей"),
                List.of(BotCommand.CITY.label, BotCommand.DATE.label, BotCommand.TRIGGER_NEWS.label));
    }

    public void addInfoButtons(final SendMessage sm) {
        addButtonsToMessage(sm, 1,
                List.of("\uD83C\uDFE5 Наши аптеки", "\uD83E\uDD16 Описание функций"),
                List.of(BotCommand.PHARMACIES.label, BotCommand.DESCRIPTION.label));
    }

    public void addHelloMessageButtons(final SendMessage sm, final Coupon coupon) {
        addButtonsToMessage(sm, 1,
                List.of("Активировать приветственный купон \uD83D\uDC4B"),
                List.of(CouponService.COUPON_PREVIEW_BUTTON_CALLBACK_STRING + coupon.getId()));
    }

    public void addAcceptCouponButton(final SendMessage sm, final Coupon coupon, final String buttonText) {
        addButtonToMessage(sm, buttonText, CouponService.COUPON_ACCEPT_BUTTON_CALLBACK_STRING + coupon.getId());
    }

    public void addRepeatConsultationButton(final SendMessage sm) {
        addButtonToMessage(sm, "❓ Задать вопрос по здоровью", BotCommand.CONSULTATION_ASK.label);
    }

    public void addPreviewCouponButton(final SendMessage sm, final Coupon coupon, final String buttonText) {
        addButtonToMessage(sm, buttonText, CouponService.COUPON_PREVIEW_BUTTON_CALLBACK_STRING + coupon.getId());
    }

    public void addPromotionAcceptButton(final SendMessage sm) {
        addButtonToMessage(sm, "⭐⭐⭐ Участвовать ⭐⭐⭐", "promotionAccept");
    }

    public InlineKeyboardMarkup createListOfCoupons(final User user, final Set<Coupon> coupons) {
        var markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Coupon coupon : coupons) {
            var button = new InlineKeyboardButton();

            String prefix = "";
            if (coupon.getBarcode().equals(helloCouponBarcode)) {
                prefix = "(" + ChronoUnit.DAYS.between(LocalDate.now(), user.getHelloCouponExpiryDate()) + " д.) ";
            } else if (coupon.getBarcode().equals(dateCouponBarcode)) {
                prefix = "(" + ChronoUnit.DAYS.between(LocalDate.now(), user.getDateCouponExpiryDate()) + " д.) ";
            } else if (coupon.getBarcode().equals(refCouponBarcode)) {
                prefix = "(" + ChronoUnit.DAYS.between(LocalDate.now(), user.getRefCouponExpiryDate()) + " д.) ";
            } else {
                prefix = "(" + ChronoUnit.DAYS.between(LocalDate.now(), coupon.getEndDate()) + " д.)";
            }

            button.setText(prefix + coupon.getName());
            button.setCallbackData(CouponService.COUPON_PREVIEW_BUTTON_CALLBACK_STRING + coupon.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);

        return markup;
    }

    public void addEditNoteButton(final SendMessage sm) {
        addButtonToMessage(sm, "Редактировать \uD83D\uDD8D", BotCommand.EDIT_NOTES.label);
    }

    private void addButtonsToMessage(final SendMessage sm, final int buttonsInRow, final List<String> buttonTextList, final List<String> callbackDataList) {
        var markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int index = 0; index < buttonTextList.size(); ) {
            var button = new InlineKeyboardButton();
            button.setText(buttonTextList.get(index));
            button.setCallbackData(callbackDataList.get(index));
            row.add(button);
            if (++index % buttonsInRow == 0 || index == buttonTextList.size()) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }

        markup.setKeyboard(keyboard);
        sm.setReplyMarkup(markup);
    }

    private void addButtonToMessage(final SendMessage sm, final String buttonText, final String callbackData) {
        var markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        var button = new InlineKeyboardButton();
        button.setText(buttonText);
        button.setCallbackData(callbackData);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        keyboard.add(row);
        markup.setKeyboard(keyboard);
        sm.setReplyMarkup(markup);
    }
}
