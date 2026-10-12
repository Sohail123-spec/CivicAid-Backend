CREATE TABLE saved_resources (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    at_category_id UUID,
    at_product_id UUID,
    scheme_id UUID,
    provider_id UUID,
    provider_location_id UUID,
    provider_service_location_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saved_resources_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_resources_at_category FOREIGN KEY (at_category_id) REFERENCES at_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_saved_resources_at_product FOREIGN KEY (at_product_id) REFERENCES at_products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_saved_resources_scheme FOREIGN KEY (scheme_id) REFERENCES schemes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_saved_resources_provider FOREIGN KEY (provider_id) REFERENCES providers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_saved_resources_provider_location FOREIGN KEY (provider_location_id) REFERENCES provider_locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_saved_resources_provider_service_location FOREIGN KEY (provider_service_location_id) REFERENCES provider_service_locations (id) ON DELETE RESTRICT,
    CONSTRAINT ck_saved_resources_exactly_one_target CHECK (
        (CASE WHEN at_category_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN at_product_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN scheme_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN provider_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN provider_location_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN provider_service_location_id IS NULL THEN 0 ELSE 1 END) = 1
    )
);

CREATE UNIQUE INDEX ux_saved_resources_user_at_category ON saved_resources (user_id, at_category_id) WHERE at_category_id IS NOT NULL;
CREATE UNIQUE INDEX ux_saved_resources_user_at_product ON saved_resources (user_id, at_product_id) WHERE at_product_id IS NOT NULL;
CREATE UNIQUE INDEX ux_saved_resources_user_scheme ON saved_resources (user_id, scheme_id) WHERE scheme_id IS NOT NULL;
CREATE UNIQUE INDEX ux_saved_resources_user_provider ON saved_resources (user_id, provider_id) WHERE provider_id IS NOT NULL;
CREATE UNIQUE INDEX ux_saved_resources_user_provider_location ON saved_resources (user_id, provider_location_id) WHERE provider_location_id IS NOT NULL;
CREATE UNIQUE INDEX ux_saved_resources_user_provider_service_location ON saved_resources (user_id, provider_service_location_id) WHERE provider_service_location_id IS NOT NULL;
CREATE INDEX idx_saved_resources_user_created_at ON saved_resources (user_id, created_at DESC);

CREATE TABLE lifecycle_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    at_product_id UUID,
    at_category_id UUID,
    acquired_on DATE NOT NULL,
    acquisition_method VARCHAR(16) NOT NULL,
    condition_at_acquisition VARCHAR(16) NOT NULL,
    lifecycle_status VARCHAR(24) NOT NULL,
    warranty_expires_on DATE,
    preferred_provider_service_location_id UUID,
    replacement_due_on DATE,
    reassessment_due_on DATE,
    replaces_lifecycle_record_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lifecycle_records_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_lifecycle_records_at_product FOREIGN KEY (at_product_id) REFERENCES at_products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lifecycle_records_at_category FOREIGN KEY (at_category_id) REFERENCES at_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lifecycle_records_preferred_service_location FOREIGN KEY (preferred_provider_service_location_id) REFERENCES provider_service_locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lifecycle_records_replaces FOREIGN KEY (replaces_lifecycle_record_id) REFERENCES lifecycle_records (id) ON DELETE RESTRICT,
    CONSTRAINT ck_lifecycle_records_catalog_reference CHECK (at_product_id IS NULL OR at_category_id IS NULL),
    CONSTRAINT ck_lifecycle_records_acquired_on_not_future CHECK (acquired_on <= CURRENT_DATE),
    CONSTRAINT ck_lifecycle_records_acquisition_method CHECK (acquisition_method IN ('PURCHASED', 'GIFTED', 'TRANSFERRED', 'OTHER')),
    CONSTRAINT ck_lifecycle_records_condition_at_acquisition CHECK (condition_at_acquisition IN ('NEW', 'REFURBISHED', 'SECOND_HAND', 'UNKNOWN')),
    CONSTRAINT ck_lifecycle_records_status CHECK (lifecycle_status IN ('IN_USE', 'NEEDS_MAINTENANCE', 'UNDER_REPAIR', 'NEEDS_REASSESSMENT', 'REPLACED', 'DISPOSED', 'LOST', 'TRANSFERRED')),
    CONSTRAINT ck_lifecycle_records_not_self_replacement CHECK (replaces_lifecycle_record_id IS NULL OR replaces_lifecycle_record_id <> id)
);

CREATE UNIQUE INDEX ux_lifecycle_records_direct_replacement
    ON lifecycle_records (replaces_lifecycle_record_id) WHERE replaces_lifecycle_record_id IS NOT NULL;
CREATE INDEX idx_lifecycle_records_user_status ON lifecycle_records (user_id, lifecycle_status);
CREATE INDEX idx_lifecycle_records_at_product_id ON lifecycle_records (at_product_id);
CREATE INDEX idx_lifecycle_records_at_category_id ON lifecycle_records (at_category_id);
CREATE INDEX idx_lifecycle_records_preferred_service_location_id ON lifecycle_records (preferred_provider_service_location_id);

