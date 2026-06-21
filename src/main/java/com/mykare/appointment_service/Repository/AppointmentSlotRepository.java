package com.mykare.appointment_service.Repository;

import com.mykare.appointment_service.Entity.AppointmentSlot;
import com.mykare.appointment_service.Enums.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT slot
            FROM AppointmentSlot slot
            WHERE slot.id = :slotId
            """)
    Optional<AppointmentSlot> findByIdForUpdate(@Param("slotId") UUID slotId);
}
