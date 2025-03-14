package com.ledikom.utils;

import com.ledikom.model.*;
import com.ledikom.service.CouponService;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BotResponses {

    public static String startMessage() {
        return """
                Вас приветствует чат-ассистент *"Фарм"*!

                Здесь у вас есть уникальная возможность получить максимум полезных и интересных функций, связанных с вашим здоровьем и комфортом. Давайте рассмотрим, что вы уже можете делать и какие потрясающие возможности скоро будут добавлены:

                """

                +
                botDescription()
                +

                """


                        А пока что, чтобы отпраздновать ваше присоединение к чату *"Фарм"*, активируйте *приветственный купон со скидкой 5%* на любые товары (действителен в течение 30 дней). Это ваш первый шаг к заботе о вашем здоровье с нами.


                        _Не забывайте, что весь функционал доступен через меню чата. Добро пожаловать в будущее вашего здоровья и комфорта с ассистентом "Фарм"!_ 🌟""";
    }

    public static String botDescription() {
        return """
                *Основные функции:*
                                
                1. 💰 *Купоны на скидки*: _Получайте эксклюзивные купоны (доступны только пользователям чата) для скидок на покупки в наших аптеках, участвующих в программе._
                                
                2. 👫 *Подарки за приглашение друзей*: _Приглашайте своих друзей воспользоваться нашим ассистентом с помощью ссылки (доступна в меню) и получайте бонусы за каждых 10 приглашенных._
                                
                3. 🎉 *Акции и новости*: _Будьте в курсе актуальных акций и получайте новости в области здоровья._
                                
                4. 🏥 *Информация о наших аптеках*: _Получите доступ к информации о местоположении, телефоне и рабочем графике наших аптек, участвующих в программе._

                5. 🩺 *Советник по здоровью*: _Наш ассистент готов помочь вам с вопросами о здоровье и профилактике заболеваний. Получите информацию и советы, не покидая чата._

                6. 🗒️ *Заметки для покупок*: _Создавайте список покупок и сохраняйте его прямо здесь. Удобно и быстро._

                7. 🎶 *Музыка для сна*: _Создайте идеальную атмосферу для сна с нашей музыкой._
                                
                8. 🏋️ *Утренняя зарядка*: _Начните день с энергии и активности, повторяя движения за тренером в четырёх стилях на выбор._
                                
                9. 😴 *Вечерняя расслабляющая гимнастика*: _Быстрый способ расслабиться и снять напряжение перед сном._


                *Скоро добавим:*

                1. ⏰ *Напоминания по приему лекарств*: _Настройте удобные напоминания о приеме назначенных вам лекарственных препаратов и других средств для вашего здоровья._

                2. 🎁 *Карта постоянного клиента*: _Участвуйте в программе лояльности и получайте призы за свои покупки._
                """;
    }

    public static String couponAcceptMessage(final Coupon coupon, final boolean inAllPharmacies, final int durationInMinutes) {
        StringBuilder sb = new StringBuilder(coupon.getText() + "\n\n");

        appendPharmacies(sb, coupon.getPharmacies().stream().toList(), inAllPharmacies);
        sb.append("\n\n");

        if (coupon.getStartDate() != null && coupon.getEndDate() != null) {
            sb.append("*С ").append(coupon.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append(" по ").append(coupon.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("*\n\n");
        }

        sb.append("*Купон действителен в течение ").append(durationInMinutes).append(" минут. Активируйте его при кассе.*");

        sb.append("\n\n");

        sb.append("_*Вы можете использовать только один купон на одну покупку_");

        return sb.toString();
    }

    public static String referralMessage(final String refLink, final int referralCount, final Coupon coupon) {
        final int toInviteLeft = referralCount < 10 ? 10 - referralCount : 10;

        return "Ваша ссылка:\n\n\n" + "[" + refLink + "](" + refLink + ")\n\n\n*Количество приглашенных вами пользователей:   "
                + referralCount
                + "*\n\n\nПоделитесь ссылкой еще с ❗*" + toInviteLeft + "*❗ друзьями (скопируйте и отправьте вашим контактам или перешлите сообщение ниже) и получайте бонусы!\n\n"
                + "\uD83E\uDD47 за каждые *10* приглашенных: \n*" + coupon.getName() + "\n" + coupon.getText() + "*";
    }

    public static String sendRefLinkReminder(final String refLink, final int referralCount, final Coupon coupon) {
        return "*Напоминаем вам, что у нас действует акция - Пригласи 10 друзей и получи купон на скидку 5 %*\uD83D\uDC9A" + "\n\n" + referralMessage(refLink, referralCount, coupon);
    }

    public static String couponExpiredMessage() {
        return "Время вашего купона истекло ⌛";
    }

    public static String triggerReceiveNewsMessage(final User user) {
        return "Подписка на рассылку новостей и акций " + (user.getReceiveNews() ? "включена \uD83D\uDD14" : "отключена \uD83D\uDD15");
    }

    public static String listOfCouponsMessage(final User user, final List<Coupon> coupons, final String helloCouponBarcode, final String dateCouponBarcode, final String refCouponBarcode) {
        final StringBuilder sb = new StringBuilder("\uD83D\uDCB8 Ваши купоны:\n\n\n");

        for (int index = 0; index < coupons.size(); index++) {
            final Coupon indexCoupon = coupons.get(index);

            sb.append(index + 1).append(". ")
                    .append(indexCoupon.getName()).append("\n")
                    .append(indexCoupon.getText()).append("\n");

            String deadlineStr = "";
            if (indexCoupon.getBarcode().equals(helloCouponBarcode)) {
                deadlineStr = "*Осталось ❗" + ChronoUnit.DAYS.between(LocalDate.now(), user.getHelloCouponExpiryDate()) + " дней❗ до оконачания срока действия купона*";
            } else if (indexCoupon.getBarcode().equals(dateCouponBarcode)) {
                deadlineStr = "*Осталось ❗" + ChronoUnit.DAYS.between(LocalDate.now(), user.getDateCouponExpiryDate()) + " дней❗ до оконачания срока действия купона*";
            } else if (indexCoupon.getBarcode().equals(refCouponBarcode)) {
                deadlineStr = "*Осталось ❗" + ChronoUnit.DAYS.between(LocalDate.now(), user.getRefCouponExpiryDate()) + " дней❗ до оконачания срока действия купона*";
            } else {
                deadlineStr = "*Осталось ❗" + ChronoUnit.DAYS.between(LocalDate.now(), indexCoupon.getEndDate()) + " дней❗ до оконачания срока действия купона*";
            }
            sb.append(deadlineStr).append("\n\n\n\n");
        }

        sb.append("_*Воспользуйтесь, выбрав в меню ниже_");

        return sb.toString();
    }

    public static String noActiveCouponsMessage() {
        return """
                У вас нет активных купонов \uD83D\uDC40

                Дождитесь новой рассылки акций в наших аптеках, а также приглашайте друзей, используя свою ссылку ⬇""";
    }

    public static String initialCouponText(final String couponTextWithBarcode, final long couponDurationInMinutes) {
        return "Времени осталось: *" + UtilityHelper.convertIntToTimeInt(couponDurationInMinutes) + ":00*" +
                "\n\n" +
                couponTextWithBarcode;
    }

    public static String updatedCouponText(final UserCouponRecord userCouponRecord, final long timeLeftInSeconds) {
        return "Времени осталось: *" + UtilityHelper.convertIntToTimeInt(timeLeftInSeconds / 60) + ":" + UtilityHelper.convertIntToTimeInt(timeLeftInSeconds % 60) +
                "*\n\n" +
                userCouponRecord.getText();
    }

    public static String noteAdded() {
        return "Заметка сохранена \uD83D\uDD16\n\n_*Чтобы просмотреть или редактировать, воспользуйтесь меню ассистента_";
    }

    public static String myNote(final String note) {
        return "Ваша заметка:\n\n" +
                "`" + note + "`";
    }

    public static String editNote() {
        return "_*Чтобы редактировать, скопируйте текст заметки выше (нажать на текст), вставьте в поле ввода, измените текст и отправьте сообщение._";
    }

    public static String addNote() {
        return "Чтобы добавить заметку, введите сообщение и отправьте ✏";
    }

    public static String musicMenu() {
        return "Выберите стиль музыки \uD83C\uDFBC";
    }

    public static String musicDurationMenu() {
        return """
                Выберите продолжительность

                _*Музыка остановится автоматически, телефон можно заблокировать и отложить_""";
    }

    public static String goodNight() {
        return "Сеть аптек \"Фарм\" желает вам добрых снов \uD83D\uDE0C";
    }

    public static String chooseYourCity() {
        return "Выберите ваш город";
    }

    public static String cityAdded(final String cityName) {
        return "Ваш город - *" + City.valueOf(cityName).label + "*";
    }

    public static String newCoupon(final Coupon coupon) {
        return coupon.getNews() + "\n\n" + coupon.getText();
    }

    public static String couponIsNotActive() {
        return "Купон неактивен!";
    }

    public static String yourCityCanUpdate(final City city) {
        return "Ваш город" + (city == null ?
                " не указан.\n\nУкажите его, чтобы получать актуальные новости и акции только для вашего города!"
                :
                " - " + city.label + ".\n\nМожете изменить, выбрав в меню ниже.");
    }

    public static String cityAddedAndNewCouponsGot(final String cityName) {
        return "Ваш город - *" + City.valueOf(cityName).label + "*\n\nПроверьте и воспользуйтесь вашими новыми купонами!";
    }

    public static String promotionAccepted() {
        return "Спасибо, что согласились поучаствовать в нашей акции!\n\n*Поспешите - количество ограниченно!*";
    }

    public static String promotionText(final PromotionFromAdmin promotionFromAdmin, final boolean inAllPharmacies) {
        StringBuilder sb = new StringBuilder(promotionFromAdmin.getText()).append("\n\n");
        appendPharmacies(sb, promotionFromAdmin.getPharmacies(), inAllPharmacies);
        sb.append("\n");
        return sb.toString();
    }

    private static void appendPharmacies(final StringBuilder sb, final List<Pharmacy> pharmacies, final boolean inAllPharmacies) {
        if (inAllPharmacies) {
            sb.append("*Действует во всех аптках, участвующих в программе.*");
        } else {
            sb.append("*Действует в аптках:*\n");
            pharmacies.forEach(pharmacy -> {
                sb.append(pharmacy.getName()).append(" - ").append(pharmacy.getCity().label).append(", ").append(pharmacy.getAddress()).append("\n");
            });
        }
    }

    public static String addSpecialDate() {
        return "\uD83D\uDCC6 Вы можете указать вашу особенную дату, в этот день вы получите купон на скидку в аптеках, участвующих в программе!\n\n*Введите и отправьте сообщение в следующем цифровом формате:\n\nдень.месяц* (пример - *07.10*)";
    }

    public static String yourSpecialDate(final LocalDateTime specialDate) {
        return "Ваша особенная дата: *" + specialDate.format(DateTimeFormatter.ofPattern("dd.MM")) + "*\n\n" + "\uD83C\uDF81 В этот день вас ждет подарок - купон на скидку в аптеках, участвующих в программе!";
    }

    public static String specialDay() {
        return """
                ✨ В этот особенный день мы хотим пожелать вам бесконечной удачи, крепкого здоровья и безмерного счастья!

                Воспользуйтесь вашим подарком \uD83C\uDF81

                *Купон на скидку 5% на любые товары в наших аптеках (/apteki), участвующих в программе!*
                                
                _*Действителен в течение 30 дней_
                """;
    }

    public static String refCoupon(final int referralCount) {
        return "\uD83E\uDEC2 *Вы пригласили уже " + referralCount + " новых пользователей!*\n\nСпасибо, что совершаете покупки у нас и приглашаете к нам ваших друзей!\n\n\uD83C\uDF81 Вы получаете подарок!\n\n"
                + "*Купон на скидку 5% на любые покупки в наших аптеках, участвующих в программе!*\n\n"
                + "_*Действителен в течение 30 дней_"
                + "\n\n"
                + "**_Продолжайте приглашать друзей и получать новые подарки!_";
    }

    public static String responseTimeExceeded() {
        return "Время ожидания на ответ вышло.\n\nВ случае необходимости повторите операцию через меню.";
    }

    public static String consultationWiki() {
        return """                             
                *Как правильно задавать вопросы*

                1. *Сформулируйте вопрос четко и развернуто:* _Постарайтесь выразить свой вопрос ясно и в полном объеме. Можете использовать несколько предложений. Это поможет ассистенту лучше понять ваш запрос и дать более точный ответ._

                2. *Соблюдайте ограничение в 300 символов:* _Ваш вопрос должен быть коротким и не превышать 300 символов._

                3. *Соблюдайте тему здоровья:* _Ассистент специализирован на вопросах по здоровью и профилактике заболеваний, поэтому задавайте вопросы, связанные с этой темой._

                *Примеры хороших вопросов:*
                -"Какие симптомы гриппа и каковы методы лечения?"
                -"Как принимать Кагоцел взрослому человеку?"
                -"Как улучшить сон и бороться с бессонницей?"
                -"Что такое антиоксиданты и почему они важны для здоровья?"
                -"Как поддерживать здоровое пищеварение и избегать желудочных проблем?"
                -"Какое продукты полезны при недостатке железа?"
                -"Как справиться с волнением перед выступлением?"

                Соблюдение этих правил поможет вам получить более точные и полезные ответы от ассистента.
                                
                _*Ассистент работает на основе сложных алгоритмов и искусственного интеллекта, и обработка вашего запроса может занять некоторое время. Обычно это занимает до 30 секунд. Ассистент постарается предоставить наилучший и информативный ответ на ваш вопрос._
                """;
    }

    public static String consultationShortWiki() {
        return """
                *Отправьте свой вопрос*
                                
                _*Постарайтесь выразить свой вопрос ясно и в полном объеме. Можете использовать несколько предложений. Это поможет ассистенту лучше понять ваш запрос и дать более точный ответ._
                """;
    }

    public static String waitForGptResponse() {
        return "Идёт обработка вашего запроса...\n" +
                "Обычно это занимает *до 30 секунд*";
    }

    public static String workOutMenu() {
        return """
                *Утренняя зарядка* - Выберите вид зарядки
                                
                \uD83E\uDDD8\u200D♂ *Йога*: _Проведите короткую йога-сессию, чтобы растянуть мышцы и подготовить тело к новому дню. Включите асаны (позы) для гибкости и медитацию для умиротворения ума._

                \uD83C\uDFC3\u200D♀ *Кардио-зарядка*: _Выполните серию кардиоупражнений, таких как бег на месте, подскоки или приседания. Это поможет увеличить уровень энергии и улучшить кровообращение._

                \uD83E\uDD38\u200D♀ *Стретчинг*: _Проведите утренний стретчинг, чтобы расслабить и размять мышцы. Это помогает улучшить гибкость и уменьшить риск напряжения._

                \uD83E\uDD3D\u200D♀ *Пилатес*: _Займитесь утренним пилатесом, чтобы укрепить мышцы корпуса и улучшить осанку. Это также способствует лучшему контролю над телом и снятию напряжения._
                                
                _*Ассистент отправит вам небольшое видео с утренней зарядкой. Пожалуйста, следуйте указаниям тренера и выполняйте упражнения для наилучшего эффекта. Желаем вам активного и здорового дня!_""";

    }

    public static String gymnasticsMenu() {
        return """
                *Вечерняя расслабляющая гимнастика* - Выберите вид гимнастики
                                
                \uD83E\uDDD8\u200D♀ *Медитация и дыхательная гимнастика*: _Практика медитации и глубокого дыхания поможет устранить стресс, успокоить ум и создать гармонию в вашем вечере._

                \uD83C\uDF19 *Сонная йога*: _Займитесь специальными асанами, которые способствуют расслаблению и улучшению качества сна. Это отличный способ снять напряжение после дня._

                \uD83D\uDECC *Постельная гимнастика*: _Расположитесь на матрасе и позвольте всему напряжению уйти. Простые упражнения расслабления и дыхания помогут вам снять стресс и подготовиться к спокойному сну._

                \uD83E\uDD38\u200D♀ *Вечерняя растяжка*: _Проведите короткую сессию растяжки, чтобы расслабить напряженные мышцы и готовить тело к спокойному сну. Это поможет улучшить гибкость и укрепить здоровье вашей спины._
                                
                _*Ассистент отправит вам небольшое видео с гимнастикой. Пожалуйста, следуйте указаниям тренера и выполняйте упражнения для наилучшего эффекта. Желаем вам спокойных снов!_""";

    }

    public static String video(final String videoLink) {
        return "[" + videoLink + "](" + videoLink + ")";
    }

    public static String chooseCityToFilterPharmacies() {
        return "Выберите город, чтобы просмотреть информация об аптеках:";
    }

    public static String pharmaciesInfo(final List<Pharmacy> pharmacies) {
        StringBuilder sb = new StringBuilder("Список аптек, участвующих в программе в вашем городе 🏥");
        sb.append("\n\n");

        pharmacies.forEach(pharmacy -> sb.append(pharmacy.getName()).append("\n")
                .append("Адрес: *").append(pharmacy.getAddress()).append("*\n")
                .append("График: *").append(pharmacy.getOpenHours()).append("*\n")
                .append("Телефон: ").append("`").append(pharmacy.getPhoneNumber()).append("`\n")
                .append("[Показать на карте](").append(pharmacy.getCoordinates()).append(")")
                .append("\n\n\n"));

        sb.append("\n\n").append("_\"Фарм\"_");


        return sb.toString();
    }

    public static String referralLinkToForward(final String refLink) {
        return """
                Привет! Приглашаю тебя присоединиться к *чат-ассистенту* сети аптек "Фарм", он предоставляет множество удобных функций *для заботы о здоровье и экономии на покупках*.
                                
                *Чтобы попробовать его и получить ❗ скидку 5% ❗, перейди по этой ссылке:*
                """
                +
                "[" + refLink + "](" + refLink + ")\n\n"
                +
                """      
                _Уверен, что и тебе пригодится! Пользуйся и экономь!_
                """;
    }

    public static String consultationMenu() {
        return """
                🔍 Эта функция позволяет просто *ввести свой вопрос*, и наш алгоритм, используя искусственный интеллект, предоставит вам подробный *ответ в течение 30 секунд*. Теперь получение информации стало еще проще и удобнее!
                """;
    }

    public static String setSpecialDate() {
        return """
                📆 Вы можете указать вашу особенную дату, в этот день вы получите *купон на скидку 5 % на все товары* в аптеках, участвующих в программе!
                """;
    }

    public static String specialDateSetReminder() {
        return "Напоминаем вам \uD83D\uDC9A" + "\n\n" + setSpecialDate();
    }

    public static String setCityReminder() {
        return "Напоминаем вам \uD83D\uDC9A" + "\n\n"
                + "Вы можете указать ваш город, чтобы получать *купоны, подарки, новости и акции* только для аптек, участвующих в программе в вашем городе!";
    }
}
