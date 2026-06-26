import json
import logging
import os
import signal
import smtplib
import sys
import time
import uuid
from email.message import EmailMessage
from logging.handlers import TimedRotatingFileHandler
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

LOG_PATH = os.getenv(
    "LOG_PATH",
    "./logs/notification-worker",
)

LOG_LEVEL_NAME = os.getenv(
    "LOG_LEVEL",
    "INFO",
).upper()

LOG_LEVEL = getattr(
    logging,
    LOG_LEVEL_NAME,
    logging.INFO,
)

LOG_FILE_NAME = os.getenv(
    "LOG_FILE_NAME",
    "notification-worker.log",
)


class LoggingContextFilter(logging.Filter):
    """
    Ensures custom logging attributes always exist.

    Without this filter, Python logging raises an error when a log
    statement does not contain transaction_id or appointment_id.
    """

    def filter(
        self,
        record: logging.LogRecord,
    ) -> bool:

        if not hasattr(record, "transaction_id"):
            record.transaction_id = "N/A"

        if not hasattr(record, "appointment_id"):
            record.appointment_id = "N/A"

        return True


def configure_logging() -> logging.Logger:
    os.makedirs(
        LOG_PATH,
        exist_ok=True,
    )

    log_file_path = os.path.join(
        LOG_PATH,
        LOG_FILE_NAME,
    )

    formatter = logging.Formatter(
        fmt=(
            "%(asctime)s "
            "%(levelname)-8s "
            "service=notification-worker "
            "thread=%(threadName)s "
            "transactionId=%(transaction_id)s "
            "appointmentId=%(appointment_id)s "
            "logger=%(name)s - "
            "%(message)s"
        ),
        datefmt="%Y-%m-%dT%H:%M:%S%z",
    )

    context_filter = LoggingContextFilter()

    console_handler = logging.StreamHandler(
        sys.stdout
    )

    console_handler.setLevel(
        LOG_LEVEL
    )

    console_handler.setFormatter(
        formatter
    )

    console_handler.addFilter(
        context_filter
    )

    file_handler = TimedRotatingFileHandler(
        filename=log_file_path,
        when="midnight",
        interval=1,
        backupCount=30,
        encoding="utf-8",
        utc=True,
    )

    file_handler.setLevel(
        LOG_LEVEL
    )

    file_handler.setFormatter(
        formatter
    )

    file_handler.addFilter(
        context_filter
    )

    root_logger = logging.getLogger()
    root_logger.setLevel(LOG_LEVEL)

    # Avoid duplicate handlers when the module is reloaded.
    root_logger.handlers.clear()

    root_logger.addHandler(
        console_handler
    )

    root_logger.addHandler(
        file_handler
    )

    # Reduce third-party library noise.
    logging.getLogger(
        "urllib3"
    ).setLevel(logging.WARNING)

    logging.getLogger(
        "requests"
    ).setLevel(logging.WARNING)

    return logging.getLogger(
        "notification-worker"
    )


LOGGER = configure_logging()


def log_extra(
    transaction_id: str = "N/A",
    appointment_id: str = "N/A",
) -> dict[str, str]:

    return {
        "transaction_id": transaction_id or "N/A",
        "appointment_id": appointment_id or "N/A",
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
).rstrip("/")

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

    del frame

    global running

    LOGGER.info(
        "Shutdown signal received. signal=%s",
        signum,
        extra=log_extra(),
    )

    running = False


signal.signal(
    signal.SIGINT,
    handle_shutdown,
)

signal.signal(
    signal.SIGTERM,
    handle_shutdown,
)


# =========================================================
# Utility methods
# =========================================================

def mask_email(
    email: str | None,
) -> str:

    if not email:
        return "N/A"

    if "@" not in email:
        return "***"

    local_part, domain = email.split(
        "@",
        maxsplit=1,
    )

    if len(local_part) <= 1:
        masked_local = "*"

    elif len(local_part) == 2:
        masked_local = (
            local_part[0]
            + "*"
        )

    else:
        masked_local = (
            local_part[0]
            + ("*" * (len(local_part) - 2))
            + local_part[-1]
        )

    return f"{masked_local}@{domain}"


