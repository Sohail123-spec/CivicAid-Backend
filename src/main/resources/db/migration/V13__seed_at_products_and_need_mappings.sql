
-- CivicAid V13: initial AT product types and functional-need mappings.
-- V11 and V12 are historical migrations: do not modify them.
-- Products remain UNVERIFIED until an administrator reviews the
-- specific product information and its authoritative source.
-- No prices, brands, or unsupported eligibility claims are seeded.

-- ============================================================
-- 1. Seed generic assistive-technology product types
-- ============================================================

INSERT INTO public.at_products (
    id, at_category_id, code, name, description, considerations,
    display_order, source_name, source_url,
    verification_status, last_verified_at, active
)
SELECT
    p.id::uuid,
    c.id,
    p.code,
    p.name,
    p.description,
    p.considerations,
    p.display_order,
    'World Health Organization - Assistive Technology',
    'https://www.who.int/health-topics/assistive-technology',
    'UNVERIFIED',
    NULL,
    TRUE
FROM (
    VALUES
    (
        '13000000-0000-0000-0000-000000000001',
        'VISION_MAGNIFIER',
        'VISION_ACCESS',
        'Optical or electronic magnifier',
        'A device that enlarges printed text or other visual details.',
        'Magnification, lighting, portability, and compatibility with the intended task vary by device.',
        10
    ),
    (
        '13000000-0000-0000-0000-000000000002',
        'VISION_WHITE_CANE',
        'VISION_ACCESS',
        'White mobility cane',
        'A mobility aid used by some people with vision impairment to detect obstacles and navigate.',
        'Correct length, technique, training, and suitability for the user and environment matter.',
        20
    ),
    (
        '13000000-0000-0000-0000-000000000003',
        'HEARING_HEARING_AID',
        'HEARING_ACCESS',
        'Hearing aid',
        'An electronic device intended to make sound more accessible to a person with hearing loss.',
        'Suitability, fitting, maintenance, and access to professional support vary by device and user.',
        10
    ),
    (
        '13000000-0000-0000-0000-000000000004',
        'MOBILITY_MANUAL_WHEELCHAIR',
        'MOBILITY_AIDS',
        'Manual wheelchair',
        'A wheeled mobility device propelled by the user or an attendant.',
        'Fit, posture, propulsion needs, terrain, transport, and maintenance should be assessed.',
        10
    ),
    (
        '13000000-0000-0000-0000-000000000005',
        'MOBILITY_WALKING_AID',
        'MOBILITY_AIDS',
        'Walking aid',
        'A mobility aid such as a cane, crutch, or walker used to support walking.',
        'The type and fit should suit the user’s balance, strength, environment, and mobility needs.',
        20
    ),
    (
        '13000000-0000-0000-0000-000000000006',
        'COMMUNICATION_AAC_BOARD',
        'COMMUNICATION_AIDS',
        'Augmentative and alternative communication board',
        'A board using symbols, pictures, letters, or words to support communication.',
        'Vocabulary, language, vision, motor access, and the user’s communication preferences matter.',
        10
    ),
    (
        '13000000-0000-0000-0000-000000000007',
        'SELF_CARE_ADAPTED_UTENSILS',
        'SELF_CARE_AIDS',
        'Adapted eating utensils',
        'Utensils designed to support grasping or eating for people with particular functional needs.',
        'Handle size, grip, weight, hygiene, and the user’s eating needs should be considered.',
        10
    ),
    (
        '13000000-0000-0000-0000-000000000008',
        'DIGITAL_SCREEN_READER',
        'DIGITAL_ACCESS',
        'Screen-reading software',
        'Software that presents on-screen content through speech or other non-visual output.',
        'Operating-system compatibility, language support, application accessibility, and user preference matter.',
        10
    )
) AS p(id, code, category_code, name, description, considerations, display_order)
JOIN public.at_categories c ON c.code = p.category_code
ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- 2. Seed reviewed-candidate need-to-category relationships
--    Mappings are active but public discovery remains gated by
--    category/product verification and freshness rules.
-- ============================================================

INSERT INTO public.need_category_mappings (
    id, functional_need_id, at_category_id, rationale, active
)
SELECT
    m.id::uuid,
    fn.id,
    c.id,
    m.rationale,
    TRUE
FROM (
    VALUES
    (
        '13000000-0000-0000-0001-000000000001',
        'VISION_READING_PRINTED',
        'VISION_ACCESS',
        'Magnification may help some users access printed information.'
    ),
    (
        '13000000-0000-0000-0001-000000000002',
        'VISION_NAVIGATING_VISUAL_INFORMATION',
        'VISION_ACCESS',
        'Vision-related assistive technology may support access to visual information and navigation.'
    ),
    (
        '13000000-0000-0000-0001-000000000003',
        'HEARING_SPEECH',
        'HEARING_ACCESS',
        'Hearing-related assistive technology may help some users access spoken information.'
    ),
    (
        '13000000-0000-0000-0001-000000000004',
        'HEARING_ALARMS_SOUNDS',
        'HEARING_ACCESS',
        'Some hearing technologies support access to sounds and alerts; device suitability varies.'
    ),
    (
        '13000000-0000-0000-0001-000000000005',
        'MOBILITY_WALKING_MOVING',
        'MOBILITY_AIDS',
        'Mobility aids may support walking or moving, depending on the user’s assessed needs.'
    ),
    (
        '13000000-0000-0000-0001-000000000006',
        'MOBILITY_TRANSFERRING',
        'MOBILITY_AIDS',
        'Some mobility equipment may assist transfers when appropriately selected and fitted.'
    ),
    (
        '13000000-0000-0000-0001-000000000007',
        'COMMUNICATION_WITHOUT_SPEECH',
        'COMMUNICATION_AIDS',
        'AAC tools may provide alternative ways to express messages.'
    ),
    (
        '13000000-0000-0000-0001-000000000008',
        'SELF_CARE_EATING_DRINKING',
        'SELF_CARE_AIDS',
        'Adapted utensils may support eating tasks for some users.'
    ),
    (
        '13000000-0000-0000-0001-000000000009',
        'DIGITAL_READING_CONTENT',
        'DIGITAL_ACCESS',
        'Screen-reading tools may provide non-visual access to compatible digital content.'
    ),
    (
        '13000000-0000-0000-0001-000000000010',
        'DIGITAL_INTERFACES',
        'DIGITAL_ACCESS',
        'Assistive software may improve access to compatible digital interfaces.'
    )
) AS m(id, need_code, category_code, rationale)
JOIN public.functional_needs fn ON fn.code = m.need_code
JOIN public.at_categories c ON c.code = m.category_code
ON CONFLICT (functional_need_id, at_category_id) DO NOTHING;
