package com.ledikom.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String barcode;
    private byte[] barcodeImageByteArray;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String name;
    private String text;
    private String news;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Pharmacy> pharmacies = new HashSet<>();

    @ManyToMany(mappedBy = "coupons", fetch = FetchType.EAGER)
    private Set<User> users = new HashSet<>();

    public Coupon(final String barcode, final byte[] barcodeImageByteArray, final LocalDateTime startDate, final LocalDateTime endDate, final List<Pharmacy> pharmacies, final String name, final String text, final String news) {
        this.barcode = barcode;
        this.barcodeImageByteArray = barcodeImageByteArray;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pharmacies = new HashSet<>(pharmacies);
        this.name = name;
        this.text = text;
        this.news = news;
    }

    @Override
    public String toString() {
        return "Coupon{" +
                "id=" + id +
                ", barcode='" + barcode + '\'' +
                ", barcodeImageByteArray=" + Arrays.toString(barcodeImageByteArray) +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", name='" + name + '\'' +
                ", text='" + text + '\'' +
                ", news='" + news + '\'' +
                '}';
    }
}
