package com.ledikom.utils;

import java.util.Arrays;
import java.util.Optional;

public enum AdminMessageToken {
    NEWS("news", 2),
    PROMOTION("promotion", 3),
    COUPON("coupon", 7);

    public final String label;
    public final int commandSize;

    AdminMessageToken(String label, final int commandSize) {
        this.label = label;
        this.commandSize = commandSize;
    }

    public static Optional<AdminMessageToken> getByLabel(final String label) {
        return Arrays.stream(AdminMessageToken.values()).filter(token -> token.label.equalsIgnoreCase(label)).findFirst();
    }
}
