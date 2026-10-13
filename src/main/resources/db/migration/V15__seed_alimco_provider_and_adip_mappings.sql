-- CivicAid V15: Seed ALIMCO provider, location, services, and map covered categories/products to ADIP scheme.
-- Do not modify historical migrations V1 - V14.
-- Preserves existing categories, products, and ADIP scheme if already created or verified by admin.

-- ============================================================
-- 1. Seed ALIMCO Provider (Artificial Limbs Manufacturing Corp of India)
-- ============================================================

INSERT INTO providers (
    id, code, name, provider_type, description, website_url,
    source_name, source_url, verification_status, last_verified_at, active
)
VALUES (
    '15000000-0000-0000-0000-000000000001',
    'ALIMCO',
    'Artificial Limbs Manufacturing Corporation of India',
    'AT_SUPPLIER_VENDOR',
    'Artificial Limbs Manufacturing Corporation of India (ALIMCO) is a Central Public Sector Enterprise operating under the Department of Empowerment of Persons with Disabilities, Ministry of Social Justice and Empowerment, Government of India. ALIMCO manufactures and supplies high-quality assistive devices, prosthetics, and orthotics for persons with disabilities across India.',
    'https://alimco.in',
    'ALIMCO Official Portal',
    'https://alimco.in',
    'VERIFIED',
    CURRENT_DATE,
    TRUE
)
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 2. Seed ALIMCO Headquarters Location (Kanpur, Uttar Pradesh)
-- ============================================================

INSERT INTO provider_locations (
    id, provider_id, address_line_1, locality, city, state_code,
    postal_code, country_code, latitude, longitude,
    public_phone, public_email, accessibility_notes,
    source_name, source_url, verification_status, last_verified_at, active
)
SELECT
    '15000000-0000-0000-0001-000000000001'::uuid,
    p.id,
    'G.T. Road, Naramau',
    'Naramau',
    'Kanpur',
    'UP',
    '209217',
    'IN',
    26.516700,
    80.233300,
    '+91-512-2770172',
    'alimco@alimco.in',
    'Wheelchair accessible campus and evaluation centre with dedicated assistive device fitting facilities.',
    'ALIMCO Official Contact Directory',
    'https://alimco.in',
    'VERIFIED',
    CURRENT_DATE,
    TRUE
FROM providers p
WHERE p.code = 'ALIMCO'
  AND NOT EXISTS (
      SELECT 1 FROM provider_locations pl WHERE pl.id = '15000000-0000-0000-0001-000000000001'::uuid
  );

-- ============================================================
-- 3. Seed ALIMCO Services
-- ============================================================

INSERT INTO provider_services (
    id, provider_id, code, name, service_type, description,
    source_name, source_url, verification_status, last_verified_at, active
)
SELECT
    s.id::uuid,
    p.id,
    s.code,
    s.name,
    s.service_type,
    s.description,
    'ALIMCO Services Directory',
    'https://alimco.in',
    'VERIFIED',
    CURRENT_DATE,
    TRUE
FROM providers p
CROSS JOIN (
    VALUES
    (
        '15000000-0000-0000-0002-000000000001',
        'ALIMCO_ASSESSMENT_FITTING',
        'Prosthetic, Orthotic and Assistive Device Assessment and Fitting',
        'ASSESSMENT_AND_FITTING',
        'Clinical assessment, measurement, fabrication, trial, and fitting services for prosthetic limbs, orthotic supports, and assistive appliances.'
    ),
    (
        '15000000-0000-0000-0002-000000000002',
        'ALIMCO_AT_SUPPLY',
        'Assistive Aids and Appliances Supply and Distribution',
        'AT_SUPPLY_OR_DISPENSING',
        'Dispensing and supply of certified assistive technology aids including wheelchairs, tricycles, walking frames, sensory aids, and daily living appliances.'
    ),
    (
        '15000000-0000-0000-0002-000000000003',
        'ALIMCO_DEVICE_REPAIR',
        'Assistive Device Repair and Maintenance Support',
        'REPAIR',
        'Technical maintenance, replacement parts fitting, and repair services for distributed assistive aids and prosthetic devices.'
    )
) AS s(id, code, name, service_type, description)
WHERE p.code = 'ALIMCO'
  AND NOT EXISTS (
      SELECT 1 FROM provider_services ps WHERE ps.provider_id = p.id AND ps.code = s.code
  );

-- ============================================================
-- 4. Seed Provider Service Locations
-- ============================================================

