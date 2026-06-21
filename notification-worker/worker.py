import json
import logging
import os
import signal
import sys
import time
from typing import Any

import requests
from confluent_kafka import Consumer, KafkaError, KafkaException, Message


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)

LOGGER = logging.getLogger("notification-worker")

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


def handle_shutdown(signum: int, frame: Any) -> None:
    global running
    LOGGER.info("Shutdown signal received: %s", signum)
    running = False


signal.signal(signal.SIGINT, handle_shutdown)
signal.signal(signal.SIGTERM, handle_shutdown)


def update_notification_status(
    appointment_id: str,
    status: str,
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
        },
        json={
            "status": status,
        },
        timeout=10,
    )

    response.raise_for_status()

    LOGGER.info(
        "Notification status updated: appointment=%s status=%s",
        appointment_id,
        status,
    )


def send_notification(event: dict[str, Any]) -> None:
    """
    Replace this simulated notification with an email,
    SMS or third-party notification provider later.
    """

    LOGGER.info(
        "Sending appointment notification to %s <%s>",
        event.get("fullName"),
        event.get("email"),
    )

    LOGGER.info(
        "Appointment time: %s to %s",
        event.get("startTime"),
        event.get("endTime"),
    )

    # Simulates external email/SMS processing.
    time.sleep(1)

    LOGGER.info(
        "Notification successfully sent for appointment %s",
        event.get("appointmentId"),
    )


def process_message(message: Message) -> None:
    raw_value = message.value()

    if raw_value is None:
        raise ValueError("Kafka message does not contain a value")

    event = json.loads(raw_value.decode("utf-8"))

    appointment_id = event.get("appointmentId")

    if not appointment_id:
        raise ValueError(
            "Kafka event does not contain appointmentId"
        )

    try:
        update_notification_status(
            appointment_id,
            "PROCESSING",
        )

        send_notification(event)

        update_notification_status(
            appointment_id,
            "SENT",
        )

    except Exception:
        LOGGER.exception(
            "Notification processing failed for appointment %s",
            appointment_id,
        )

        try:
            update_notification_status(
                appointment_id,
                "FAILED",
            )
        except Exception:
            LOGGER.exception(
                "Failed to update notification status to FAILED"
            )

        raise


def create_consumer() -> Consumer:
    return Consumer(
        {
            "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
            "group.id": KAFKA_GROUP_ID,

            # Commit only after successful processing.
            "enable.auto.commit": False,

            # New group starts with existing messages.
            "auto.offset.reset": "earliest",

            "session.timeout.ms": 45000,
        }
    )


def main() -> None:
    consumer = create_consumer()

    consumer.subscribe([KAFKA_TOPIC])

    LOGGER.info(
        "Notification worker started. Topic=%s, group=%s",
        KAFKA_TOPIC,
        KAFKA_GROUP_ID,
    )

    try:
        while running:
            message = consumer.poll(timeout=1.0)

            if message is None:
                continue

            if message.error():
                if (
                    message.error().code()
                    == KafkaError._PARTITION_EOF
                ):
                    continue

                raise KafkaException(message.error())

            try:
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
                )

            except Exception:
                """
                The offset is not committed.

                The message can be redelivered when the worker
                restarts or the partition is reassigned.
                """
                LOGGER.exception(
                    "Message was not committed because processing failed"
                )

                time.sleep(5)

    finally:
        LOGGER.info("Closing Kafka consumer")
        consumer.close()


if __name__ == "__main__":
    try:
        main()
    except Exception:
        LOGGER.exception("Worker stopped unexpectedly")
        sys.exit(1)