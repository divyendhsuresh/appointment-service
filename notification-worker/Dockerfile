FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .

RUN pip install \
    --no-cache-dir \
    -r requirements.txt

RUN useradd \
    --create-home \
    --shell /usr/sbin/nologin \
    workeruser

COPY worker.py .

# Create the log directory before switching to the non-root user.
RUN mkdir -p /app/logs \
    && chown -R workeruser:workeruser /app

ENV PYTHONUNBUFFERED=1
ENV LOG_PATH=/app/logs
ENV LOG_LEVEL=INFO
ENV LOG_FILE_NAME=notification-worker.log

USER workeruser

CMD ["python", "-u", "worker.py"]