package com.mykare.appointment_service.Repository;

import com.mykare.appointment_service.Entity.Appointment;
import com.mykare.appointment_service.Enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    boolean existsBySlotIdAndStatus(UUID slotId, AppointmentStatus status);

}