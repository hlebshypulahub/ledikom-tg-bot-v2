package com.ledikom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PromotionFromAdmin {
    private List<Pharmacy> pharmacies;
    private String text;
    private String photoPath;
}
