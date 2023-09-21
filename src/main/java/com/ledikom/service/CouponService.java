package com.ledikom.service;

import com.ledikom.bot.LedikomBot;
import com.ledikom.callback.SendMessageCallback;
import com.ledikom.callback.SendMessageWithPhotoCallback;
import com.ledikom.model.*;
import com.ledikom.repository.CouponRepository;
import com.ledikom.utils.BotResponses;
import com.ledikom.utils.City;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CouponService {

    public static final Map<MessageIdInChat, UserCouponRecord> userCoupons = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(CouponService.class);

    @Value("${hello-coupon.barcode}")
    private String helloCouponBarcode;
    @Value("${date-coupon.barcode}")
    private String dateCouponBarcode;
    @Value("${ref-coupon.barcode}")
    private String refCouponBarcode;
    @Value("${coupon.duration-minutes}")
    private int couponDurationInMinutes;
    @Value("${coupon.expire-in-days}")
    private long couponExpireInDays;
    @Value("${admin.id}")
    private long adminId;

    private final CouponRepository couponRepository;
    private final UserService userService;
    private final PharmacyService pharmacyService;
    private final BotUtilityService botUtilityService;
    private final LedikomBot ledikomBot;
    private final RestTemplate restTemplate;

    private SendMessageCallback sendMessageCallback;
    private SendMessageWithPhotoCallback sendMessageWithPhotoCallback;

    public CouponService(final CouponRepository couponRepository, final UserService userService, final PharmacyService pharmacyService, final BotUtilityService botUtilityService, final LedikomBot ledikomBot, final RestTemplate restTemplate) {
        this.couponRepository = couponRepository;
        this.userService = userService;
        this.pharmacyService = pharmacyService;
        this.botUtilityService = botUtilityService;
        this.ledikomBot = ledikomBot;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void initCallbacks() {
        this.sendMessageCallback = ledikomBot.getSendMessageCallback();
        this.sendMessageWithPhotoCallback = ledikomBot.getSendMessageWithPhotoCallback();
    }

    private void saveStaleCoupons() {
        List<Coupon> couponsToSave = new ArrayList<>();
        if (couponRepository.findByBarcode(helloCouponBarcode).isEmpty()) {
            couponsToSave.add(getNewValidCoupon(List.of("coupon", helloCouponBarcode, "", "", "Приветственный купон -5% \uD83D\uDC4B",
                    "Здоровье – важнейшая ценность! С этим купоном вы получаете *5%* скидку на любой товар из нашего ассортимента!", "")));
        }
        if (couponRepository.findByBarcode(dateCouponBarcode).isEmpty()) {
            couponsToSave.add(getNewValidCoupon(List.of("coupon", dateCouponBarcode, "", "", "Купон к Особому Дню -5% \uD83D\uDCC6",
                    "Специальное предложение для особенного момента. Отмечайте с нами и экономьте! С этим купоном вы получаете *5%* скидку на любой товар из нашего ассортимента!", "")));
        }
        if (couponRepository.findByBarcode(refCouponBarcode).isEmpty()) {
            couponsToSave.add(getNewValidCoupon(List.of("coupon", refCouponBarcode, "", "", "Купон за приглашенных друзей -5% 👫",
                    "Ваши друзья - наши друзья! С этим купоном вы получаете *5%* скидку на любой товар из нашего ассортимента!", "")));
        }
        couponRepository.saveAll(couponsToSave);
    }

    public Coupon findCouponForUser(final User user, final String couponCommand) {
        int couponId = Integer.parseInt(couponCommand.split("_")[1]);
        LOGGER.info("Looking for a coupon: {}", couponId);
        return user.getCoupons().stream()
                .filter(coupon -> coupon.getId() == couponId)
                .filter(this::couponCanBeUsedNow)
                .findFirst()
                .orElseThrow(() -> {
                    sendMessageCallback.execute(botUtilityService.buildSendMessage("❌ Купон не найден / завершен / использован ❌", user.getChatId()));
                    return new RuntimeException("Coupon " + couponId + " not found / expired / used for user " + user.getChatId());
                });
    }

    public Coupon getHelloCoupon() {
        return couponRepository.findByBarcode(helloCouponBarcode).orElseThrow(() -> new RuntimeException("Coupon hello not found by barcode: " + helloCouponBarcode));
    }

    public Coupon getDateCoupon() {
        return couponRepository.findByBarcode(dateCouponBarcode).orElseThrow(() -> new RuntimeException("Coupon date not found by barcode"));
    }

    public Coupon getRefCoupon() {
        return couponRepository.findByBarcode(refCouponBarcode).orElseThrow(() -> new RuntimeException("Coupon ref not found by barcode"));
    }

    public List<Coupon> findAllTempActiveCouponsByCity(final City city) {
        return couponRepository.findAll().stream()
                .filter(this::couponIsTempAndActive)
                .filter(coupon -> coupon.getPharmacies().stream()
                        .anyMatch(pharmacy -> pharmacy.getCity() == city))
                .toList();
    }

    public void addHelloCouponToUser(final User user) {
        addCouponToUser(getHelloCoupon(), user);
    }

    public void addCouponToMap(final MessageIdInChat messageIdInChat, final String couponText) {
        long expiryTimestamp = System.currentTimeMillis() + couponDurationInMinutes * 60 * 1000L;
        UserCouponRecord userCouponRecord = new UserCouponRecord(expiryTimestamp, couponText);
        userCoupons.put(messageIdInChat, userCouponRecord);
    }

    public void createAndSendNewCoupon(final String photoPath, final List<String> splitStringsFromAdminMessage) throws IOException {
        LOGGER.info("Creating new coupon");

        Coupon coupon = getNewValidCoupon(splitStringsFromAdminMessage);
        couponRepository.save(coupon);

        if (couponIsTempAndActive(coupon)) {
            List<User> usersForCouponCities = userService.findAllUsersByPharmaciesCities(coupon.getPharmacies());
            usersForCouponCities.forEach(user -> addCouponToUser(coupon, user));

            sendCouponNewsToUsers(coupon, userService.filterUsersToSendNews(usersForCouponCities), photoPath);

            LOGGER.info("Coupon added and news sent");
            sendMessageCallback.execute(botUtilityService.buildSendMessage("Купон добавлен, рассылка произведена.", adminId));
        } else {
            LOGGER.info("Coupon added, news will be sent on the first day of validity");
            sendMessageCallback.execute(botUtilityService.buildSendMessage("Купон добавлен, рассылка будет произведена в первый день действия купона: "
                    + (coupon.getStartDate() != null ? coupon.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "нет даты"), adminId));
        }
    }

    private void sendCouponNewsToUsers(final Coupon coupon, final List<User> users, final String photoPath) throws IOException {
        if (photoPath != null) {
            InputStream imageStream = new URL(photoPath).openStream();
            InputFile inputFile = new InputFile(imageStream, "image.jpg");
            users.forEach(user -> sendMessageWithPhotoCallback.execute(inputFile, "", user.getChatId()));
        }
        sendCouponNewsToUsers(coupon, users);
    }

    private void sendCouponNewsToUsers(final Coupon coupon, final List<User> users) {
        users.forEach(user -> {
            var sm = botUtilityService.buildSendMessage(BotResponses.newCoupon(coupon), user.getChatId());
            botUtilityService.addPreviewCouponButton(sm, coupon, "Активировать купон ✅");
            sendMessageCallback.execute(sm);
        });
        LOGGER.info("Coupon sent to users: {}", coupon.getId());
    }

    public void addRefCouponToUser(final User user) {
        Optional<Coupon> optionalCoupon = Optional.empty();
        if (user.getReferralCount() % 10 == 0) {
            optionalCoupon = couponRepository.findByBarcode(refCouponBarcode);
        }

        if (optionalCoupon.isPresent()) {
            Coupon coupon = optionalCoupon.get();
            LOGGER.info("Adding referral coupon {} for user: {}", coupon.getName(), user.getId());
            addCouponToUser(coupon, user);
            var sm = botUtilityService.buildSendMessage(BotResponses.refCoupon(user.getReferralCount()), user.getChatId());
            botUtilityService.addPreviewCouponButton(sm, coupon, "Активировать купон ✅");
            sendMessageCallback.execute(sm);
        }
    }

    public void addDateCouponToUsers() {
        LOGGER.info("Adding date coupon to users");

        userService.findAllUsers().stream()
                .filter(user ->
                        user.getSpecialDate() != null
                                && user.getSpecialDate().getDayOfMonth() == getZonedDateTimeNow().getDayOfMonth()
                                && user.getSpecialDate().getMonth() == getZonedDateTimeNow().getMonth())
                .toList()
                .forEach(this::addDateCouponToUser);
    }

    public void addDateCouponToUser(final User user) {
        Coupon coupon = couponRepository.findByBarcode(dateCouponBarcode).orElseThrow(() -> new RuntimeException("Date coupon not exist by barcode " + dateCouponBarcode));

        LOGGER.info("Adding date coupon for user: {}", user.getId());
        addCouponToUser(coupon, user);
        var sm = botUtilityService.buildSendMessage(BotResponses.specialDay(), user.getChatId());
        botUtilityService.addPreviewCouponButton(sm, coupon, "Активировать купон ✅");
        sendMessageCallback.execute(sm);
    }

    public void addCouponsToUsersOnFirstActiveDay() {
        LOGGER.info("Adding coupons on first active day");

        List<User> users = userService.findAllUsers();

        List<Coupon> couponsWithFirstActiveDay = couponRepository.findAll().stream().filter(coupon -> !couponIsStale(coupon) && datesEqual(coupon.getStartDate(), getZonedDateTimeNow())).toList();

        LOGGER.info("Coupons found:\n{}", couponsWithFirstActiveDay);

        for (Coupon coupon : couponsWithFirstActiveDay) {
            List<User> usersToGetNewCoupon = new ArrayList<>();
            users.forEach(user -> {
                if (coupon.getPharmacies().stream().anyMatch(pharmacy -> user.getCity() == null || pharmacy.getCity() == user.getCity())) {
                    usersToGetNewCoupon.add(user);
                    addCouponToUser(coupon, user);
                }
            });
            sendCouponNewsToUsers(coupon, userService.filterUsersToSendNews(usersToGetNewCoupon));
        }
    }

    private boolean datesEqual(final LocalDateTime date1, final LocalDateTime date2) {
        return date1.getYear() == date2.getYear() &&
                date1.getMonth() == date2.getMonth() &&
                date1.getDayOfMonth() == date2.getDayOfMonth();
    }

    private void addCouponToUser(final Coupon coupon, final User user) {
        user.getCoupons().add(coupon);

        if (coupon.getBarcode().equals(helloCouponBarcode)) {
            user.setHelloCouponExpiryDate(LocalDate.now().plusDays(couponExpireInDays));
        } else if (coupon.getBarcode().equals(dateCouponBarcode)) {
            user.setDateCouponExpiryDate(LocalDate.now().plusDays(couponExpireInDays));
        } else if (coupon.getBarcode().equals(refCouponBarcode)) {
            user.setRefCouponExpiryDate(LocalDate.now().plusDays(couponExpireInDays));
        }

        userService.saveUser(user);
    }

    // TODO: add regex checks and split on methods
    private Coupon getNewValidCoupon(final List<String> splitStringsFromAdminMessage) {
        LOGGER.info("Generating new valid coupon");

        String barcode = splitStringsFromAdminMessage.get(1);

        if (couponRepository.findByBarcode(barcode).isPresent()) {
            throw new RuntimeException("Coupon already exists with barcode: " + barcode);
        }

        byte[] barcodeImageByteArray;
        String requestUrl = "https://barcodeapi.org/api/EAN13/" + barcode;
        try {
            barcodeImageByteArray = restTemplate.getForObject(requestUrl, byte[].class);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to GET barcode image, request URL: {}", requestUrl);
            String noBarcodeImagePath = "/no-barcode.png";
            try (InputStream inputStream = CouponService.class.getResourceAsStream(noBarcodeImagePath)) {
                if (inputStream == null) {
                    throw new IOException("Image file not found: " + noBarcodeImagePath);
                }
                barcodeImageByteArray = inputStream.readAllBytes();
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create input stream for barcode image");
            }
        }

        String datesArgument = splitStringsFromAdminMessage.get(2);
        LocalDateTime startDate, endDate;
        if (datesArgument.isBlank()) {
            if (barcode.equals(helloCouponBarcode)
                    || barcode.equals(dateCouponBarcode)
                    || barcode.equals(refCouponBarcode)) {
                startDate = null;
                endDate = null;
            } else {
                sendMessageCallback.execute(botUtilityService.buildSendMessage("Купон неактивен! Проверьте даты действия!", adminId));
                throw new RuntimeException("Coupon to be created is not active, dates string: " + datesArgument);
            }
        } else {
            String[] splitDates = datesArgument.split("-");
            if (splitDates.length == 2) {
                startDate = LocalDateTime.of(
                        2000 + Integer.parseInt(splitDates[0].substring(4)),
                        Integer.parseInt(splitDates[0].substring(2, 4)),
                        Integer.parseInt(splitDates[0].substring(0, 2)),
                        0, 0);
                endDate = LocalDateTime.of(
                        2000 + Integer.parseInt(splitDates[splitDates.length - 1].substring(4)),
                        Integer.parseInt(splitDates[splitDates.length - 1].substring(2, 4)),
                        Integer.parseInt(splitDates[splitDates.length - 1].substring(0, 2)),
                        23, 59);
            } else {
                sendMessageCallback.execute(botUtilityService.buildSendMessage("Купон неактивен! Проверьте даты действия!", adminId));
                throw new RuntimeException("Coupon to be created is not active, dates string: " + datesArgument);
            }
        }

        List<Pharmacy> pharmacies = pharmacyService.getPharmaciesFromIdsString(splitStringsFromAdminMessage.get(3));

        String name = splitStringsFromAdminMessage.get(4);
        String text = splitStringsFromAdminMessage.get(5);
        String news = splitStringsFromAdminMessage.get(6);

        Coupon coupon = new Coupon(barcode, barcodeImageByteArray, startDate, endDate, pharmacies, name, text, news);

        LOGGER.info("Coupon generated:\n{}", coupon);

        return coupon;
    }

    private boolean couponIsTempAndActive(final Coupon coupon) {
        if (coupon.getStartDate() != null && coupon.getEndDate() != null) {
            return getZonedDateTimeNow().isAfter(coupon.getStartDate()) && getZonedDateTimeNow().isBefore(coupon.getEndDate());
        }
        return false;
    }

    private boolean couponIsStale(final Coupon coupon) {
        return coupon.getStartDate() == null && coupon.getEndDate() == null;
    }

    public boolean couponCanBeUsedNow(final Coupon coupon) {
        return couponIsStale(coupon) || couponIsTempAndActive(coupon);
    }

    private LocalDateTime getZonedDateTimeNow() {
        return LocalDateTime.now(ZoneId.of("Europe/Moscow"));
    }

    public void deleteExpiredCoupons() {
        LOGGER.info("Deleting expired coupons");

        List<Coupon> coupons = couponRepository.findAll();
        List<Coupon> couponsToDelete = coupons.stream().filter(coupon -> coupon.getEndDate() != null && coupon.getEndDate().isBefore(getZonedDateTimeNow())).toList();

        LOGGER.info("Coupons to delete:\n{}", couponsToDelete);

        couponsToDelete.forEach(coupon -> coupon.getUsers().forEach(user -> {
            user.getCoupons().remove(coupon);
            userService.saveUser(user);
        }));
        couponRepository.deleteAll(couponsToDelete);
    }

    @Transactional
    public void clearUserCityCoupons(final User user) {
        LOGGER.info("Clearing user city coupons");

        List<Coupon> couponsToDelete = user.getCoupons().stream().filter(coupon -> !couponIsStale(coupon)).toList();
        couponsToDelete.forEach(coupon -> {
            user.getCoupons().remove(coupon);
            userService.saveUser(user);
        });
    }

    public String getTimeSign() {
        return getZonedDateTimeNow().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }
}
