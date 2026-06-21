import json
import logging
import os
import signal
import sys
import time
import uuid
from typing import Any

import requests
from confluent_kafka import (
    Consumer,
    KafkaError,
    KafkaException,
    Message,
)


logging.basicConfig(
    level=logging.INFO,
    format=(
        "%(asctime)s "
        "%(levelname)s "
        "[transactionId=%(transaction_id)s] "
        "%(message)s"
    ),
)


class TransactionIdFilter(logging.Filter):

    def filter(self, record: logging.LogRecord) -> bool:
        if not hasattr(record, "transaction_id"):
            record.transaction_id = "N/A"

        return True


LOGGER = logging.getLogger("notification-worker")
LOGGER.addFilter(TransactionIdFilter())


KAFKA_BOOTSTRAP_SERVERS = os.getenv(
    "KAFKA_BOOTSTRAP_SERVERS",
    "localhost:9092",
)

KAFKA_TOPIC = os.getenv(
    "KAFKA_TOPIC",
    "appointment-notifications",
)

KAFKA_GROUP_ID = os.getenv(
    "KAFKA_GROUP_ID",
    "appointment-notification-workers",
)

APPOINTMENT_SERVICE_URL = os.getenv(
    "APPOINTMENT_SERVICE_URL",
    "http://localhost:8080",
)

INTERNAL_API_KEY = os.getenv(
    "INTERNAL_API_KEY",
    "mykare-internal-secret",
)

running = True


def log_extra(transaction_id: str) -> dict[str, str]:
    """
    Adds the transaction ID to every related log entry.
    """
    return {
        "transaction_id": transaction_id,
    }


def handle_shutdown(
    signum: int,
    frame: Any,
) -> None:

    global running

    LOGGER.info(
        "Shutdown signal received: %s",
        signum,
        extra=log_extra("N/A"),
    )

    running = False


signal.signal(signal.SIGINT, handle_shutdown)
signal.signal(signal.SIGTERM, handle_shutdown)


def update_notification_status(
    appointment_id: str,
    status: str,
    transaction_id: str,
) -> None:

    url = (
        f"{APPOINTMENT_SERVICE_URL}"
        f"/api/v1/internal/appointments/"
        f"{appointment_id}/notification-status"
    )

    response = requests.patch(
        url,
        headers={
            "Content-Type": "application/json",
            "X-Internal-Api-Key": INTERNAL_API_KEY,
            "X-Transaction-Id": transaction_id,
        },
        json={
            "status": status,
        },
        timeout=10,
    )

    response.raise_for_status()

    LOGGER.info(
        "Notification status updated: "
        "appointment=%s status=%s",
        appointment_id,
        status,
        extra=log_extra(transaction_id),
    )


def send_notification(
    event: dict[str, Any],
    transaction_id: str,
) -> None:

    """
    Simulates sending an email or SMS notification.

    Replace this implementation later with an actual
    email, SMS, or third-party notification provider.
    """

    LOGGER.info(
        "Sending appointment notification to %s <%s>",
        event.get("fullName"),
        event.get("email"),
        extra=log_extra(transaction_id),
    )

    LOGGER.info(
        "Appointment time: %s to %s",
        event.get("startTime"),
        event.get("endTime"),
        extra=log_extra(transaction_id),
    )

    # Simulate notification provider processing.
    time.sleep(1)

    LOGGER.info(
        "Notification successfully sent for appointment %s",
        event.get("appointmentId"),
        extra=log_extra(transaction_id),
    )


