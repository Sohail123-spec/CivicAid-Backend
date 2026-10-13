
ALTER TABLE audit_logs
    DROP CONSTRAINT ck_audit_logs_changed_fields_allowlist;

ALTER TABLE audit_logs
    ADD CONSTRAINT ck_audit_logs_changed_fields_allowlist
    CHECK (
        changed_fields IS NULL
        OR changed_fields <@ ARRAY[
            'email', 'display_name', 'account_status', 'role', 'code', 'label', 'description',
            'parent_need_id', 'is_selectable', 'display_order', 'active', 'name', 'source_name',
            'source_url', 'verification_status', 'last_verified_at', 'considerations', 'authority_name',
            'benefits_summary', 'eligibility_summary', 'required_documents_summary', 'application_method',
            'application_instructions', 'official_source_url', 'official_application_url', 'jurisdiction_level',
            'applicable_state_code', 'source_version', 'effective_from', 'effective_to', 'rationale',
            'provider_type', 'website_url', 'address_line_1', 'address_line_2', 'locality', 'city',
            'state_code', 'postal_code', 'country_code', 'latitude', 'longitude', 'public_phone',
            'public_email', 'accessibility_notes', 'service_type', 'verification_result', 'review_notes',
            'verification_source_version'
        ]::TEXT[]
    );
