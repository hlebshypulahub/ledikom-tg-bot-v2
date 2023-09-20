package com.ledikom.utils;

public enum GymnasticsMenu {
    MEDITATION("Дыхательная гимнастика \uD83E\uDDD8\u200D♀", "gymnastics_meditation", "https://www.youtube.com/watch?v=qK1qhKVqj7o&pp=ygVV0JzQtdC00LjRgtCw0YbQuNGPINC4INC00YvRhdCw0YLQtdC70YzQvdCw0Y8g0LPQuNC80L3QsNGB0YLQuNC60LAg0YEg0YLRgNC10L3QtdGA0L7QvA%3D%3D"),
    YOGA("Сонная йога \uD83C\uDF19", "gymnastics_yoga", "https://www.youtube.com/watch?v=E1JT1CKEOuA&pp=ygUp0KHQvtC90L3QsNGPINC50L7Qs9CwINGBINGC0YDQtdC90LXRgNC-0Lw%3D"),
    BED("Постельная гимнастика \uD83D\uDECC", "gymnastics_bed", "https://www.youtube.com/watch?v=tbNfk2VejQA"),
    STRETCHING("Стретчинг \uD83E\uDD38\u200D♀", "gymnastics_stretching", "https://www.youtube.com/watch?v=imiItHDlMdQ");

    public final String buttonText;
    public final String callbackDataString;
    public final String videoLink;

    GymnasticsMenu(String buttonText, String callbackDataString, final String videoLink) {
        this.buttonText = buttonText;
        this.callbackDataString = callbackDataString;
        this.videoLink = videoLink;
    }
}
