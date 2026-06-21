package com.mykare.appointment_service.Config;

import com.mykare.appointment_service.Service.Interface.AppointmentSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlotStartupInitializer implements CommandLineRunner {

    private final AppointmentSlotService slotService;

    @Override
    public void run(String... args) {

        int createdSlots = slotService.generateSlotsTwoDaysAhead();
        int tomorrowSlots = slotService.generateTomorrowSlots();

        log.info("Startup slot generation completed. Created {} slots", createdSlots);
    }
}