INSERT INTO provider_service_locations (
    id, provider_service_id, provider_location_id,
    source_name, source_url, verification_status, last_verified_at, active
)
SELECT
    sl.id::uuid,
    ps.id,
    pl.id,
    'ALIMCO Services Directory',
    'https://alimco.in',
    'VERIFIED',
    CURRENT_DATE,
    TRUE
FROM providers p
JOIN provider_locations pl ON pl.provider_id = p.id AND pl.id = '15000000-0000-0000-0001-000000000001'::uuid
JOIN provider_services ps ON ps.provider_id = p.id
CROSS JOIN (
    VALUES
    ('15000000-0000-0000-0003-000000000001', 'ALIMCO_ASSESSMENT_FITTING'),
    ('15000000-0000-0000-0003-000000000002', 'ALIMCO_AT_SUPPLY'),
    ('15000000-0000-0000-0003-000000000003', 'ALIMCO_DEVICE_REPAIR')
) AS sl(id, service_code)
WHERE p.code = 'ALIMCO'
  AND ps.code = sl.service_code
  AND NOT EXISTS (
      SELECT 1 FROM provider_service_locations psl
      WHERE psl.provider_service_id = ps.id AND psl.provider_location_id = pl.id
  );

-- ============================================================
-- 5. Map Covered Categories to Existing ADIP Scheme
-- ============================================================

INSERT INTO scheme_categories (
    id, scheme_id, at_category_id, rationale, active
)
SELECT
    m.id::uuid,
    s.id,
    c.id,
    m.rationale,
    TRUE
FROM schemes s
CROSS JOIN (
    VALUES
    (
        '15000000-0000-0000-0004-000000000001',
        'MOBILITY_AIDS',
        'Covered under ADIP guidelines for mobility appliances including wheelchairs, tricycles, walkers, crutches, and prosthetics.'
    ),
    (
        '15000000-0000-0000-0004-000000000002',
        'VISION_ACCESS',
        'Covered under ADIP guidelines for visual aids including smart canes, braille kits, and magnifiers.'
    ),
    (
        '15000000-0000-0000-0004-000000000003',
        'HEARING_ACCESS',
        'Covered under ADIP guidelines for hearing aids and assistive listening devices.'
    ),
    (
        '15000000-0000-0000-0004-000000000004',
        'COMMUNICATION_AIDS',
        'Covered under ADIP guidelines for communication and educational assistive aids.'
    )
) AS m(id, category_code, rationale)
JOIN at_categories c ON c.code = m.category_code
WHERE s.code = 'ADIP'
  AND NOT EXISTS (
      SELECT 1 FROM scheme_categories sc
      WHERE sc.scheme_id = s.id AND sc.at_category_id = c.id
  );

-- ============================================================
-- 6. Map Eligible Products to Existing ADIP Scheme
-- ============================================================

INSERT INTO scheme_products (
    id, scheme_id, at_product_id, rationale, active
)
SELECT
    m.id::uuid,
    s.id,
    p.id,
    m.rationale,
    TRUE
FROM schemes s
CROSS JOIN (
    VALUES
    (
        '15000000-0000-0000-0005-000000000001',
        'MOBILITY_MANUAL_WHEELCHAIR',
        'Standard and rough-terrain manual wheelchairs distributed to eligible beneficiaries with locomotor disability under ADIP.'
    ),
    (
        '15000000-0000-0000-0005-000000000002',
        'MOBILITY_WALKING_AID',
        'Crutches, walking frames, and walking sticks distributed for ambulatory assistance under ADIP.'
    ),
    (
        '15000000-0000-0000-0005-000000000003',
        'VISION_WHITE_CANE',
        'White canes and smart canes provided to visually impaired beneficiaries under ADIP.'
    ),
    (
        '15000000-0000-0000-0005-000000000004',
        'VISION_MAGNIFIER',
        'Optical and electronic magnifiers provided for low-vision beneficiaries under ADIP.'
    ),
    (
        '15000000-0000-0000-0005-000000000005',
        'HEARING_HEARING_AID',
        'Behind-the-ear digital hearing aids provided to hearing impaired beneficiaries under ADIP.'
    ),
    (
        '15000000-0000-0000-0005-000000000006',
        'COMMUNICATION_AAC_BOARD',
        'Augmentative communication boards and teaching-learning materials distributed under ADIP.'
    )
) AS m(id, product_code, rationale)
JOIN at_products p ON p.code = m.product_code
WHERE s.code = 'ADIP'
  AND NOT EXISTS (
      SELECT 1 FROM scheme_products sp
      WHERE sp.scheme_id = s.id AND sp.at_product_id = p.id
  );
