package com.mykare.appointment_service.Service.Implementation;

import com.mykare.appointment_service.DTO.Response.AvailableSlotResponse;
import com.mykare.appointment_service.DTO.Response.AvailableSlotsResponse;
import com.mykare.appointment_service.Entity.AppointmentSlot;
import com.mykare.appointment_service.Enums.SlotStatus;
import com.mykare.appointment_service.Repository.AppointmentSlotRepository;
import com.mykare.appointment_service.Service.Interface.AppointmentSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentSlotServiceImpl
        implements AppointmentSlotService {

    private final AppointmentSlotRepository slotRepository;

    private static final ZoneId CLINIC_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final int START_HOUR = 9;

    private static final int END_HOUR = 18;

    @Override
    @Transactional
    public int generateSlotsTwoDaysAhead() {

        LocalDate targetDate =
                LocalDate.now(CLINIC_ZONE).plusDays(2);

        return generateSlotsForDate(targetDate);
    }

    @Override
    @Transactional
    public int generateSlotsForDate(LocalDate date) {

        LocalDate today = LocalDate.now(CLINIC_ZONE);

        if (date.isBefore(today)) {
            throw new IllegalArgumentException(
                    "Slots cannot be generated for a past date"
            );
        }

        /*
         * Clinic is closed on Sundays.
         * Remove this condition if Sunday is also a working day.
         */
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return 0;
        }

        List<AppointmentSlot> slotsToCreate =
                new ArrayList<>();

        /*
         * Generates:
         *
         * 09:00 - 10:00
         * 10:00 - 11:00
         * 11:00 - 12:00
         * ...
         * 17:00 - 18:00
         */
        for (int hour = START_HOUR; hour < END_HOUR; hour++) {

            OffsetDateTime startTime = date
                    .atTime(hour, 0)
                    .atZone(CLINIC_ZONE)
                    .toOffsetDateTime();

            OffsetDateTime endTime =
                    startTime.plusHours(1);

            boolean slotAlreadyExists =
                    slotRepository.existsByStartTimeAndEndTime(
                            startTime,
                            endTime
                    );

            if (!slotAlreadyExists) {

                AppointmentSlot slot =
                        AppointmentSlot.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(SlotStatus.AVAILABLE)
                                .build();

                slotsToCreate.add(slot);
            }
        }

        if (slotsToCreate.isEmpty()) {
            return 0;
        }

        List<AppointmentSlot> savedSlots =
                slotRepository.saveAll(slotsToCreate);

        return savedSlots.size();
    }

    @Override
    @Transactional(readOnly = true)
    public AvailableSlotsResponse fetchAvailableSlots(
            LocalDate date
    ) {

        LocalDate today = LocalDate.now(CLINIC_ZONE);

        if (date.isBefore(today)) {
            throw new IllegalArgumentException(
                    "Cannot fetch available slots for a past date"
            );
        }

        OffsetDateTime startOfDay = date
                .atStartOfDay(CLINIC_ZONE)
                .toOffsetDateTime();

        OffsetDateTime startOfNextDay = date
                .plusDays(1)
                .atStartOfDay(CLINIC_ZONE)
                .toOffsetDateTime();

        List<AppointmentSlot> availableSlots =
                slotRepository
                        .findByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                                SlotStatus.AVAILABLE,
                                startOfDay,
                                startOfNextDay
                        );

        List<AvailableSlotResponse> slotResponses =
                availableSlots.stream()
                        .map(slot ->
                                new AvailableSlotResponse(
                                        slot.getId(),
                                        slot.getStartTime(),
                                        slot.getEndTime(),
                                        slot.getStatus()
                                )
                        )
                        .toList();

        return new AvailableSlotsResponse(
                date,
                CLINIC_ZONE.getId(),
                slotResponses.size(),
                slotResponses
        );
    }
    @Override
    @Transactional
    public int generateTomorrowSlots() {

        LocalDate tomorrow =
                LocalDate.now(CLINIC_ZONE).plusDays(1);

        return generateSlotsForDate(tomorrow);
    }
}
