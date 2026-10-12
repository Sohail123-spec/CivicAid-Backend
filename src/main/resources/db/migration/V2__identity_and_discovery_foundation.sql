CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    account_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT ck_users_account_status CHECK (account_status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX ux_users_email_normalized ON users (lower(email));
CREATE INDEX idx_users_account_status ON users (account_status);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(16) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('USER', 'ADMIN'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE TABLE functional_needs (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    label VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    parent_need_id UUID,
    is_selectable BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_functional_needs_code UNIQUE (code),
    CONSTRAINT fk_functional_needs_parent
        FOREIGN KEY (parent_need_id) REFERENCES functional_needs (id) ON DELETE RESTRICT,
    CONSTRAINT ck_functional_needs_code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_functional_needs_parent_not_self CHECK (parent_need_id IS NULL OR parent_need_id <> id),
    CONSTRAINT ck_functional_needs_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_functional_needs_parent_id ON functional_needs (parent_need_id);
CREATE INDEX idx_functional_needs_public_lookup
    ON functional_needs (active, is_selectable, display_order, label);

CREATE TABLE at_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(750),
    display_order INTEGER NOT NULL DEFAULT 0,
    source_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(2048),
    verification_status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
    last_verified_at DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_at_categories_code UNIQUE (code),
    CONSTRAINT ck_at_categories_code CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_at_categories_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_at_categories_source_url_https
        CHECK (source_url IS NULL OR source_url ~* '^https://[^[:space:]]+$'),
    CONSTRAINT ck_at_categories_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_at_categories_verified_date
        CHECK (verification_status <> 'VERIFIED' OR last_verified_at IS NOT NULL),
    CONSTRAINT ck_at_categories_last_verified_not_future
        CHECK (last_verified_at IS NULL OR last_verified_at <= CURRENT_DATE)
);

CREATE UNIQUE INDEX ux_at_categories_name_normalized ON at_categories (lower(btrim(name)));
CREATE INDEX idx_at_categories_public_lookup
    ON at_categories (active, verification_status, last_verified_at, display_order, name);
