package com.mykare.appointment_service.Repository;

import com.mykare.appointment_service.Entity.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, UUID> {
}
