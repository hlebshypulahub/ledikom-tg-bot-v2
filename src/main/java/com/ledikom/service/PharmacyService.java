package com.ledikom.service;

import com.ledikom.model.Pharmacy;
import com.ledikom.repository.PharmacyRepository;
import com.ledikom.utils.City;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PharmacyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PharmacyService.class);

    private final PharmacyRepository pharmacyRepository;
    private final BotUtilityService botUtilityService;

    public PharmacyService(final PharmacyRepository pharmacyRepository, final BotUtilityService botUtilityService) {
        this.pharmacyRepository = pharmacyRepository;
        this.botUtilityService = botUtilityService;
    }

    @PostConstruct
    public void initAndPersistPharmacies() {
        Pharmacy pharmacy = new Pharmacy(12, "Аптека №12", City.CHERVIEN, "г. Червень, ул. Чапаева, 23", "09:00-20:00, обед 14:00-14:30", "+375 1714 55470", "https://maps.app.goo.gl/WVXiHHU6r5y3LKQAA");
        Pharmacy pharmacy2 = new Pharmacy(18, "Аптека №18", City.MINSK, "г. Минск, ул. Красная, 13", "09:00-21:00, обед 14:00-14:30", "+375 17 2705428", "https://maps.app.goo.gl/YiasqjM3h4ESHaZr9");
        Pharmacy pharmacy3 = new Pharmacy(19, "Аптека №19", City.BORISOV, "г. Борисов, ул. М.Горького, 115", "09:00-20:00, обед 14:00-14:30", "+375 177 794003", "https://maps.app.goo.gl/DAFah6gg3V2bhKaZA");
        Pharmacy pharmacy4 = new Pharmacy(20, "Аптека №20", City.BORISOV, "г. Борисов, ул. Р.Люксембург, 65", "09:00-20:00, обед 14:00-14:30", "+375 177 741100", "https://maps.app.goo.gl/CpHGGvYoqqC2dSxj8");
        pharmacyRepository.saveAll(List.of(pharmacy, pharmacy2, pharmacy3, pharmacy4));
    }

    public void addCitiesButtons(final SendMessage sm) {
        botUtilityService.addCitiesButtons(sm, findAll().stream().map(Pharmacy::getCity).collect(Collectors.toSet()));
    }

    public Pharmacy findById(final long id) {
        return pharmacyRepository.findById(id).orElseThrow(() -> new RuntimeException("Pharmacy not found by id " + id));
    }

    public List<Pharmacy> findAll() {
        return pharmacyRepository.findAll();
    }

    // TODO: validation
    public List<Pharmacy> getPharmaciesFromIdsString(final String ids) {
        LOGGER.info("Getting pharmacies for ids string: {}", ids);

        List<Pharmacy> pharmacies = new ArrayList<>();
        if (ids.isBlank()) {
            pharmacies.addAll(findAll());
        } else {
            List<String> pharmacyIds = Arrays.stream(ids.split(",")).map(String::trim).toList();
            for (String id : pharmacyIds) {
                pharmacies.add(findById(Long.parseLong(id)));
            }
        }

        LOGGER.info("Pharmacies found:\n{}", pharmacies);

        return pharmacies;
    }

    public void addPharmaciesFilterCitiesButtons(final SendMessage sm) {
        botUtilityService.addPharmaciesFilterCitiesButtons(sm, findAll().stream().map(Pharmacy::getCity).collect(Collectors.toSet()));
    }
}
