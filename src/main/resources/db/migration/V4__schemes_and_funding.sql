CREATE TABLE schemes (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    authority_name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    benefits_summary TEXT NOT NULL,
    eligibility_summary TEXT NOT NULL,
    required_documents_summary TEXT,
    application_method VARCHAR(16) NOT NULL,
    application_instructions TEXT NOT NULL,
    official_source_url VARCHAR(2048) NOT NULL,
    official_application_url VARCHAR(2048),
    jurisdiction_level VARCHAR(16) NOT NULL,
    applicable_state_code CHAR(2),
    source_version VARCHAR(100),
    effective_from DATE,
    effective_to DATE,
    verification_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    last_verified_at DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_schemes_code UNIQUE (code),
    CONSTRAINT ck_schemes_code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_schemes_application_method
        CHECK (application_method IN ('ONLINE', 'OFFLINE', 'BOTH', 'OTHER')),
    CONSTRAINT ck_schemes_jurisdiction_level
        CHECK (jurisdiction_level IN ('NATIONAL', 'STATE')),
    CONSTRAINT ck_schemes_state_scope
        CHECK ((jurisdiction_level = 'NATIONAL' AND applicable_state_code IS NULL)
            OR (jurisdiction_level = 'STATE' AND applicable_state_code ~ '^[A-Z]{2}$')),
    CONSTRAINT ck_schemes_effective_period
        CHECK (effective_from IS NULL OR effective_to IS NULL OR effective_from <= effective_to),
    CONSTRAINT ck_schemes_official_source_url_https
        CHECK (official_source_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_schemes_official_application_url_https
        CHECK (official_application_url IS NULL OR official_application_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_schemes_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_schemes_verified_date
        CHECK (verification_status <> 'VERIFIED' OR last_verified_at IS NOT NULL),
    CONSTRAINT ck_schemes_last_verified_not_future
        CHECK (last_verified_at IS NULL OR last_verified_at <= CURRENT_DATE)
);

CREATE UNIQUE INDEX ux_schemes_name_jurisdiction_state_normalized
    ON schemes (jurisdiction_level, COALESCE(applicable_state_code, ''), lower(btrim(name)));
CREATE INDEX idx_schemes_public_lookup
    ON schemes (jurisdiction_level, applicable_state_code, active, verification_status, last_verified_at, name);

CREATE TABLE scheme_categories (
    id UUID PRIMARY KEY,
    scheme_id UUID NOT NULL,
    at_category_id UUID NOT NULL,
    rationale VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_scheme_categories_scheme_category UNIQUE (scheme_id, at_category_id),
    CONSTRAINT fk_scheme_categories_scheme
        FOREIGN KEY (scheme_id) REFERENCES schemes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_scheme_categories_at_category
        FOREIGN KEY (at_category_id) REFERENCES at_categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_scheme_categories_scheme_id ON scheme_categories (scheme_id);
CREATE INDEX idx_scheme_categories_at_category_id ON scheme_categories (at_category_id);
CREATE INDEX idx_scheme_categories_active_scheme ON scheme_categories (active, scheme_id);

CREATE TABLE scheme_products (
    id UUID PRIMARY KEY,
    scheme_id UUID NOT NULL,
    at_product_id UUID NOT NULL,
    rationale VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_scheme_products_scheme_product UNIQUE (scheme_id, at_product_id),
    CONSTRAINT fk_scheme_products_scheme
        FOREIGN KEY (scheme_id) REFERENCES schemes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_scheme_products_at_product
        FOREIGN KEY (at_product_id) REFERENCES at_products (id) ON DELETE RESTRICT
);

CREATE INDEX idx_scheme_products_scheme_id ON scheme_products (scheme_id);
CREATE INDEX idx_scheme_products_at_product_id ON scheme_products (at_product_id);
CREATE INDEX idx_scheme_products_active_scheme ON scheme_products (active, scheme_id);
