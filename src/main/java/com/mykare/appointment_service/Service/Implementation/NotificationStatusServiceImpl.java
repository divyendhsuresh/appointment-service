package com.mykare.appointment_service.Service.Implementation;

import com.mykare.appointment_service.DTO.Response.NotificationStatusResponse;
import com.mykare.appointment_service.Entity.Appointment;
import com.mykare.appointment_service.Enums.NotificationStatus;
import com.mykare.appointment_service.Exception.AppointmentNotFoundException;
import com.mykare.appointment_service.Repository.AppointmentRepository;
import com.mykare.appointment_service.Service.Interface.NotificationStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationStatusServiceImpl
        implements NotificationStatusService {

    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public NotificationStatusResponse updateStatus(
            UUID appointmentId,
            NotificationStatus status
    ) {

        Appointment appointment = appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() ->
                        new AppointmentNotFoundException(
                                "Appointment not found"
                        )
                );

        validateStatusTransition(
                appointment.getNotificationStatus(),
                status
        );

        appointment.setNotificationStatus(status);

        appointmentRepository.save(appointment);

        log.info(
                "Notification status updated. Appointment ID: {}, status: {}",
                appointmentId,
                status
        );

        return new NotificationStatusResponse(
                appointment.getId(),
                appointment.getNotificationStatus()
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID appointmentId) {

        appointmentRepository.findById(appointmentId)
                .ifPresent(appointment -> {
                    appointment.setNotificationStatus(
                            NotificationStatus.FAILED
                    );

                    appointmentRepository.save(appointment);

                    log.warn(
                            "Notification marked FAILED. Appointment ID: {}",
                            appointmentId
                    );
                });
    }

    private void validateStatusTransition(
            NotificationStatus currentStatus,
            NotificationStatus requestedStatus
    ) {

        if (currentStatus == NotificationStatus.SENT
                && requestedStatus != NotificationStatus.SENT) {

            throw new IllegalArgumentException(
                    "A sent notification status cannot be changed"
            );
        }

        if (currentStatus == NotificationStatus.FAILED
                && requestedStatus == NotificationStatus.PROCESSING) {

            /*
             * Allow this only if you implement retry processing.
             * For the initial implementation, reject it.
             */
            throw new IllegalArgumentException(
                    "A failed notification cannot be moved to processing"
            );
        }
    }
}