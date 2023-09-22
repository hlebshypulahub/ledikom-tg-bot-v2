package com.ledikom.utils;

public enum WorkOutMenuItem {
    YOGA("Йога \uD83E\uDDD8\u200D♂", "workout_yoga", "https://www.youtube.com/watch?v=NCSKTE6BPvk&pp=ygUt0YPRgtGA0LXQvdC90Y_RjyDQudC-0LPQsCDRgSDRgtGA0LXQvdC10YDQvtC8"),
    CARDIO("Кардио-зарядка \uD83C\uDFC3\u200D♀", "workout_cardio", "https://www.youtube.com/watch?v=0WpPi3jmDzk&pp=ygVP0YPRgtGA0LXQvdC90Y_RjyDQutCw0YDQuNC00L4g0LfQsNGA0Y_QtNC60LAg0YEg0YLRgNC10L3QtdGA0L7QvCDQvdCwIDE1INC80LjQvQ%3D%3D"),
    PILATES("Пилатес \uD83E\uDD3D\u200D♀", "workout_pilates", "https://www.youtube.com/watch?v=cQC5Yw2fG2Q"),
    STRETCHING("Стретчинг \uD83E\uDD38\u200D♀", "workout_stretching", "https://www.youtube.com/watch?v=5s20-4dkKoQ&pp=ygU10YHRgtGA0LXRgtGH0LjQvdCzINGBINGC0YDQtdC90LXRgNC-0Lwg0L3QsCAxNSDQvNC40L0%3D");

    public static final String WORK_OUT_BUTTON_CALLBACK_STRING = "workout_";

    public final String buttonText;
    public final String callbackDataString;
    public final String videoLink;

    WorkOutMenuItem(String buttonText, String callbackDataString, final String videoLink) {
        this.buttonText = buttonText;
        this.callbackDataString = callbackDataString;
        this.videoLink = videoLink;
    }
}
