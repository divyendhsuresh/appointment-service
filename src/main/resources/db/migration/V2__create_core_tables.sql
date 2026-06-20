CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(30) NOT NULL DEFAULT 'USER',
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE appointment_slots
(
    id         UUID PRIMARY KEY,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time   TIMESTAMP WITH TIME ZONE NOT NULL,
    status     VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    version    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_slot_time
        CHECK (end_time > start_time),

    CONSTRAINT uq_appointment_slot_time
        UNIQUE (start_time, end_time)
);

CREATE TABLE appointments
(
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    slot_id             UUID NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(30) NOT NULL,
    notification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointment_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_appointment_slot
        FOREIGN KEY (slot_id)
        REFERENCES appointment_slots (id)
);

CREATE UNIQUE INDEX uq_confirmed_appointment_slot
    ON appointments (slot_id)
    WHERE status = 'CONFIRMED';

CREATE TABLE appointment_history
(
    id              UUID PRIMARY KEY,
    appointment_id  UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30),
    new_status      VARCHAR(30),
    description     VARCHAR(500),
    changed_by      UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_history_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments (id)
);

CREATE INDEX idx_appointments_user_id
    ON appointments (user_id);

CREATE INDEX idx_slots_start_time
    ON appointment_slots (start_time);

CREATE INDEX idx_history_appointment_id
    ON appointment_history (appointment_id);