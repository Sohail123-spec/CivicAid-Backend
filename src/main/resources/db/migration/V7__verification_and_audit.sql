CREATE TABLE verification_records (
    id UUID PRIMARY KEY,
    verifier_user_id UUID NOT NULL,
    at_category_id UUID,
    at_product_id UUID,
    scheme_id UUID,
    provider_id UUID,
    provider_location_id UUID,
    provider_service_id UUID,
    provider_service_location_id UUID,
    scheme_product_id UUID,
    provider_service_product_id UUID,
    verification_result VARCHAR(16) NOT NULL,
    source_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(2048),
    source_version VARCHAR(100),
    review_notes VARCHAR(1000),
    performed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_records_verifier FOREIGN KEY (verifier_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_at_category FOREIGN KEY (at_category_id) REFERENCES at_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_at_product FOREIGN KEY (at_product_id) REFERENCES at_products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_scheme FOREIGN KEY (scheme_id) REFERENCES schemes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_provider FOREIGN KEY (provider_id) REFERENCES providers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_provider_location FOREIGN KEY (provider_location_id) REFERENCES provider_locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_provider_service FOREIGN KEY (provider_service_id) REFERENCES provider_services (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_provider_service_location FOREIGN KEY (provider_service_location_id) REFERENCES provider_service_locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_scheme_product FOREIGN KEY (scheme_product_id) REFERENCES scheme_products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_verification_records_provider_service_product FOREIGN KEY (provider_service_product_id) REFERENCES provider_service_products (id) ON DELETE RESTRICT,
    CONSTRAINT ck_verification_records_exactly_one_target CHECK (
        (CASE WHEN at_category_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN at_product_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN scheme_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN provider_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN provider_location_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN provider_service_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN provider_service_location_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN scheme_product_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN provider_service_product_id IS NULL THEN 0 ELSE 1 END) = 1
    ),
    CONSTRAINT ck_verification_records_result CHECK (verification_result IN ('VERIFIED', 'REJECTED', 'REVOKED')),
    CONSTRAINT ck_verification_records_review_notes CHECK (
        verification_result = 'VERIFIED' OR review_notes IS NOT NULL
    ),
    CONSTRAINT ck_verification_records_source_url_https
        CHECK (source_url IS NULL OR source_url ~* '^https://[^[:space:]]+$')
);

CREATE INDEX idx_verification_records_verifier_performed_at ON verification_records (verifier_user_id, performed_at DESC);
CREATE INDEX idx_verification_records_at_category_performed_at ON verification_records (at_category_id, performed_at DESC) WHERE at_category_id IS NOT NULL;
CREATE INDEX idx_verification_records_at_product_performed_at ON verification_records (at_product_id, performed_at DESC) WHERE at_product_id IS NOT NULL;
CREATE INDEX idx_verification_records_scheme_performed_at ON verification_records (scheme_id, performed_at DESC) WHERE scheme_id IS NOT NULL;
CREATE INDEX idx_verification_records_provider_performed_at ON verification_records (provider_id, performed_at DESC) WHERE provider_id IS NOT NULL;
CREATE INDEX idx_verification_records_provider_location_performed_at ON verification_records (provider_location_id, performed_at DESC) WHERE provider_location_id IS NOT NULL;
CREATE INDEX idx_verification_records_provider_service_performed_at ON verification_records (provider_service_id, performed_at DESC) WHERE provider_service_id IS NOT NULL;
CREATE INDEX idx_verification_records_provider_service_location_performed_at ON verification_records (provider_service_location_id, performed_at DESC) WHERE provider_service_location_id IS NOT NULL;
CREATE INDEX idx_verification_records_scheme_product_performed_at ON verification_records (scheme_product_id, performed_at DESC) WHERE scheme_product_id IS NOT NULL;
CREATE INDEX idx_verification_records_provider_service_product_performed_at ON verification_records (provider_service_product_id, performed_at DESC) WHERE provider_service_product_id IS NOT NULL;

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID NOT NULL,
    action_type VARCHAR(24) NOT NULL,
    changed_fields TEXT[],
    change_summary VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_audit_logs_target_type CHECK (target_type IN (
        'USER_ACCOUNT', 'FUNCTIONAL_NEED', 'AT_CATEGORY', 'AT_PRODUCT', 'NEED_CATEGORY_MAPPING',
        'SCHEME', 'SCHEME_CATEGORY', 'SCHEME_PRODUCT', 'PROVIDER', 'PROVIDER_LOCATION',
        'PROVIDER_SERVICE', 'PROVIDER_SERVICE_LOCATION', 'PROVIDER_SERVICE_CATEGORY',
        'PROVIDER_SERVICE_PRODUCT', 'VERIFICATION_RECORD')),
    CONSTRAINT ck_audit_logs_action_type CHECK (action_type IN (
        'CREATE', 'UPDATE', 'VERIFY', 'REJECT_VERIFICATION', 'REVOKE_VERIFICATION',
        'ACTIVATE', 'ARCHIVE', 'ROLE_ASSIGNED', 'ROLE_REMOVED')),
    CONSTRAINT ck_audit_logs_changed_fields_allowlist CHECK (
        changed_fields IS NULL OR changed_fields <@ ARRAY[
            'email', 'display_name', 'account_status', 'role', 'code', 'label', 'description',
            'parent_need_id', 'is_selectable', 'display_order', 'active', 'name', 'source_name',
            'source_url', 'verification_status', 'last_verified_at', 'considerations', 'authority_name',
            'benefits_summary', 'eligibility_summary', 'required_documents_summary', 'application_method',
            'application_instructions', 'official_source_url', 'official_application_url', 'jurisdiction_level',
            'applicable_state_code', 'source_version', 'effective_from', 'effective_to', 'rationale',
            'provider_type', 'website_url', 'address_line_1', 'address_line_2', 'locality', 'city',
            'state_code', 'postal_code', 'country_code', 'latitude', 'longitude', 'public_phone',
            'public_email', 'accessibility_notes', 'service_type', 'verification_result', 'review_notes'
        ]::TEXT[]
    )
);

CREATE INDEX idx_audit_logs_actor_occurred_at ON audit_logs (actor_user_id, occurred_at DESC);
CREATE INDEX idx_audit_logs_target_occurred_at ON audit_logs (target_type, target_id, occurred_at DESC);
CREATE INDEX idx_audit_logs_action_occurred_at ON audit_logs (action_type, occurred_at DESC);

CREATE OR REPLACE FUNCTION prevent_immutable_history_changes()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'immutable history records cannot be updated or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verification_records_immutable
    BEFORE UPDATE OR DELETE ON verification_records
    FOR EACH ROW EXECUTE FUNCTION prevent_immutable_history_changes();

CREATE TRIGGER trg_audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_immutable_history_changes();
