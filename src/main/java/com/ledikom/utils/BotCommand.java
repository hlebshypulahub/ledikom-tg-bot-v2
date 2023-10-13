package com.ledikom.utils;

public enum BotCommand {
    START("/start"),
    COUPONS("/kupony"),
    NOTES("/zametki"),
    EDIT_NOTES("/note_edit"),
    MUSIC("/muzyka_dla_sna"),
    WORK_OUT("/zariadka_utrom"),
    GYMNASTICS("/gimnastika_vecherom"),
    REF_LINK("/moya_ssylka"),
    CITY("/moy_gorod"),
    PROMOTION_ACCEPT("promotionAccept"),
    DATE("/osobennaya_data"),
    CONSULTATION_MENU("/konsultaciya"),
    CONSULTATION_ASK("/consultation_ask"),
    CONSULTATION_WIKI("/consultation_wiki"),
    PHARMACIES("/apteki"),
    DESCRIPTION("/opisaniye_bota"),
    TRIGGER_NEWS("/vkl_otkl_rassylku"),
    HEALTH_BEING("/zdorovye"),
    SETTINGS("/nastroyki"),
    INFO("/informaciya"),
    ADMIN_EVENTS("events5463");

    public final String label;

    BotCommand(String label) {
        this.label = label;
    }
}
