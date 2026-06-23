package com.mykare.appointment_service.Scheduler;

import com.mykare.appointment_service.Service.Interface.AppointmentSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentSlotScheduler {

    private final AppointmentSlotService slotService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Kolkata") public void generateUpcomingSlots() {
        
        int tomorrowSlots =
                slotService.generateTomorrowSlots();

        int twoDaysAheadSlots =
                slotService.generateSlotsTwoDaysAhead();

        log.info(
                "Scheduled slot generation completed. Tomorrow: {}, Two days ahead: {}",
                tomorrowSlots,
                twoDaysAheadSlots
        );
    }
}