def process_message(message: Message) -> None:

    raw_value = message.value()

    if raw_value is None:
        raise ValueError(
            "Kafka message does not contain a value"
        )

    try:
        event = json.loads(
            raw_value.decode("utf-8")
        )

    except json.JSONDecodeError as exception:
        raise ValueError(
            "Kafka message contains invalid JSON"
        ) from exception

    appointment_id = event.get("appointmentId")

    if not appointment_id:
        raise ValueError(
            "Kafka event does not contain appointmentId"
        )

    transaction_id = event.get("transactionId")

    if not transaction_id:
        transaction_id = str(uuid.uuid4())

        LOGGER.warning(
            "Kafka event does not contain transactionId. "
            "Generated fallback transaction ID: %s",
            transaction_id,
            extra=log_extra(transaction_id),
        )

    LOGGER.info(
        "Processing notification event: "
        "appointmentId=%s eventId=%s eventType=%s",
        appointment_id,
        event.get("eventId"),
        event.get("eventType"),
        extra=log_extra(transaction_id),
    )

    try:
        update_notification_status(
            appointment_id=appointment_id,
            status="PROCESSING",
            transaction_id=transaction_id,
        )

        send_notification(
            event=event,
            transaction_id=transaction_id,
        )

        update_notification_status(
            appointment_id=appointment_id,
            status="SENT",
            transaction_id=transaction_id,
        )

    except Exception:
        LOGGER.exception(
            "Notification processing failed for appointment %s",
            appointment_id,
            extra=log_extra(transaction_id),
        )

        try:
            update_notification_status(
                appointment_id=appointment_id,
                status="FAILED",
                transaction_id=transaction_id,
            )

        except Exception:
            LOGGER.exception(
                "Failed to update notification status "
                "to FAILED for appointment %s",
                appointment_id,
                extra=log_extra(transaction_id),
            )

        raise


def create_consumer() -> Consumer:

    return Consumer(
        {
            "bootstrap.servers":
                KAFKA_BOOTSTRAP_SERVERS,

            "group.id":
                KAFKA_GROUP_ID,

            # Offset is committed only after processing succeeds.
            "enable.auto.commit":
                False,

            # A newly created consumer group reads old events.
            "auto.offset.reset":
                "earliest",

            "session.timeout.ms":
                45000,

            "max.poll.interval.ms":
                300000,
        }
    )


def main() -> None:

    consumer = create_consumer()

    consumer.subscribe(
        [KAFKA_TOPIC]
    )

    LOGGER.info(
        "Notification worker started. "
        "Topic=%s group=%s bootstrapServers=%s",
        KAFKA_TOPIC,
        KAFKA_GROUP_ID,
        KAFKA_BOOTSTRAP_SERVERS,
        extra=log_extra("N/A"),
    )

    try:
        while running:

            message = consumer.poll(
                timeout=1.0
            )

            if message is None:
                continue

            if message.error():

                if (
                    message.error().code()
                    == KafkaError._PARTITION_EOF
                ):
                    continue

                raise KafkaException(
                    message.error()
                )

            transaction_id = "N/A"

            try:
                raw_value = message.value()

                if raw_value is not None:
                    temporary_event = json.loads(
                        raw_value.decode("utf-8")
                    )

                    transaction_id = (
                        temporary_event.get("transactionId")
                        or "N/A"
                    )

                process_message(message)

                consumer.commit(
                    message=message,
                    asynchronous=False,
                )

                LOGGER.info(
                    "Kafka message committed. "
                    "topic=%s partition=%s offset=%s",
                    message.topic(),
                    message.partition(),
                    message.offset(),
                    extra=log_extra(transaction_id),
                )

            except Exception:
                """
                The offset is intentionally not committed.

                Kafka can redeliver the message when the worker
                restarts or when the partition is reassigned.
                """

                LOGGER.exception(
                    "Kafka message was not committed because "
                    "notification processing failed",
                    extra=log_extra(transaction_id),
                )

                time.sleep(5)

    finally:
        LOGGER.info(
            "Closing Kafka consumer",
            extra=log_extra("N/A"),
        )

        consumer.close()


if __name__ == "__main__":

    try:
        main()

    except Exception:
        LOGGER.exception(
            "Notification worker stopped unexpectedly",
            extra=log_extra("N/A"),
        )

        sys.exit(1)