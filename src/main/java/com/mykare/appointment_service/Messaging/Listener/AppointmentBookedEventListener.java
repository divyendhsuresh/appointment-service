package com.mykare.appointment_service.Messaging.Listener;

import com.mykare.appointment_service.Messaging.Event.AppointmentBookedDomainEvent;
import com.mykare.appointment_service.Messaging.Producer.AppointmentNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentBookedEventListener {

    private final AppointmentNotificationProducer notificationProducer;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleAppointmentBooked(
            AppointmentBookedDomainEvent domainEvent
    ) {

        log.info(
                "Appointment transaction committed. Publishing notification event for appointment {}",
                domainEvent.notificationEvent().appointmentId()
        );

        notificationProducer.send(
                domainEvent.notificationEvent()
        );
    }
}