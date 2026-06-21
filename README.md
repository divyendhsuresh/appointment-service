# Healthcare Appointment Platform

Backend service for a healthcare appointment booking platform built with Spring Boot, PostgreSQL, Kafka, Docker, and a Python notification worker.

## Features

- User registration
- User login with JWT authentication
- User logout with JWT token blacklisting
- Mandatory `X-Transaction-Id` request tracing
- Appointment slot generation
- Fetch available appointment slots
- Appointment booking
- Concurrent double-booking prevention
- Fetch logged-in user's appointments
- Appointment cancellation
- Appointment history tracking
- Kafka-based notification processing
- Python notification worker
- Notification status tracking
- PostgreSQL persistence
- Flyway database migrations
- Swagger/OpenAPI documentation
- Docker Compose support
- CORS configuration

## Technology Stack

### Backend

- Java 21
- Spring Boot 3.5.7
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Kafka
- Hibernate
- Flyway
- JWT using JJWT
- Maven

### Infrastructure

- PostgreSQL 16
- Apache Kafka
- Docker
- Docker Compose

### Notification Worker

- Python 3.12
- confluent-kafka
- requests

## Project Structure

```text
appointment-service/
├── src/
│   ├── main/
│   │   ├── java/com/mykare/appointment_service/
│   │   │   ├── Common/
│   │   │   ├── Config/
│   │   │   ├── Controller/
│   │   │   ├── Dto/
│   │   │   ├── Entity/
│   │   │   ├── Enums/
│   │   │   ├── Exception/
│   │   │   ├── Filter/
│   │   │   ├── Messaging/
│   │   │   ├── Repository/
│   │   │   ├── Scheduler/
│   │   │   ├── Security/
│   │   │   └── Service/
│   │   └── resources/
│   │       ├── db/migration/
│   │       └── application.properties
├── notification-worker/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── worker.py
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## System Architecture

```text
Client
  |
  | HTTP + JWT + X-Transaction-Id
  v
Spring Boot Appointment Service
  |
  | JPA / Flyway
  v
PostgreSQL

Spring Boot
  |
  | AppointmentNotificationEvent
  v
Kafka
  |
  v
Python Notification Worker
  |
  | Internal notification-status API
  v
