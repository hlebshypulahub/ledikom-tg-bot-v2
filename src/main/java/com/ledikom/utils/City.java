package com.ledikom.utils;

public enum City {
    BORISOV("Борисов", "\uD83C\uDFD9"),
    MINSK("Минск", "\uD83C\uDF03"),
    CHERVIEN("Червень", "\uD83C\uDF09");

    public final String label;
    public final String logo;

    City(String label, final String logo) {
        this.label = label;
        this.logo = logo;
    }
}
