package com.ledikom.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserCouponRecord {
    private long expiryTimestamp;
    private String text;
}
