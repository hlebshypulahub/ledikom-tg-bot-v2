package com.ledikom.repository;

import com.ledikom.model.Pharmacy;
import com.ledikom.utils.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
    Set<Pharmacy> findAllByCity(City city);
}
