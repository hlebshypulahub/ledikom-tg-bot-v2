package com.ledikom.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PollOption {

    private String text;
    private Integer voterCount;

}
