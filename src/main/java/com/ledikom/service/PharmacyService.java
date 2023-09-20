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
