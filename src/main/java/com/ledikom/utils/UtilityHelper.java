package com.ledikom.utils;

import com.ledikom.model.MusicCallbackRequest;

public class UtilityHelper {

    public static String convertIntToTimeInt(long value) {
        return value < 10 ? "0" + value : "" + value;
    }

    public static MusicCallbackRequest getMusicCallbackRequest(String command) {
        String[] splitString = command.split("_");
        MusicCallbackRequest musicCallbackRequest = new MusicCallbackRequest(command);
        musicCallbackRequest.setStyleString(splitString[1]);
        if (splitString.length == 3) {
            musicCallbackRequest.setDuration(Integer.parseInt(splitString[2]));
        }
        return musicCallbackRequest;
    }

}
