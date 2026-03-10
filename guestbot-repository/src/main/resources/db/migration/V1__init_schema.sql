-- V1__init_schema.sql

-- Owners
CREATE TABLE owners (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    phone         VARCHAR(50)  NOT NULL,
    telegram_chat_id VARCHAR(50),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP
);

-- Hotels
CREATE TABLE hotels (
    id                   BIGSERIAL PRIMARY KEY,
    owner_id             BIGINT NOT NULL REFERENCES owners(id),
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    address              VARCHAR(500),
    city                 VARCHAR(100),
    phone                VARCHAR(50),
    email                VARCHAR(255),
    website              VARCHAR(255),
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    amenities            JSONB,
    check_in_time        VARCHAR(10),
    check_out_time       VARCHAR(10),
    min_age              INT,
    pets_allowed         BOOLEAN DEFAULT FALSE,
    children_allowed     BOOLEAN DEFAULT TRUE,
    cancellation_policy  TEXT,
    bank_name            VARCHAR(255),
    bank_account         VARCHAR(255),
    tax_id               VARCHAR(100),
    bank_recipient       VARCHAR(255),
    telegram_bot_token   VARCHAR(255) UNIQUE,
    bot_active           BOOLEAN DEFAULT FALSE,
    welcome_message      TEXT,
    off_hours_message    TEXT,
    working_hours_start  VARCHAR(10),
    working_hours_end    VARCHAR(10),
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP
);

-- Hotel photos
CREATE TABLE hotel_photos (
    id          BIGSERIAL PRIMARY KEY,
    hotel_id    BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    minio_key   VARCHAR(500),
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

-- Rooms
CREATE TABLE rooms (
    id              BIGSERIAL PRIMARY KEY,
    hotel_id        BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    type            VARCHAR(100) NOT NULL,
    description     TEXT,
    capacity        INT NOT NULL,
    count           INT NOT NULL DEFAULT 1,
    price_per_night DECIMAL(10,2) NOT NULL,
    amenities       JSONB,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

-- Room photos
CREATE TABLE room_photos (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    minio_key   VARCHAR(500),
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

-- Bookings
CREATE TABLE bookings (
    id               BIGSERIAL PRIMARY KEY,
    booking_number   VARCHAR(50) NOT NULL UNIQUE,
    hotel_id         BIGINT NOT NULL REFERENCES hotels(id),
    room_id          BIGINT NOT NULL REFERENCES rooms(id),
    guest_name       VARCHAR(255) NOT NULL,
    guest_phone      VARCHAR(50) NOT NULL,
    guest_email      VARCHAR(255),
    check_in         DATE NOT NULL,
    check_out        DATE NOT NULL,
    nights           INT NOT NULL,
    total_amount     DECIMAL(10,2) NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_deadline TIMESTAMP,
    telegram_chat_id BIGINT,
    source           VARCHAR(50) DEFAULT 'TELEGRAM',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP
);

-- Availability blocks
CREATE TABLE availability_blocks (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    hotel_id    BIGINT NOT NULL REFERENCES hotels(id),
    date_from   DATE NOT NULL,
    date_to     DATE NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

-- Conversations
CREATE TABLE conversations (
    id               BIGSERIAL PRIMARY KEY,
    hotel_id         BIGINT NOT NULL REFERENCES hotels(id),
    telegram_chat_id BIGINT NOT NULL,
    guest_name       VARCHAR(255),
    guest_phone      VARCHAR(50),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    last_message_at  TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP,
    UNIQUE(hotel_id, telegram_chat_id)
);

-- Messages
CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_type     VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

-- Knowledge base
CREATE TABLE knowledge_base (
    id          BIGSERIAL PRIMARY KEY,
    hotel_id    BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    category    VARCHAR(100) NOT NULL,
    question    VARCHAR(500) NOT NULL,
    answer      TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

-- Payment transactions
CREATE TABLE payment_transactions (
    id                  BIGSERIAL PRIMARY KEY,
    booking_id          BIGINT NOT NULL UNIQUE REFERENCES bookings(id),
    external_payment_id VARCHAR(255) NOT NULL UNIQUE,
    amount              DECIMAL(10,2) NOT NULL,
    commission_amount   DECIMAL(10,2) NOT NULL,
    hotel_amount        DECIMAL(10,2) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    card_type           VARCHAR(50),
    payment_url         VARCHAR(500),
    idempotency_key     VARCHAR(255) UNIQUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

-- Failed notifications (retry mechanism)
CREATE TABLE failed_notifications (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB NOT NULL,
    error       TEXT,
    attempts    INT NOT NULL DEFAULT 0,
    next_retry  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ==================
-- INDEXES
-- ==================

-- Bookings: самый частый запрос — проверка доступности
CREATE INDEX idx_bookings_room_dates ON bookings(room_id, check_in, check_out)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN');

CREATE INDEX idx_bookings_hotel_id ON bookings(hotel_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_payment_deadline ON bookings(payment_deadline)
    WHERE status = 'PENDING_PAYMENT';
CREATE INDEX idx_bookings_check_in ON bookings(check_in)
    WHERE status = 'CONFIRMED';

-- Hotels
CREATE INDEX idx_hotels_owner_id ON hotels(owner_id);
CREATE INDEX idx_hotels_bot_token ON hotels(telegram_bot_token);

-- Rooms
CREATE INDEX idx_rooms_hotel_id ON rooms(hotel_id);

-- Conversations
CREATE INDEX idx_conversations_hotel_id ON conversations(hotel_id);
CREATE INDEX idx_conversations_telegram ON conversations(telegram_chat_id);

-- Messages
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);

-- Knowledge base
CREATE INDEX idx_knowledge_base_hotel_id ON knowledge_base(hotel_id);

-- Availability blocks
CREATE INDEX idx_availability_blocks_room ON availability_blocks(room_id, date_from, date_to);

-- Failed notifications
CREATE INDEX idx_failed_notifications_retry ON failed_notifications(next_retry, attempts);
