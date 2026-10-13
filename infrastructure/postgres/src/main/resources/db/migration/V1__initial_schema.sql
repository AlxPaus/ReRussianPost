CREATE TABLE posttrack.users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE CHECK (email = lower(email)),
    password_hash VARCHAR(255) NOT NULL,
    roles TEXT[] NOT NULL CHECK (cardinality(roles) > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE posttrack.delivery_quotes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES posttrack.users(id),
    from_index CHAR(6) NOT NULL,
    to_index CHAR(6) NOT NULL,
    weight_grams INTEGER NOT NULL CHECK (weight_grams BETWEEN 1 AND 50000),
    length_cm INTEGER NOT NULL CHECK (length_cm BETWEEN 1 AND 150),
    width_cm INTEGER NOT NULL CHECK (width_cm BETWEEN 1 AND 150),
    height_cm INTEGER NOT NULL CHECK (height_cm BETWEEN 1 AND 150),
    service_level VARCHAR(16) NOT NULL CHECK (service_level IN ('STANDARD', 'EXPRESS')),
    price_minor BIGINT NOT NULL CHECK (price_minor >= 0),
    currency CHAR(3) NOT NULL,
    delivery_from DATE NOT NULL,
    delivery_to DATE NOT NULL,
    provider VARCHAR(64) NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CHECK (length_cm + width_cm + height_cm <= 300),
    CHECK (delivery_to >= delivery_from),
    CHECK (expires_at > created_at)
);

CREATE TABLE posttrack.shipments (
    id UUID PRIMARY KEY,
    tracking_number VARCHAR(32) NOT NULL UNIQUE,
    sender_id UUID NOT NULL REFERENCES posttrack.users(id),
    quote_id UUID NOT NULL REFERENCES posttrack.delivery_quotes(id),
    recipient_name VARCHAR(200) NOT NULL,
    recipient_phone VARCHAR(16) NOT NULL,
    recipient_email VARCHAR(320),
    from_index CHAR(6) NOT NULL,
    from_region VARCHAR(255) NOT NULL,
    from_city VARCHAR(255) NOT NULL,
    from_address_line VARCHAR(255) NOT NULL,
    to_index CHAR(6) NOT NULL,
    to_region VARCHAR(255) NOT NULL,
    to_city VARCHAR(255) NOT NULL,
    to_address_line VARCHAR(255) NOT NULL,
    declared_weight_grams INTEGER NOT NULL CHECK (declared_weight_grams BETWEEN 1 AND 50000),
    actual_weight_grams INTEGER CHECK (actual_weight_grams BETWEEN 1 AND 50000),
    status VARCHAR(32) NOT NULL CHECK (
        status IN ('CREATED', 'ACCEPTED', 'IN_TRANSIT', 'READY_FOR_PICKUP', 'DELIVERED')
    ),
    expected_delivery_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (
        (status = 'CREATED' AND actual_weight_grams IS NULL)
        OR (status <> 'CREATED' AND actual_weight_grams IS NOT NULL)
    )
);

CREATE INDEX idx_shipments_sender ON posttrack.shipments(sender_id, created_at DESC);
CREATE INDEX idx_shipments_phone ON posttrack.shipments(recipient_phone);

CREATE TABLE posttrack.tracking_events (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES posttrack.shipments(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL CHECK (
        status IN ('CREATED', 'ACCEPTED', 'IN_TRANSIT', 'READY_FOR_PICKUP', 'DELIVERED')
    ),
    location VARCHAR(255) NOT NULL,
    expected_delivery_date DATE,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tracking_timeline ON posttrack.tracking_events(shipment_id, occurred_at);

CREATE TABLE posttrack.storage_cells (
    id UUID PRIMARY KEY,
    post_office_index CHAR(6) NOT NULL,
    rack INTEGER NOT NULL CHECK (rack > 0),
    shelf INTEGER NOT NULL CHECK (shelf > 0),
    cell INTEGER NOT NULL CHECK (cell > 0),
    occupied_by_shipment_id UUID UNIQUE REFERENCES posttrack.shipments(id),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (post_office_index, rack, shelf, cell)
);

CREATE TABLE posttrack.pickup_codes (
    shipment_id UUID PRIMARY KEY REFERENCES posttrack.shipments(id) ON DELETE CASCADE,
    code_hash VARCHAR(255) NOT NULL CHECK (code_hash <> ''),
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts_left INTEGER NOT NULL DEFAULT 5 CHECK (attempts_left BETWEEN 0 AND 5),
    used_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (expires_at > issued_at),
    CHECK (used_at IS NULL OR (used_at >= issued_at AND used_at < expires_at))
);
