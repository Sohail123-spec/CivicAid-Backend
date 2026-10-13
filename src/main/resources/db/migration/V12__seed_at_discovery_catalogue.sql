
-- CivicAid V12: seed initial AT discovery categories.
-- V11 is already applied; do not edit historical migrations.
-- Seeded categories are intentionally UNVERIFIED.
-- Public discovery must continue enforcing verification/freshness rules.

INSERT INTO at_categories (
    id, code, name, description, display_order,
    source_name, source_url,
    verification_status, last_verified_at, active
)
VALUES
(
    '12000000-0000-0000-0000-000000000001',
    'VISION_ACCESS',
    'Vision and reading aids',
    'Assistive technology categories that may help people access printed, visual, or digital information.',
    10,
    'World Health Organization',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED', NULL, TRUE
),
(
    '12000000-0000-0000-0000-000000000002',
    'HEARING_ACCESS',
    'Hearing and listening aids',
    'Assistive technology categories that may help people access speech, audio, alerts, and environmental sounds.',
    20,
    'World Health Organization',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED', NULL, TRUE
),
(
    '12000000-0000-0000-0000-000000000003',
    'MOBILITY_AIDS',
    'Mobility aids',
    'Assistive technology categories that may support moving around, walking, balance, and transfers.',
    30,
    'World Health Organization',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED', NULL, TRUE
),
(
    '12000000-0000-0000-0000-000000000004',
    'COMMUNICATION_AIDS',
    'Communication aids',
    'Assistive technology categories that may support expressing needs, understanding communication, and participation.',
    40,
    'World Health Organization',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED', NULL, TRUE
),
(
    '12000000-0000-0000-0000-000000000005',
    'COGNITIVE_SUPPORT',
    'Cognition and learning supports',
    'Assistive technology categories that may help with memory, attention, understanding, and organizing information.',
    50,
    'World Health Organization',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED', NULL, TRUE
),
(
    '12000000-0000-0000-0000-000000000006',
    'SELF_CARE_AIDS',
    'Self-care aids',
    'Assistive technology categories that may support everyday personal care activities.',
    60,
    'World Health Organization',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED', NULL, TRUE
),
(
    '12000000-0000-0000-0000-000000000007',
    'ENVIRONMENTAL_ACCESS',
    'Environmental access aids',
    'Assistive technology categories that may help people interact with buildings, spaces, and everyday objects.',
    70,
    'World Health Organization',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED', NULL, TRUE
),
(
    '12000000-0000-0000-0000-000000000008',
    'DIGITAL_ACCESS',
    'Digital access aids',
    'Assistive technology categories that may support access to digital devices, content, and interfaces.',
    80,
    'World Health Organization',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED', NULL, TRUE
)
ON CONFLICT (code) DO NOTHING;

-- Deliberately do not seed active need-category mappings here.
-- Mappings must be grounded in reviewed relationships and should not
-- expose unverified categories as recommendations.
-- Concrete AT products will be added in a follow-up migration after
-- product-level source review.
