package com.mykare.appointment_service.Messaging.Producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mykare.appointment_service.Messaging.Event.AppointmentNotificationEvent;
import com.mykare.appointment_service.Service.Interface.NotificationStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentNotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationStatusService notificationStatusService;

    @Value("${app.kafka.topics.appointment-notifications}")
    private String notificationTopic;

    public void send(
            AppointmentNotificationEvent event
    ) {

        final String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {

            log.error(
                    "Failed to serialize notification event. Appointment ID: {}",
                    event.appointmentId(),
                    exception
            );

            notificationStatusService.markFailed(
                    event.appointmentId()
            );

            return;
        }

        kafkaTemplate
                .send(
                        notificationTopic,
                        event.appointmentId().toString(),
                        payload
                )
                .whenComplete((result, exception) -> {

                    if (exception != null) {

                        log.error(
                                "Failed to publish Kafka notification event. Appointment ID: {}",
                                event.appointmentId(),
                                exception
                        );

                        notificationStatusService.markFailed(
                                event.appointmentId()
                        );

                        return;
                    }

                    log.info(
                            "Kafka event published. Appointment ID: {}, topic: {}, partition: {}, offset: {}",
                            event.appointmentId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}