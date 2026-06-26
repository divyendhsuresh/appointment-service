package com.mykare.appointment_service.Messaging.Producer;

import com.mykare.appointment_service.Messaging.Event.AppointmentNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentNotificationProducer {

    private static final String TRANSACTION_ID_HEADER =
            "X-Transaction-Id";

    private static final String EVENT_TYPE_HEADER =
            "eventType";

    private final KafkaTemplate<String, AppointmentNotificationEvent>
            kafkaTemplate;

    @Value("${app.kafka.topics.appointment-notifications}")
    private String notificationTopic;

    public void send(AppointmentNotificationEvent event) {

        String messageKey = event.appointmentId().toString();

        Headers headers = new org.apache.kafka.common.header.internals.RecordHeaders();

        headers.add(
                new RecordHeader(
                        TRANSACTION_ID_HEADER,
                        event.transactionId().getBytes(StandardCharsets.UTF_8)
                )
        );

        headers.add(
                new RecordHeader(
                        EVENT_TYPE_HEADER,
                        event.eventType().getBytes(StandardCharsets.UTF_8)
                )
        );

        ProducerRecord<String, AppointmentNotificationEvent> record =
                new ProducerRecord<>(
                        notificationTopic,
                        null,
                        messageKey,
                        event,
                        headers
                );

        log.info(
                "Publishing Kafka event. topic={}, key={}, appointmentId={}, transactionId={}",
                notificationTopic,
                messageKey,
                event.appointmentId(),
                event.transactionId()
        );

        kafkaTemplate.send(record)
                .whenComplete((result, exception) -> {

                    if (exception != null) {
                        log.error(
                                "Kafka publishing failed. topic={}, key={}, appointmentId={}, transactionId={}",
                                notificationTopic,
                                messageKey,
                                event.appointmentId(),
                                event.transactionId(),
                                exception
                        );

                        return;
                    }

                    var metadata = result.getRecordMetadata();

                    log.info(
                            "Kafka event published successfully. topic={}, partition={}, offset={}, timestamp={}, appointmentId={}, transactionId={}",
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            metadata.timestamp(),
                            event.appointmentId(),
                            event.transactionId()
                    );
                });
    }
}