package com.mykare.appointment_service.Repository;

import com.mykare.appointment_service.Entity.AppointmentSlot;
import com.mykare.appointment_service.Enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentSlotRepository
        extends JpaRepository<AppointmentSlot, UUID> {

    boolean existsByStartTimeAndEndTime(
            OffsetDateTime startTime,
            OffsetDateTime endTime
    );

    List<AppointmentSlot>
    findByStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            SlotStatus status,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    );
}
