package com.ledikom.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MessageIdInChat {
    private long chatId;
    private int messageId;
}
