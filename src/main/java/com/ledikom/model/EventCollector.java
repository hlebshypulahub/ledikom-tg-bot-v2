package com.ledikom.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;
import java.util.function.BinaryOperator;
import java.util.function.Function;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCollector {

    @Id
    private LocalDateTime timestamp;
    private int coupon;
    private int helloCoupon;
    private int dateCoupon;
    private int refLink;
    private int music;
    private int newUser;
    private int promotion;
    private int poll;
    private int date;
    private int note;
    private int city;
    private int newsDisabled;
    private int consultation;
    private int gymnastics;
    private int workout;

    public void reset() {
        timestamp = null;
        coupon = 0;
        helloCoupon = 0;
        dateCoupon = 0;
        refLink = 0;
        music = 0;
        newUser = 0;
        promotion = 0;
        poll = 0;
        date = 0;
        note = 0;
        city = 0;
        newsDisabled = 0;
        consultation = 0;
        gymnastics = 0;
        workout = 0;
    }

    public void incrementCoupon() {
        coupon = coupon + 1;
    }

    public void incrementHelloCoupon() {
        helloCoupon = helloCoupon + 1;
    }

    public void incrementDateCoupon() {
        dateCoupon = dateCoupon + 1;
    }

    public void incrementRefLink() {
        refLink = refLink + 1;
    }

    public void incrementMusic() {
        music = music + 1;
    }

    public void incrementNewUser() {
        newUser = newUser + 1;
    }

    public void incrementPromotion() {
        promotion = promotion + 1;
    }

    public void incrementPoll() {
        poll = poll + 1;
    }

    public void incrementDate() {
        date = date + 1;
    }

    public void incrementNote() {
        note = note + 1;
    }

    public void incrementCity() {
        city = city + 1;
    }

    public void incrementGymnastics() {
        gymnastics = gymnastics + 1;
    }

    public void incrementWorkOut() {
        workout = workout + 1;
    }

    public void incrementConsultation() {
        consultation = consultation + 1;
    }

    public void incrementNewsDisabled() {
        newsDisabled = newsDisabled + 1;
    }

    public void decrementNewsDisabled() {
        newsDisabled = newsDisabled - 1;
    }

    public String messageToAdmin(MessageFrequency messageFrequency) {
        return "Счетчик новых событий (" + messageFrequency.label + "):\n\n" +
                newUser + " - Новые пользователи\n" +
                refLink + " - Переход по реф. ссылке\n" +
                city + " - Настройка города\n" +
                date + " - Настройка особенной даты\n" +
                coupon + " - Активированные купоны\n" +
                helloCoupon + " - Приветственные купоны\n" +
                dateCoupon + " - Особенная Дата купоны\n" +
                consultation + " - Консультации\n" +
                music + " - Музыка для сна\n" +
                gymnastics + " - Вечерняя гимнастика\n" +
                workout + " - Утренняя зарядка\n" +
                promotion + " - Участие в промоакции\n" +
                poll + " - Участие в опросах\n" +
                note + " - Настройка заметок\n" +
                newsDisabled + " - Отключение рассылки\n";
    }

    public static EventCollector add(final EventCollector collector1, final EventCollector collector2) {
        return EventCollector.builder()
                .newUser(collector1.getNewUser() + collector2.getNewUser())
                .refLink(collector1.getRefLink() + collector2.getRefLink())
                .city(collector1.getCity() + collector2.getCity())
                .date(collector1.getDate() + collector2.getDate())
                .coupon(collector1.getCoupon() + collector2.getCoupon())
                .helloCoupon(collector1.getHelloCoupon() + collector2.getHelloCoupon())
                .dateCoupon(collector1.getDateCoupon() + collector2.getDateCoupon())
                .consultation(collector1.getConsultation() + collector2.getConsultation())
                .music(collector1.getMusic() + collector2.getMusic())
                .gymnastics(collector1.getGymnastics() + collector2.getGymnastics())
                .workout(collector1.getWorkout() + collector2.getWorkout())
                .promotion(collector1.getPromotion() + collector2.getPromotion())
                .poll(collector1.getPoll() + collector2.getPoll())
                .note(collector1.getNote() + collector2.getNote())
                .newsDisabled(collector1.getNewsDisabled() + collector2.getNewsDisabled())
                .build();
    }

    public enum MessageFrequency {
        DAILY("за последние сутки"),
        HOURLY("за последний час");

        public final String label;

        MessageFrequency(final String label) {
            this.label = label;
        }
    }
}
