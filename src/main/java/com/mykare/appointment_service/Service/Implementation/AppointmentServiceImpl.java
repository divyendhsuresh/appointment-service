package com.mykare.appointment_service.Service.Implementation;

import com.mykare.appointment_service.DTO.Request.CreateAppointmentRequest;
import com.mykare.appointment_service.DTO.Response.CreateAppointmentResponse;
import com.mykare.appointment_service.Entity.Appointment;
import com.mykare.appointment_service.Entity.AppointmentHistory;
import com.mykare.appointment_service.Entity.AppointmentSlot;
import com.mykare.appointment_service.Entity.User;
import com.mykare.appointment_service.Enums.AppointmentHistoryAction;
import com.mykare.appointment_service.Enums.AppointmentStatus;
import com.mykare.appointment_service.Enums.NotificationStatus;
import com.mykare.appointment_service.Enums.SlotStatus;
import com.mykare.appointment_service.Exception.SlotNotFoundException;
import com.mykare.appointment_service.Exception.SlotUnavailableException;
import com.mykare.appointment_service.Repository.AppointmentHistoryRepository;
import com.mykare.appointment_service.Repository.AppointmentRepository;
import com.mykare.appointment_service.Repository.AppointmentSlotRepository;
import com.mykare.appointment_service.Repository.UserRepository;
import com.mykare.appointment_service.Service.Interface.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl
        implements AppointmentService {

    private static final ZoneId CLINIC_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CreateAppointmentResponse createAppointment(
            String userEmail,
            CreateAppointmentRequest request
    ) {

        User user = userRepository
                .findByEmailIgnoreCase(userEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Authenticated user not found"
                        )
                );

        AppointmentSlot slot = slotRepository
                .findByIdForUpdate(request.slotId())
                .orElseThrow(() ->
                        new SlotNotFoundException(
                                "Appointment slot not found"
                        )
                );

        if (slot.getStartTime().isBefore(
                OffsetDateTime.now(CLINIC_ZONE)
        )) {
            throw new SlotUnavailableException(
                    "Cannot book a past appointment slot"
            );
        }

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new SlotUnavailableException(
                    "The selected slot is no longer available"
            );
        }

        boolean confirmedAppointmentExists =
                appointmentRepository.existsBySlotIdAndStatus(
                        slot.getId(),
                        AppointmentStatus.CONFIRMED
                );

        if (confirmedAppointmentExists) {
            throw new SlotUnavailableException(
                    "The selected slot is already booked"
            );
        }

        Appointment appointment = Appointment.builder()
                .user(user)
                .slot(slot)
                .reason(normalizeReason(request.reason()))
                .status(AppointmentStatus.CONFIRMED)
                .notificationStatus(NotificationStatus.PENDING)
                .build();

        appointment = appointmentRepository.save(appointment);

        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        AppointmentHistory history =
                AppointmentHistory.builder()
                        .appointment(appointment)
                        .action(AppointmentHistoryAction.CREATED)
                        .previousStatus(null)
                        .newStatus(AppointmentStatus.CONFIRMED)
                        .description("Appointment created successfully")
                        .changedBy(user.getId())
                        .build();

        historyRepository.save(history);

        return new CreateAppointmentResponse(
                appointment.getId(),
                slot.getId(),
                user.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                appointment.getReason(),
                appointment.getStatus(),
                appointment.getNotificationStatus(),
                appointment.getCreatedAt()
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }

        return reason.trim();
    }
}