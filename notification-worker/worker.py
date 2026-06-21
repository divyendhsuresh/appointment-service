import json
import logging
import os
import signal
import smtplib
import sys
import time
import uuid
from email.message import EmailMessage
from typing import Any

import requests
from confluent_kafka import (
    Consumer,
    KafkaError,
    KafkaException,
    Message,
)


# =========================================================
# Logging configuration
# =========================================================

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


def log_extra(transaction_id: str) -> dict[str, str]:
    return {
        "transaction_id": transaction_id,
    }


# =========================================================
# Kafka configuration
# =========================================================

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


# =========================================================
# Appointment service configuration
# =========================================================

APPOINTMENT_SERVICE_URL = os.getenv(
    "APPOINTMENT_SERVICE_URL",
    "http://localhost:8080",
)

INTERNAL_API_KEY = os.getenv(
    "INTERNAL_API_KEY",
    "mykare-internal-secret",
)


# =========================================================
# SMTP configuration
# =========================================================

SMTP_HOST = os.getenv(
    "SMTP_HOST",
    "smtp.gmail.com",
)

SMTP_PORT = int(
    os.getenv(
        "SMTP_PORT",
        "587",
    )
)

SMTP_USERNAME = os.getenv(
    "SMTP_USERNAME",
)

SMTP_PASSWORD = os.getenv(
    "SMTP_PASSWORD",
)

SMTP_FROM_EMAIL = os.getenv(
    "SMTP_FROM_EMAIL",
    SMTP_USERNAME or "",
)

SMTP_USE_TLS = os.getenv(
    "SMTP_USE_TLS",
    "true",
).lower() == "true"


# =========================================================
# Application lifecycle
# =========================================================

running = True


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


# =========================================================
# Notification status update
# =========================================================

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


# =========================================================
# Email notification
# =========================================================

def validate_smtp_configuration() -> None:

    if not SMTP_USERNAME:
        raise ValueError(
            "SMTP_USERNAME is not configured"
        )

    if not SMTP_PASSWORD:
        raise ValueError(
            "SMTP_PASSWORD is not configured"
        )

    if not SMTP_FROM_EMAIL:
        raise ValueError(
            "SMTP_FROM_EMAIL is not configured"
        )


def create_email_message(
    event: dict[str, Any],
) -> EmailMessage:

    recipient_email = event.get("email")
    full_name = event.get("fullName") or "User"
    appointment_id = event.get("appointmentId")
    start_time = event.get("startTime")
    end_time = event.get("endTime")
    reason = event.get("reason") or "Not specified"

    if not recipient_email:
        raise ValueError(
            "Kafka event does not contain recipient email"
        )

    message = EmailMessage()

    message["Subject"] = "Appointment Booking Confirmation"
    message["From"] = SMTP_FROM_EMAIL
    message["To"] = recipient_email

    message.set_content(
        f"""
Hello {full_name},

Your appointment has been successfully booked.

Appointment details:

Appointment ID: {appointment_id}
Start Time: {start_time}
End Time: {end_time}
Reason: {reason}

Please arrive a few minutes before your scheduled time.

Thank you,
MyKare Appointment Team
""".strip()
    )

    return message


def send_notification(
    event: dict[str, Any],
    transaction_id: str,
) -> None:

    validate_smtp_configuration()

    recipient_email = event.get("email")

    message = create_email_message(event)

    LOGGER.info(
        "Sending appointment confirmation email to %s",
        recipient_email,
        extra=log_extra(transaction_id),
    )

    with smtplib.SMTP(
        SMTP_HOST,
        SMTP_PORT,
        timeout=30,
    ) as smtp:

        smtp.ehlo()

        if SMTP_USE_TLS:
            smtp.starttls()
            smtp.ehlo()

        smtp.login(
            SMTP_USERNAME,
            SMTP_PASSWORD,
        )

        smtp.send_message(message)

    LOGGER.info(
        "Appointment confirmation email sent successfully "
        "to %s for appointment %s",
        recipient_email,
        event.get("appointmentId"),
        extra=log_extra(transaction_id),
    )


# =========================================================
# Kafka message processing
# =========================================================

def parse_event(
    message: Message,
) -> dict[str, Any]:

    raw_value = message.value()

    if raw_value is None:
        raise ValueError(
            "Kafka message does not contain a value"
        )

    try:
        return json.loads(
            raw_value.decode("utf-8")
        )

    except json.JSONDecodeError as exception:
        raise ValueError(
            "Kafka message contains invalid JSON"
        ) from exception


def resolve_transaction_id(
    event: dict[str, Any],
) -> str:

    transaction_id = event.get("transactionId")

    if transaction_id:
        return transaction_id

    fallback_transaction_id = str(uuid.uuid4())

    LOGGER.warning(
        "Kafka event does not contain transactionId. "
        "Generated fallback transaction ID: %s",
        fallback_transaction_id,
        extra=log_extra(fallback_transaction_id),
    )

    return fallback_transaction_id


def process_message(
    message: Message,
) -> None:

    event = parse_event(message)

    appointment_id = event.get("appointmentId")

    if not appointment_id:
        raise ValueError(
            "Kafka event does not contain appointmentId"
        )

    transaction_id = resolve_transaction_id(event)

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


# =========================================================
# Kafka consumer
# =========================================================

def create_consumer() -> Consumer:

    return Consumer(
        {
            "bootstrap.servers":
                KAFKA_BOOTSTRAP_SERVERS,

            "group.id":
                KAFKA_GROUP_ID,

            "enable.auto.commit":
                False,

            "auto.offset.reset":
                "earliest",

            "session.timeout.ms":
                45000,

            "max.poll.interval.ms":
                300000,
        }
    )


def extract_transaction_id_for_logging(
    message: Message,
) -> str:

    try:
        event = parse_event(message)

        return event.get("transactionId") or "N/A"

    except Exception:
        return "N/A"


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

            transaction_id = (
                extract_transaction_id_for_logging(message)
            )

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
                    extra=log_extra(transaction_id),
                )

            except Exception:
                LOGGER.exception(
                    "Kafka message was not committed because "
                    "notification processing failed",
                    extra=log_extra(transaction_id),
                )

                # Prevent continuous high-speed retry.
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