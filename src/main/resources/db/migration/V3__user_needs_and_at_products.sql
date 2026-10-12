CREATE TABLE user_functional_needs (
    user_id UUID NOT NULL,
    functional_need_id UUID NOT NULL,
    PRIMARY KEY (user_id, functional_need_id),
    CONSTRAINT fk_user_functional_needs_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_functional_needs_functional_need
        FOREIGN KEY (functional_need_id) REFERENCES functional_needs (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_functional_needs_functional_need_id
    ON user_functional_needs (functional_need_id);

CREATE TABLE need_category_mappings (
    id UUID PRIMARY KEY,
    functional_need_id UUID NOT NULL,
    at_category_id UUID NOT NULL,
    rationale VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_need_category_mappings_need_category
        UNIQUE (functional_need_id, at_category_id),
    CONSTRAINT fk_need_category_mappings_functional_need
        FOREIGN KEY (functional_need_id) REFERENCES functional_needs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_need_category_mappings_at_category
        FOREIGN KEY (at_category_id) REFERENCES at_categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_need_category_mappings_functional_need_id
    ON need_category_mappings (functional_need_id);
CREATE INDEX idx_need_category_mappings_at_category_id
    ON need_category_mappings (at_category_id);
CREATE INDEX idx_need_category_mappings_active_need
    ON need_category_mappings (active, functional_need_id);

CREATE TABLE at_products (
    id UUID PRIMARY KEY,
    at_category_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    considerations VARCHAR(1000),
    display_order INTEGER NOT NULL DEFAULT 0,
    source_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(2048),
    verification_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    last_verified_at DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_at_products_code UNIQUE (code),
    CONSTRAINT fk_at_products_at_category
        FOREIGN KEY (at_category_id) REFERENCES at_categories (id) ON DELETE RESTRICT,
    CONSTRAINT ck_at_products_code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_at_products_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_at_products_source_url_https
        CHECK (source_url IS NULL OR source_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_at_products_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_at_products_verified_date
        CHECK (verification_status <> 'VERIFIED' OR last_verified_at IS NOT NULL),
    CONSTRAINT ck_at_products_last_verified_not_future
        CHECK (last_verified_at IS NULL OR last_verified_at <= CURRENT_DATE)
);

CREATE UNIQUE INDEX ux_at_products_category_name_normalized
    ON at_products (at_category_id, lower(btrim(name)));
CREATE INDEX idx_at_products_public_lookup
    ON at_products (at_category_id, active, verification_status, last_verified_at, display_order, name);
