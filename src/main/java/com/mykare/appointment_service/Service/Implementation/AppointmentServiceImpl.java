package com.mykare.appointment_service.Service.Implementation;

import com.mykare.appointment_service.DTO.Request.CreateAppointmentRequest;
import com.mykare.appointment_service.DTO.Response.CancelAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.CreateAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.UserAppointmentResponse;
import com.mykare.appointment_service.DTO.Response.UserAppointmentsResponse;
import com.mykare.appointment_service.Entity.Appointment;
import com.mykare.appointment_service.Entity.AppointmentHistory;
import com.mykare.appointment_service.Entity.AppointmentSlot;
import com.mykare.appointment_service.Entity.User;
import com.mykare.appointment_service.Enums.AppointmentHistoryAction;
import com.mykare.appointment_service.Enums.AppointmentStatus;
import com.mykare.appointment_service.Enums.NotificationStatus;
import com.mykare.appointment_service.Enums.SlotStatus;
import com.mykare.appointment_service.Exception.*;
import com.mykare.appointment_service.Messaging.Event.AppointmentBookedDomainEvent;
import com.mykare.appointment_service.Messaging.Event.AppointmentNotificationEvent;
import com.mykare.appointment_service.Repository.AppointmentHistoryRepository;
import com.mykare.appointment_service.Repository.AppointmentRepository;
import com.mykare.appointment_service.Repository.AppointmentSlotRepository;
import com.mykare.appointment_service.Repository.UserRepository;
import com.mykare.appointment_service.Service.Interface.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CreateAppointmentResponse createAppointment(String userEmail, CreateAppointmentRequest request) {

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
        AppointmentNotificationEvent notificationEvent =
                new AppointmentNotificationEvent(
                        UUID.randomUUID(),
                        appointment.getId(),
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getPhone(),
                        slot.getId(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        appointment.getReason(),
                        "APPOINTMENT_BOOKED",
                        OffsetDateTime.now(ZoneOffset.UTC)
                );

        eventPublisher.publishEvent(
                new AppointmentBookedDomainEvent(
                        notificationEvent
                )
        );

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
    @Override
    @Transactional(readOnly = true)
    public UserAppointmentsResponse fetchUserAppointments(String userEmail) {

        User user = userRepository
                .findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));

        List<Appointment> appointments = appointmentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<UserAppointmentResponse> appointmentResponses =
                appointments.stream()
                        .map(appointment ->
                                new UserAppointmentResponse(
                                        appointment.getId(),
                                        appointment.getSlot().getId(),
                                        appointment.getSlot().getStartTime(),
                                        appointment.getSlot().getEndTime(),
                                        appointment.getReason(),
                                        appointment.getStatus(),
                                        appointment.getNotificationStatus(),
                                        appointment.getCancelledAt(),
                                        appointment.getCreatedAt()
                                )
                        )
                        .toList();

        return new UserAppointmentsResponse(appointmentResponses.size(), appointmentResponses);
    }

    @Override
    @Transactional
    public CancelAppointmentResponse cancelAppointment(String userEmail, UUID appointmentId) {

        User user = userRepository
                .findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));

        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() ->
                        new AppointmentNotFoundException(
                                "Appointment not found")
                );

        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new AppointmentAccessDeniedException(
                    "You are not allowed to cancel this appointment"
            );
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentCancellationException(
                    "Appointment is already cancelled"
            );
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppointmentCancellationException(
                    "Only confirmed appointments can be cancelled"
            );
        }

        AppointmentSlot slot = appointment.getSlot();

        AppointmentStatus previousStatus =
                appointment.getStatus();

        OffsetDateTime cancelledAt =
                OffsetDateTime.now(ZoneOffset.UTC);

        appointment.setStatus(
                AppointmentStatus.CANCELLED
        );

        appointment.setCancelledAt(cancelledAt);

        slot.setStatus(SlotStatus.AVAILABLE);

        appointmentRepository.save(appointment);
        slotRepository.save(slot);

        AppointmentHistory history =
                AppointmentHistory.builder()
                        .appointment(appointment)
                        .action(
                                AppointmentHistoryAction.CANCELLED
                        )
                        .previousStatus(previousStatus)
                        .newStatus(
                                AppointmentStatus.CANCELLED
                        )
                        .description(
                                "Appointment cancelled by user"
                        )
                        .changedBy(user.getId())
                        .build();

        historyRepository.save(history);

        return new CancelAppointmentResponse(
                appointment.getId(),
                slot.getId(),
                appointment.getStatus(),
                slot.getStatus(),
                appointment.getCancelledAt()
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }

        return reason.trim();
    }
}