FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .

RUN pip install \
    --no-cache-dir \
    -r requirements.txt

COPY worker.py .

RUN useradd \
    --create-home \
    --shell /usr/sbin/nologin \
    workeruser

USER workeruser

CMD ["python", "-u", "worker.py"]