package com.ledikom.model;

import com.ledikom.utils.City;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Pharmacy {

    @Id
    private long id;
    private String name;
    private City city;
    private String address;
    private String openHours;
    private String phoneNumber;
    private String coordinates;

    @ManyToMany(mappedBy = "pharmacies", fetch = FetchType.EAGER)
    private Set<Coupon> coupons = new HashSet<>();

    public Pharmacy(final long id, final String name, final City city, final String address, final String openHours, final String phoneNumber, final String coordinates) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.address = address;
        this.openHours = openHours;
        this.phoneNumber = phoneNumber;
        this.coordinates = coordinates;
    }

    @Override
    public String toString() {
        return "Pharmacy{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", city=" + city +
                ", address='" + address + '\'' +
                ", openHours='" + openHours + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", coordinates='" + coordinates + '\'' +
                '}';
    }
}
