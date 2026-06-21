package com.mykare.appointment_service.Repository;

import com.mykare.appointment_service.Entity.Appointment;
import com.mykare.appointment_service.Enums.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    boolean existsBySlotIdAndStatus(UUID slotId, AppointmentStatus status);
    List<Appointment> findByUserIdOrderByCreatedAtDesc(UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT appointment
            FROM Appointment appointment
            JOIN FETCH appointment.user
            JOIN FETCH appointment.slot
            WHERE appointment.id = :appointmentId
            """)
    Optional<Appointment> findByIdForUpdate(@Param("appointmentId") UUID appointmentId);
}