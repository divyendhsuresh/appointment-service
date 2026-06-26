package com.mykare.appointment_service.Service.Implementation;

import com.mykare.appointment_service.Common.Constants.HeaderConstants;
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
import com.mykare.appointment_service.Exception.AppointmentAccessDeniedException;
import com.mykare.appointment_service.Exception.AppointmentCancellationException;
import com.mykare.appointment_service.Exception.AppointmentNotFoundException;
import com.mykare.appointment_service.Exception.SlotNotFoundException;
import com.mykare.appointment_service.Exception.SlotUnavailableException;
import com.mykare.appointment_service.Messaging.Event.AppointmentBookedDomainEvent;
import com.mykare.appointment_service.Messaging.Event.AppointmentNotificationEvent;
import com.mykare.appointment_service.Repository.AppointmentHistoryRepository;
import com.mykare.appointment_service.Repository.AppointmentRepository;
import com.mykare.appointment_service.Repository.AppointmentSlotRepository;
import com.mykare.appointment_service.Repository.UserRepository;
import com.mykare.appointment_service.Service.Interface.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

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

        String transactionId = getTransactionId();

        log.info("Appointment booking started. slotId={}, transactionId={}", request.slotId(), transactionId);

        User user = userRepository
                .findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> {

                    log.warn("Appointment booking failed because authenticated user was not found. transactionId={}", transactionId);

                    return new UsernameNotFoundException(
                            "Authenticated user not found"
                    );
                });

        log.debug(
                "Authenticated user resolved for appointment booking. userId={}, slotId={}, transactionId={}",
                user.getId(),
                request.slotId(),
                transactionId
        );

        AppointmentSlot slot = slotRepository
                .findByIdForUpdate(request.slotId())
                .orElseThrow(() -> {

                    log.warn(
                            "Appointment slot not found. slotId={}, userId={}, transactionId={}",
                            request.slotId(),
                            user.getId(),
                            transactionId
                    );

                    return new SlotNotFoundException(
                            "Appointment slot not found"
                    );
                });

        log.debug(
                "Appointment slot locked for booking. slotId={}, status={}, startTime={}, transactionId={}",
                slot.getId(),
                slot.getStatus(),
                slot.getStartTime(),
                transactionId
        );

        OffsetDateTime currentClinicTime =
                OffsetDateTime.now(CLINIC_ZONE);

        if (slot.getStartTime().isBefore(currentClinicTime)) {

            log.warn(
                    "Past appointment slot booking prevented. slotId={}, startTime={}, currentTime={}, userId={}, transactionId={}",
                    slot.getId(),
                    slot.getStartTime(),
                    currentClinicTime,
                    user.getId(),
                    transactionId
            );

            throw new SlotUnavailableException(
                    "Cannot book a past appointment slot"
            );
        }

        if (slot.getStatus() != SlotStatus.AVAILABLE) {

            log.warn(
                    "Unavailable appointment slot booking prevented. slotId={}, slotStatus={}, userId={}, transactionId={}",
                    slot.getId(),
                    slot.getStatus(),
                    user.getId(),
                    transactionId
            );

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

            log.warn(
                    "Duplicate confirmed appointment prevented. slotId={}, userId={}, transactionId={}",
                    slot.getId(),
                    user.getId(),
                    transactionId
            );

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

        log.info(
                "Appointment saved successfully. appointmentId={}, slotId={}, userId={}, status={}, transactionId={}",
                appointment.getId(),
                slot.getId(),
                user.getId(),
                appointment.getStatus(),
                transactionId
        );

        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        log.info(
                "Appointment slot status updated. slotId={}, status={}, appointmentId={}, transactionId={}",
                slot.getId(),
                slot.getStatus(),
                appointment.getId(),
                transactionId
        );

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

        log.debug(
                "Appointment history created. appointmentId={}, action={}, changedBy={}, transactionId={}",
                appointment.getId(),
                AppointmentHistoryAction.CREATED,
                user.getId(),
                transactionId
        );

        AppointmentNotificationEvent notificationEvent =
                new AppointmentNotificationEvent(
                        UUID.randomUUID(),
                        transactionId,
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

        log.info(
                "Appointment booked domain event published. eventId={}, appointmentId={}, transactionId={}",
                notificationEvent.eventId(),
                appointment.getId(),
                transactionId
        );

        log.info(
                "Appointment booking completed successfully. appointmentId={}, slotId={}, userId={}, transactionId={}",
                appointment.getId(),
                slot.getId(),
                user.getId(),
                transactionId
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
    public UserAppointmentsResponse fetchUserAppointments(
            String userEmail
    ) {

        String transactionId = getTransactionId();

        log.info(
                "Fetching user appointments. transactionId={}",
                transactionId
        );

        User user = userRepository
                .findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> {

                    log.warn(
                            "Unable to fetch appointments because authenticated user was not found. transactionId={}",
                            transactionId
                    );

                    return new UsernameNotFoundException(
                            "Authenticated user not found"
                    );
                });

        List<Appointment> appointments =
                appointmentRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                user.getId()
                        );

        log.info(
                "User appointments fetched. userId={}, appointmentCount={}, transactionId={}",
                user.getId(),
                appointments.size(),
                transactionId
        );

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

        return new UserAppointmentsResponse(
                appointmentResponses.size(),
                appointmentResponses
        );
    }

    @Override
    @Transactional
    public CancelAppointmentResponse cancelAppointment(
            String userEmail,
            UUID appointmentId
    ) {

        String transactionId = getTransactionId();

        log.info(
                "Appointment cancellation started. appointmentId={}, transactionId={}",
                appointmentId,
                transactionId
        );

        User user = userRepository
                .findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> {

                    log.warn(
                            "Appointment cancellation failed because authenticated user was not found. appointmentId={}, transactionId={}",
                            appointmentId,
                            transactionId
                    );

                    return new UsernameNotFoundException(
                            "Authenticated user not found"
                    );
                });

        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() -> {

                    log.warn(
                            "Appointment not found during cancellation. appointmentId={}, userId={}, transactionId={}",
                            appointmentId,
                            user.getId(),
                            transactionId
                    );

                    return new AppointmentNotFoundException(
                            "Appointment not found"
                    );
                });

        log.debug(
                "Appointment locked for cancellation. appointmentId={}, status={}, userId={}, transactionId={}",
                appointment.getId(),
                appointment.getStatus(),
                user.getId(),
                transactionId
        );

        if (!appointment.getUser().getId().equals(user.getId())) {

            log.warn(
                    "Unauthorized appointment cancellation attempt. appointmentId={}, requestedByUserId={}, ownerUserId={}, transactionId={}",
                    appointmentId,
                    user.getId(),
                    appointment.getUser().getId(),
                    transactionId
            );

            throw new AppointmentAccessDeniedException(
                    "You are not allowed to cancel this appointment"
            );
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {

            log.warn(
                    "Appointment is already cancelled. appointmentId={}, userId={}, transactionId={}",
                    appointmentId,
                    user.getId(),
                    transactionId
            );

            throw new AppointmentCancellationException(
                    "Appointment is already cancelled"
            );
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {

            log.warn(
                    "Appointment cannot be cancelled because of current status. appointmentId={}, status={}, userId={}, transactionId={}",
                    appointmentId,
                    appointment.getStatus(),
                    user.getId(),
                    transactionId
            );

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

        log.info(
                "Appointment cancelled and slot released. appointmentId={}, slotId={}, previousStatus={}, newStatus={}, transactionId={}",
                appointment.getId(),
                slot.getId(),
                previousStatus,
                appointment.getStatus(),
                transactionId
        );

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

        log.debug(
                "Appointment cancellation history created. appointmentId={}, userId={}, transactionId={}",
                appointment.getId(),
                user.getId(),
                transactionId
        );

        log.info(
                "Appointment cancellation completed successfully. appointmentId={}, slotId={}, userId={}, transactionId={}",
                appointment.getId(),
                slot.getId(),
                user.getId(),
                transactionId
        );

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

    private String getTransactionId() {

        String transactionId =
                MDC.get(HeaderConstants.MDC_TRANSACTION_ID);

        return transactionId != null
                ? transactionId
                : "N/A";
    }
}