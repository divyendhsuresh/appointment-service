package com.mykare.appointment_service.Messaging.Listener;

import com.mykare.appointment_service.Messaging.Event.AppointmentBookedDomainEvent;
import com.mykare.appointment_service.Messaging.Producer.AppointmentNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentBookedEventListener {

    private final AppointmentNotificationProducer notificationProducer;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleAppointmentBooked(
            AppointmentBookedDomainEvent domainEvent
    ) {

        var event = domainEvent.notificationEvent();

        log.info(
                "Database transaction committed. Publishing notification event. appointmentId={}, transactionId={}",
                event.appointmentId(),
                event.transactionId()
        );

        notificationProducer.send(event);
    }
}