def decode_message_key(
    message: Message,
) -> str:

    key = message.key()

    if key is None:
        return "N/A"

    if isinstance(key, bytes):
        return key.decode(
            "utf-8",
            errors="replace",
        )

    return str(key)


def get_transaction_id_from_headers(
    message: Message,
) -> str | None:

    headers = message.headers() or []

    for header_name, header_value in headers:

        if header_name.lower() != "x-transaction-id":
            continue

        if header_value is None:
            return None

        if isinstance(header_value, bytes):
            return header_value.decode(
                "utf-8",
                errors="replace",
            )

        return str(header_value)

    return None


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

    context = log_extra(
        transaction_id,
        appointment_id,
    )

    LOGGER.info(
        "Updating notification status. status=%s",
        status,
        extra=context,
    )

    response = requests.patch(
        url=url,
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

    try:
        response.raise_for_status()

    except requests.HTTPError:

        LOGGER.error(
            "Notification status update failed. "
            "status=%s httpStatus=%s response=%s",
            status,
            response.status_code,
            response.text[:500],
            extra=context,
        )

        raise

    LOGGER.info(
        "Notification status updated successfully. "
        "status=%s httpStatus=%s",
        status,
        response.status_code,
        extra=context,
    )


# =========================================================
# Email notification
# =========================================================

def validate_smtp_configuration() -> None:

    missing_values: list[str] = []

    if not SMTP_USERNAME:
        missing_values.append(
            "SMTP_USERNAME"
        )

    if not SMTP_PASSWORD:
        missing_values.append(
            "SMTP_PASSWORD"
        )

    if not SMTP_FROM_EMAIL:
        missing_values.append(
            "SMTP_FROM_EMAIL"
        )

    if missing_values:
        raise ValueError(
            "Missing SMTP configuration: "
            + ", ".join(missing_values)
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

    message["Subject"] = (
        "Appointment Booking Confirmation"
    )

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

    appointment_id = str(
        event.get(
            "appointmentId",
            "N/A",
        )
    )

    recipient_email = event.get(
        "email"
    )

    context = log_extra(
        transaction_id,
        appointment_id,
    )

    message = create_email_message(
        event
    )

    LOGGER.info(
        "Sending appointment confirmation email. recipient=%s",
        mask_email(recipient_email),
        extra=context,
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

        smtp.send_message(
            message
        )

    LOGGER.info(
        "Appointment confirmation email sent successfully. recipient=%s",
        mask_email(recipient_email),
        extra=context,
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

    if isinstance(raw_value, bytes):
        raw_value = raw_value.decode(
            "utf-8",
        )

    try:
        parsed_value = json.loads(
            raw_value
        )

    except json.JSONDecodeError as exception:
        raise ValueError(
            "Kafka message contains invalid JSON"
        ) from exception

    if not isinstance(
        parsed_value,
        dict,
    ):
        raise ValueError(
            "Kafka message must contain a JSON object"
        )

    return parsed_value


def resolve_transaction_id(
    event: dict[str, Any],
    message: Message,
) -> str:

    transaction_id = event.get(
        "transactionId"
    )

    if transaction_id:
        return str(
            transaction_id
        )

    header_transaction_id = (
        get_transaction_id_from_headers(
            message
        )
    )

    if header_transaction_id:
        LOGGER.warning(
            "Kafka event body does not contain transactionId. "
            "Using transaction ID from Kafka header.",
            extra=log_extra(
                header_transaction_id
            ),
        )

        return header_transaction_id

    fallback_transaction_id = str(
        uuid.uuid4()
    )

    LOGGER.warning(
        "Kafka event does not contain transactionId. "
        "Generated fallback transaction ID.",
        extra=log_extra(
            fallback_transaction_id
        ),
    )

    return fallback_transaction_id


def process_message(
    message: Message,
) -> None:

    event = parse_event(
        message
    )

    appointment_id_value = event.get(
        "appointmentId"
    )

    if not appointment_id_value:
        raise ValueError(
            "Kafka event does not contain appointmentId"
        )

    appointment_id = str(
        appointment_id_value
    )

    transaction_id = resolve_transaction_id(
        event,
        message,
    )

    context = log_extra(
        transaction_id,
        appointment_id,
    )

    LOGGER.info(
        "Processing notification event. "
        "eventId=%s eventType=%s topic=%s "
        "partition=%s offset=%s key=%s",
        event.get("eventId"),
        event.get("eventType"),
        message.topic(),
        message.partition(),
        message.offset(),
        decode_message_key(message),
        extra=context,
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

        LOGGER.info(
            "Notification event processed successfully.",
            extra=context,
        )

    except Exception:

        LOGGER.exception(
            "Notification processing failed.",
            extra=context,
        )

        try:
            update_notification_status(
                appointment_id=appointment_id,
                status="FAILED",
                transaction_id=transaction_id,
            )

        except Exception:

            LOGGER.exception(
                "Failed to update notification status to FAILED.",
                extra=context,
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


def extract_logging_context(
    message: Message,
) -> tuple[str, str]:

    try:
        event = parse_event(
            message
        )

        appointment_id = str(
            event.get(
                "appointmentId",
                "N/A",
            )
        )

        transaction_id = (
            event.get("transactionId")
            or get_transaction_id_from_headers(message)
            or "N/A"
        )

        return (
            str(transaction_id),
            appointment_id,
        )

    except Exception:
        return (
            "N/A",
            "N/A",
        )


def main() -> None:

    consumer = create_consumer()

    LOGGER.info(
        "Starting notification worker. "
        "topic=%s group=%s bootstrapServers=%s logPath=%s",
        KAFKA_TOPIC,
        KAFKA_GROUP_ID,
        KAFKA_BOOTSTRAP_SERVERS,
        LOG_PATH,
        extra=log_extra(),
    )

    consumer.subscribe(
        [KAFKA_TOPIC]
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

                    LOGGER.debug(
                        "Reached Kafka partition end. "
                        "topic=%s partition=%s offset=%s",
                        message.topic(),
                        message.partition(),
                        message.offset(),
                        extra=log_extra(),
                    )

                    continue

                LOGGER.error(
                    "Kafka consumer error. code=%s error=%s",
                    message.error().code(),
                    message.error(),
                    extra=log_extra(),
                )

                raise KafkaException(
                    message.error()
                )

            transaction_id, appointment_id = (
                extract_logging_context(
                    message
                )
            )

            context = log_extra(
                transaction_id,
                appointment_id,
            )

            LOGGER.info(
                "Kafka message received. "
                "topic=%s partition=%s offset=%s key=%s",
                message.topic(),
                message.partition(),
                message.offset(),
                decode_message_key(message),
                extra=context,
            )

            try:
                process_message(
                    message
                )

                consumer.commit(
                    message=message,
                    asynchronous=False,
                )

                LOGGER.info(
                    "Kafka message committed successfully. "
                    "topic=%s partition=%s offset=%s",
                    message.topic(),
                    message.partition(),
                    message.offset(),
                    extra=context,
                )

            except Exception:

                LOGGER.exception(
                    "Kafka message was not committed because "
                    "notification processing failed. "
                    "topic=%s partition=%s offset=%s",
                    message.topic(),
                    message.partition(),
                    message.offset(),
                    extra=context,
                )

                # Since the offset is not committed, Kafka can
                # deliver the message again.
                time.sleep(5)

    finally:

        LOGGER.info(
            "Closing Kafka consumer.",
            extra=log_extra(),
        )

        consumer.close()

        LOGGER.info(
            "Kafka consumer closed.",
            extra=log_extra(),
        )


if __name__ == "__main__":

    try:
        main()

    except Exception:

        LOGGER.exception(
            "Notification worker stopped unexpectedly.",
            extra=log_extra(),
        )

        sys.exit(1)