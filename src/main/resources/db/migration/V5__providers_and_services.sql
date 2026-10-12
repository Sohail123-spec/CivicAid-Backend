CREATE TABLE providers (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    website_url VARCHAR(2048),
    source_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(2048),
    verification_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    last_verified_at DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_providers_code UNIQUE (code),
    CONSTRAINT ck_providers_code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_providers_type CHECK (provider_type IN (
        'NGO', 'REHABILITATION_CENTRE', 'AT_SUPPLIER_VENDOR',
        'HOSPITAL_INSTITUTION', 'REPAIR_SERVICE_PROVIDER')),
    CONSTRAINT ck_providers_website_url_https
        CHECK (website_url IS NULL OR website_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_providers_source_url_https
        CHECK (source_url IS NULL OR source_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_providers_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_providers_verified_date
        CHECK (verification_status <> 'VERIFIED' OR last_verified_at IS NOT NULL),
    CONSTRAINT ck_providers_last_verified_not_future
        CHECK (last_verified_at IS NULL OR last_verified_at <= CURRENT_DATE)
);

CREATE UNIQUE INDEX ux_providers_name_normalized ON providers (lower(btrim(name)));
CREATE INDEX idx_providers_public_lookup
    ON providers (provider_type, active, verification_status, last_verified_at, name);

CREATE TABLE provider_locations (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    address_line_1 VARCHAR(250) NOT NULL,
    address_line_2 VARCHAR(250),
    locality VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state_code CHAR(2) NOT NULL,
    postal_code CHAR(6),
    country_code CHAR(2) NOT NULL DEFAULT 'IN',
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    public_phone VARCHAR(30),
    public_email VARCHAR(254),
    accessibility_notes VARCHAR(1000),
    source_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(2048),
    verification_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    last_verified_at DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_provider_locations_provider
        FOREIGN KEY (provider_id) REFERENCES providers (id) ON DELETE RESTRICT,
    CONSTRAINT uq_provider_locations_id_provider UNIQUE (id, provider_id),
    CONSTRAINT ck_provider_locations_state_code CHECK (state_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_provider_locations_postal_code CHECK (postal_code IS NULL OR postal_code ~ '^[0-9]{6}$'),
    CONSTRAINT ck_provider_locations_country_code CHECK (country_code = 'IN'),
    CONSTRAINT ck_provider_locations_coordinates_paired
        CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)),
    CONSTRAINT ck_provider_locations_latitude_india
        CHECK (latitude IS NULL OR latitude BETWEEN 6.000000 AND 38.000000),
    CONSTRAINT ck_provider_locations_longitude_india
        CHECK (longitude IS NULL OR longitude BETWEEN 68.000000 AND 98.000000),
    CONSTRAINT ck_provider_locations_contact_route
        CHECK (public_phone IS NOT NULL OR public_email IS NOT NULL),
    CONSTRAINT ck_provider_locations_source_url_https
        CHECK (source_url IS NULL OR source_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_provider_locations_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_provider_locations_verified_date
        CHECK (verification_status <> 'VERIFIED' OR last_verified_at IS NOT NULL),
    CONSTRAINT ck_provider_locations_last_verified_not_future
        CHECK (last_verified_at IS NULL OR last_verified_at <= CURRENT_DATE)
);

CREATE INDEX idx_provider_locations_provider_id ON provider_locations (provider_id);
CREATE INDEX idx_provider_locations_public_lookup
    ON provider_locations (state_code, city, active, verification_status, last_verified_at);

CREATE TABLE provider_services (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    service_type VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    source_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(2048),
    verification_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    last_verified_at DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_services_provider_code UNIQUE (provider_id, code),
    CONSTRAINT uq_provider_services_id_provider UNIQUE (id, provider_id),
    CONSTRAINT fk_provider_services_provider
        FOREIGN KEY (provider_id) REFERENCES providers (id) ON DELETE RESTRICT,
    CONSTRAINT ck_provider_services_code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_provider_services_type CHECK (service_type IN (
        'INFORMATION_AND_REFERRAL', 'ASSESSMENT_AND_FITTING', 'AT_SUPPLY_OR_DISPENSING',
        'TRAINING', 'MAINTENANCE', 'REPAIR', 'REPLACEMENT_OR_REASSESSMENT_SUPPORT')),
    CONSTRAINT ck_provider_services_source_url_https
        CHECK (source_url IS NULL OR source_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_provider_services_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_provider_services_verified_date
        CHECK (verification_status <> 'VERIFIED' OR last_verified_at IS NOT NULL),
    CONSTRAINT ck_provider_services_last_verified_not_future
        CHECK (last_verified_at IS NULL OR last_verified_at <= CURRENT_DATE)
);

CREATE UNIQUE INDEX ux_provider_services_provider_name_normalized
    ON provider_services (provider_id, lower(btrim(name)));
CREATE INDEX idx_provider_services_provider_id ON provider_services (provider_id);
CREATE INDEX idx_provider_services_public_lookup
    ON provider_services (service_type, active, verification_status, last_verified_at, name);

CREATE TABLE provider_service_locations (
    id UUID PRIMARY KEY,
    provider_service_id UUID NOT NULL,
    provider_location_id UUID NOT NULL,
    source_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(2048),
    verification_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    last_verified_at DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_service_locations_service_location
        UNIQUE (provider_service_id, provider_location_id),
    CONSTRAINT fk_provider_service_locations_service
        FOREIGN KEY (provider_service_id) REFERENCES provider_services (id) ON DELETE RESTRICT,
    CONSTRAINT fk_provider_service_locations_location
        FOREIGN KEY (provider_location_id) REFERENCES provider_locations (id) ON DELETE RESTRICT,
    CONSTRAINT ck_provider_service_locations_source_url_https
        CHECK (source_url IS NULL OR source_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_provider_service_locations_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_provider_service_locations_verified_date
        CHECK (verification_status <> 'VERIFIED' OR last_verified_at IS NOT NULL),
    CONSTRAINT ck_provider_service_locations_last_verified_not_future
        CHECK (last_verified_at IS NULL OR last_verified_at <= CURRENT_DATE)
);

CREATE OR REPLACE FUNCTION ensure_provider_service_location_same_provider()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM provider_services service
        JOIN provider_locations location ON location.provider_id = service.provider_id
        WHERE service.id = NEW.provider_service_id
          AND location.id = NEW.provider_location_id
    ) THEN
        RAISE EXCEPTION 'provider service and provider location must belong to the same provider';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_provider_service_locations_same_provider
    BEFORE INSERT OR UPDATE OF provider_service_id, provider_location_id
    ON provider_service_locations
    FOR EACH ROW
    EXECUTE FUNCTION ensure_provider_service_location_same_provider();

CREATE INDEX idx_provider_service_locations_service_id ON provider_service_locations (provider_service_id);
CREATE INDEX idx_provider_service_locations_location_id ON provider_service_locations (provider_location_id);
CREATE INDEX idx_provider_service_locations_public_lookup
    ON provider_service_locations (provider_location_id, active, verification_status, last_verified_at);

CREATE TABLE provider_service_categories (
    id UUID PRIMARY KEY,
    provider_service_id UUID NOT NULL,
    at_category_id UUID NOT NULL,
    rationale VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_service_categories_service_category
        UNIQUE (provider_service_id, at_category_id),
    CONSTRAINT fk_provider_service_categories_service
        FOREIGN KEY (provider_service_id) REFERENCES provider_services (id) ON DELETE RESTRICT,
    CONSTRAINT fk_provider_service_categories_at_category
        FOREIGN KEY (at_category_id) REFERENCES at_categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_provider_service_categories_service_id ON provider_service_categories (provider_service_id);
CREATE INDEX idx_provider_service_categories_at_category_id ON provider_service_categories (at_category_id);
CREATE INDEX idx_provider_service_categories_active_service
    ON provider_service_categories (active, provider_service_id);

CREATE TABLE provider_service_products (
    id UUID PRIMARY KEY,
    provider_service_id UUID NOT NULL,
    at_product_id UUID NOT NULL,
    rationale VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_service_products_service_product
        UNIQUE (provider_service_id, at_product_id),
    CONSTRAINT fk_provider_service_products_service
        FOREIGN KEY (provider_service_id) REFERENCES provider_services (id) ON DELETE RESTRICT,
    CONSTRAINT fk_provider_service_products_at_product
        FOREIGN KEY (at_product_id) REFERENCES at_products (id) ON DELETE RESTRICT
);

CREATE INDEX idx_provider_service_products_service_id ON provider_service_products (provider_service_id);
CREATE INDEX idx_provider_service_products_at_product_id ON provider_service_products (at_product_id);
CREATE INDEX idx_provider_service_products_active_service
    ON provider_service_products (active, provider_service_id);