CREATE OR REPLACE FUNCTION ensure_lifecycle_replacement_valid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.replaces_lifecycle_record_id IS NULL THEN
        RETURN NEW;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM lifecycle_records predecessor
        WHERE predecessor.id = NEW.replaces_lifecycle_record_id
          AND predecessor.user_id = NEW.user_id
    ) THEN
        RAISE EXCEPTION 'replacement record must reference a lifecycle record owned by the same user';
    END IF;
    IF EXISTS (
        WITH RECURSIVE replacement_chain AS (
            SELECT id, replaces_lifecycle_record_id FROM lifecycle_records WHERE id = NEW.replaces_lifecycle_record_id
            UNION ALL
            SELECT record.id, record.replaces_lifecycle_record_id
            FROM lifecycle_records record
            JOIN replacement_chain chain ON record.id = chain.replaces_lifecycle_record_id
        ) SELECT 1 FROM replacement_chain WHERE id = NEW.id
    ) THEN
        RAISE EXCEPTION 'lifecycle replacement relationships cannot form a cycle';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lifecycle_records_replacement_valid
    BEFORE INSERT OR UPDATE OF user_id, replaces_lifecycle_record_id ON lifecycle_records
    FOR EACH ROW EXECUTE FUNCTION ensure_lifecycle_replacement_valid();

CREATE TABLE maintenance_records (
    id UUID PRIMARY KEY,
    lifecycle_record_id UUID NOT NULL,
    maintenance_type VARCHAR(16) NOT NULL,
    maintenance_date DATE NOT NULL,
    provider_service_location_id UUID,
    details VARCHAR(1000),
    next_maintenance_due_on DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_maintenance_records_lifecycle_record FOREIGN KEY (lifecycle_record_id) REFERENCES lifecycle_records (id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_records_provider_service_location FOREIGN KEY (provider_service_location_id) REFERENCES provider_service_locations (id) ON DELETE RESTRICT,
    CONSTRAINT ck_maintenance_records_type CHECK (maintenance_type IN ('ROUTINE', 'PREVENTIVE', 'CLEANING', 'ADJUSTMENT', 'OTHER')),
    CONSTRAINT ck_maintenance_records_next_due CHECK (next_maintenance_due_on IS NULL OR next_maintenance_due_on >= maintenance_date)
);

CREATE OR REPLACE FUNCTION ensure_maintenance_record_dates_valid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.maintenance_date > CURRENT_DATE OR NOT EXISTS (
        SELECT 1 FROM lifecycle_records record
        WHERE record.id = NEW.lifecycle_record_id AND NEW.maintenance_date >= record.acquired_on
    ) THEN
        RAISE EXCEPTION 'maintenance date must be on or after acquisition and cannot be in the future';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_maintenance_records_dates_valid
    BEFORE INSERT OR UPDATE OF lifecycle_record_id, maintenance_date ON maintenance_records
    FOR EACH ROW EXECUTE FUNCTION ensure_maintenance_record_dates_valid();

CREATE INDEX idx_maintenance_records_lifecycle_date ON maintenance_records (lifecycle_record_id, maintenance_date DESC);
CREATE INDEX idx_maintenance_records_provider_service_location_id ON maintenance_records (provider_service_location_id);

CREATE TABLE repair_records (
    id UUID PRIMARY KEY,
    lifecycle_record_id UUID NOT NULL,
    repair_date DATE NOT NULL,
    provider_service_location_id UUID,
    issue_description VARCHAR(1000) NOT NULL,
    repair_outcome_status VARCHAR(24) NOT NULL,
    work_summary VARCHAR(1000),
    cost_inr NUMERIC(12,2),
    warranty_used BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repair_records_lifecycle_record FOREIGN KEY (lifecycle_record_id) REFERENCES lifecycle_records (id) ON DELETE CASCADE,
    CONSTRAINT fk_repair_records_provider_service_location FOREIGN KEY (provider_service_location_id) REFERENCES provider_service_locations (id) ON DELETE RESTRICT,
    CONSTRAINT ck_repair_records_outcome CHECK (repair_outcome_status IN ('RESOLVED', 'PARTIALLY_RESOLVED', 'UNRESOLVED', 'REPLACEMENT_CONSIDERED')),
    CONSTRAINT ck_repair_records_cost_non_negative CHECK (cost_inr IS NULL OR cost_inr >= 0)
);

CREATE OR REPLACE FUNCTION ensure_repair_record_date_valid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.repair_date > CURRENT_DATE OR NOT EXISTS (
        SELECT 1 FROM lifecycle_records record
        WHERE record.id = NEW.lifecycle_record_id AND NEW.repair_date >= record.acquired_on
    ) THEN
        RAISE EXCEPTION 'repair date must be on or after acquisition and cannot be in the future';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_repair_records_date_valid
    BEFORE INSERT OR UPDATE OF lifecycle_record_id, repair_date ON repair_records
    FOR EACH ROW EXECUTE FUNCTION ensure_repair_record_date_valid();

CREATE INDEX idx_repair_records_lifecycle_date ON repair_records (lifecycle_record_id, repair_date DESC);
CREATE INDEX idx_repair_records_provider_service_location_id ON repair_records (provider_service_location_id);