Spring Boot
```

## Main APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a user |
| POST | `/api/v1/auth/login` | Login and receive JWT |
| POST | `/api/v1/auth/logout` | Logout and blacklist JWT |
| GET | `/api/v1/slots/available` | Fetch available slots |
| POST | `/api/v1/appointments` | Book an appointment |
| GET | `/api/v1/appointments` | Fetch logged-in user's appointments |
| PATCH | `/api/v1/appointments/{appointmentId}/cancel` | Cancel appointment |

## Required Request Header

Every business API request must contain:

```http
X-Transaction-Id: TXN-10001
```

Protected APIs must also contain:

```http
Authorization: Bearer <JWT_TOKEN>
```

The same transaction ID is added to logs through MDC, returned in the response header, propagated to Kafka events, and used by the Python notification worker.

## Appointment Slot Rules

- Working hours: 9:00 AM to 6:00 PM
- Slot duration: 1 hour
- Slots per day: 9
- Sunday can be excluded
- Slots are generated for tomorrow and two days ahead during startup
- The scheduler creates slots for two days ahead every day

## Appointment Statuses

```text
CONFIRMED
CANCELLED
COMPLETED
```

## Slot Statuses

```text
AVAILABLE
BOOKED
BLOCKED
```

## Notification Statuses

```text
PENDING
PROCESSING
SENT
FAILED
```

## Database Tables

```text
users
appointment_slots
appointments
appointment_history
```

Flyway migrations are stored under:

```text
src/main/resources/db/migration
```

## Concurrency Handling

Appointment booking uses pessimistic locking on the selected slot:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

The booking transaction locks the slot, verifies availability, creates the appointment, marks the slot as booked, creates history, and commits everything together.

## Kafka Notification Flow

1. Appointment is saved with notification status `PENDING`.
2. The database transaction commits.
3. Spring publishes an event to Kafka.
4. The Python worker consumes the event.
5. Status changes to `PROCESSING`.
6. The worker sends or simulates the notification.
7. Status changes to `SENT`.
8. Failures change the status to `FAILED`.

Kafka topic:

```text
appointment-notifications
```

## Prerequisites

Install:

- Docker
- Docker Compose
- Java 21
- Maven

Java and Maven are only required when running the application outside Docker.

## Run with Docker Compose

Build and start all services:

```bash
docker compose up --build -d
```

Check status:

```bash
docker compose ps
```

View all logs:

```bash
docker compose logs -f
```

View Spring Boot logs:

```bash
docker compose logs -f appointment-service
```

View notification worker logs:

```bash
docker compose logs -f notification-worker
```

Stop services:

```bash
docker compose down
```

Remove services and PostgreSQL data:

```bash
docker compose down -v
```

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

## Swagger UI

Open:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

### Using JWT in Swagger

1. Call the login API.
2. Copy the `accessToken`.
3. Click **Authorize**.
4. Paste only the token.
5. Do not include the word `Bearer`.
6. Enter a value for `X-Transaction-Id`.
7. Execute the protected API.

## API Examples

### Register

```bash
curl -X POST \
"http://localhost:8080/api/v1/auth/register" \
-H "Content-Type: application/json" \
-H "X-Transaction-Id: TXN-REGISTER-10001" \
-d '{
  "fullName": "Divyendh Suresh",
  "email": "divyendh@example.com",
  "password": "Password@123",
  "phone": "9876543210"
}'
```

### Login

```bash
curl -X POST \
"http://localhost:8080/api/v1/auth/login" \
-H "Content-Type: application/json" \
-H "X-Transaction-Id: TXN-LOGIN-10001" \
-d '{
  "email": "divyendh@example.com",
  "password": "Password@123"
}'
```

### Fetch Available Slots

```bash
curl \
"http://localhost:8080/api/v1/slots/available?date=2026-06-23" \
-H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
-H "X-Transaction-Id: TXN-SLOTS-10001"
```

### Book Appointment

```bash
curl -X POST \
"http://localhost:8080/api/v1/appointments" \
-H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
-H "X-Transaction-Id: TXN-BOOKING-10001" \
-H "Content-Type: application/json" \
-d '{
  "slotId": "YOUR_SLOT_ID",
  "reason": "General consultation"
}'
```

### Fetch User Appointments

```bash
curl \
"http://localhost:8080/api/v1/appointments" \
-H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
-H "X-Transaction-Id: TXN-APPOINTMENTS-10001"
```

### Cancel Appointment

```bash
curl -X PATCH \
"http://localhost:8080/api/v1/appointments/YOUR_APPOINTMENT_ID/cancel" \
-H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
-H "X-Transaction-Id: TXN-CANCEL-10001"
```

### Logout

```bash
curl -X POST \
"http://localhost:8080/api/v1/auth/logout" \
-H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
-H "X-Transaction-Id: TXN-LOGOUT-10001"
```

## Standard Response Structure

Success response:

```json
{
  "status": {
    "code": 200,
    "message": "Request completed successfully",
    "timestamp": "2026-06-21T12:00:00Z"
  },
  "data": {}
}
```

Error response:

```json
{
  "status": {
    "code": 400,
    "message": "Invalid request",
    "timestamp": "2026-06-21T12:00:00Z"
  },
  "data": {
    "error": "Error description",
    "validationErrors": null
  }
}
```

## Docker Database Access

```bash
docker exec -it appointment-postgres \
psql -U admin -d appointment_db
```

List tables:

```sql
\dt
```

## Troubleshooting

### Spring Boot container keeps restarting

```bash
docker compose logs -f appointment-service
```

### Notification worker keeps restarting

```bash
docker compose logs -f notification-worker
```

### API returns missing transaction ID

Add:

```http
X-Transaction-Id: TXN-10001
```

### API returns 401

Add:

```http
Authorization: Bearer <JWT_TOKEN>
```

### Swagger UI does not load

Ensure these paths are permitted in Spring Security and excluded from the transaction filter:

```text
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**
```

### No slots returned

Slots must already exist in `appointment_slots`. The startup initializer creates tomorrow and two-days-ahead slots, and the scheduler creates the two-days-ahead slots daily.

## Security Notes

- Passwords are stored using BCrypt.
- JWT-protected APIs are stateless.
- Logged-out JWT identifiers are stored in an in-memory blacklist.
- Internal worker calls use `X-Internal-Api-Key`.
- CORS allows only configured frontend origins.
- `X-Transaction-Id` is mandatory on business API requests.

For production:

- move the token blacklist to Redis
- move secrets to a secret manager
- use HTTPS
- use service-to-service authentication or mTLS
- use a transactional outbox for reliable Kafka publishing
- configure Kafka authentication and encryption
- add automated tests and monitoring

## Future Improvements

- Redis-based logout blacklist
- Transactional outbox pattern
- Dead-letter Kafka topic
- Notification retries
- Real email or SMS provider
- Appointment history API
- Pagination and filtering
- Admin APIs
- Kubernetes deployment
- Helm charts
- Unit and integration tests

## License

Private project for evaluation and demonstration purposes.
