import logging
import os
from logging.handlers import TimedRotatingFileHandler


class DefaultContextFilter(logging.Filter):
    """
    Ensures transaction_id and appointment_id always exist,
    preventing logging formatter errors.
    """

    def filter(self, record: logging.LogRecord) -> bool:
        if not hasattr(record, "transaction_id"):
            record.transaction_id = "N/A"

        if not hasattr(record, "appointment_id"):
            record.appointment_id = "N/A"

        return True


def configure_logging() -> None:
    log_path = os.getenv(
        "LOG_PATH",
        "./logs/notification-worker"
    )

    os.makedirs(log_path, exist_ok=True)

    log_file = os.path.join(
        log_path,
        "notification-worker.log"
    )

    log_level_name = os.getenv(
        "LOG_LEVEL",
        "INFO"
    ).upper()

    log_level = getattr(
        logging,
        log_level_name,
        logging.INFO
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
        datefmt="%Y-%m-%dT%H:%M:%S%z"
    )

    context_filter = DefaultContextFilter()

    console_handler = logging.StreamHandler()
    console_handler.setLevel(log_level)
    console_handler.setFormatter(formatter)
    console_handler.addFilter(context_filter)

    file_handler = TimedRotatingFileHandler(
        filename=log_file,
        when="midnight",
        interval=1,
        backupCount=30,
        encoding="utf-8",
        utc=True
    )

    file_handler.setLevel(log_level)
    file_handler.setFormatter(formatter)
    file_handler.addFilter(context_filter)

    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)

    # Prevent duplicate handlers during reload/testing.
    root_logger.handlers.clear()

    root_logger.addHandler(console_handler)
    root_logger.addHandler(file_handler)

    # Reduce noisy third-party logs.
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    logging.getLogger("requests").setLevel(logging.WARNING)