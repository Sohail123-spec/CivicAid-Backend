-- CivicAid Functional Need seed data
-- Parents are non-selectable.
-- Child needs are selectable by users.
-- The vocabulary describes functional difficulties, not diagnoses.

-- ============================================================
-- PARENT CATEGORIES
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000001',
     'VISION',
     'Vision',
     'Difficulty with seeing, reading, or accessing visual information.',
     NULL,
     FALSE,
     10,
     TRUE),

    ('00000000-0000-0000-0000-000000000002',
     'HEARING',
     'Hearing',
     'Difficulty with hearing or accessing spoken or audible information.',
     NULL,
     FALSE,
     20,
     TRUE),

    ('00000000-0000-0000-0000-000000000003',
     'MOBILITY',
     'Mobility',
     'Difficulty with moving, walking, maintaining balance, or using the arms and hands.',
     NULL,
     FALSE,
     30,
     TRUE),

    ('00000000-0000-0000-0000-000000000004',
     'COMMUNICATION',
     'Communication',
     'Difficulty expressing, understanding, or participating in communication.',
     NULL,
     FALSE,
     40,
     TRUE),

    ('00000000-0000-0000-0000-000000000005',
     'COGNITION_LEARNING',
     'Cognition & Learning',
     'Difficulty with memory, attention, understanding, processing, or organizing information.',
     NULL,
     FALSE,
     50,
     TRUE),

    ('00000000-0000-0000-0000-000000000006',
     'SELF_CARE',
     'Self-care',
     'Difficulty performing everyday personal care activities independently.',
     NULL,
     FALSE,
     60,
     TRUE),

    ('00000000-0000-0000-0000-000000000007',
     'ENVIRONMENTAL_ACCESS',
     'Environmental Access',
     'Difficulty accessing or interacting with buildings, spaces, objects, or public facilities.',
     NULL,
     FALSE,
     70,
     TRUE),

    ('00000000-0000-0000-0000-000000000008',
     'DIGITAL_ACCESS',
     'Digital Access',
     'Difficulty accessing or interacting with digital devices, content, or interfaces.',
     NULL,
     FALSE,
     80,
     TRUE);


-- ============================================================
-- VISION
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000101',
     'VISION_READING_PRINTED',
     'Reading printed information',
     'Difficulty reading books, documents, labels, signs, or other printed information.',
     '00000000-0000-0000-0000-000000000001',
     TRUE,
     11,
     TRUE),

    ('00000000-0000-0000-0000-000000000102',
     'VISION_RECOGNIZING_OBJECTS_PEOPLE',
     'Recognizing objects or people',
     'Difficulty recognizing objects, faces, or people using visual information.',
     '00000000-0000-0000-0000-000000000001',
     TRUE,
     12,
     TRUE),

    ('00000000-0000-0000-0000-000000000103',
     'VISION_DETAILS_CONTRAST',
     'Seeing details or contrast',
     'Difficulty seeing fine details or distinguishing objects because of contrast or visual clarity.',
     '00000000-0000-0000-0000-000000000001',
     TRUE,
     13,
     TRUE),

    ('00000000-0000-0000-0000-000000000104',
     'VISION_NAVIGATING_VISUAL_INFORMATION',
     'Navigating using visual information',
     'Difficulty using visual information to move through or understand surroundings.',
     '00000000-0000-0000-0000-000000000001',
     TRUE,
     14,
     TRUE);


-- ============================================================
-- HEARING
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000201',
     'HEARING_SPEECH',
     'Hearing speech',
     'Difficulty hearing spoken conversations or instructions.',
     '00000000-0000-0000-0000-000000000002',
     TRUE,
     21,
     TRUE),

    ('00000000-0000-0000-0000-000000000202',
     'HEARING_ALARMS_SOUNDS',
     'Hearing alarms and environmental sounds',
     'Difficulty noticing alarms, warnings, doorbells, traffic sounds, or other environmental sounds.',
     '00000000-0000-0000-0000-000000000002',
     TRUE,
     22,
     TRUE),

    ('00000000-0000-0000-0000-000000000203',
     'HEARING_NOISY_ENVIRONMENTS',
     'Understanding speech in noisy environments',
     'Difficulty understanding spoken communication when background noise is present.',
     '00000000-0000-0000-0000-000000000002',
     TRUE,
     23,
     TRUE),

    ('00000000-0000-0000-0000-000000000204',
     'HEARING_AUDIO_INFORMATION',
     'Accessing audio information',
     'Difficulty accessing information that is primarily provided through sound or audio.',
     '00000000-0000-0000-0000-000000000002',
     TRUE,
     24,
     TRUE);


-- ============================================================
-- MOBILITY
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000301',
     'MOBILITY_WALKING_MOVING',
     'Walking or moving around',
     'Difficulty walking or moving from one place to another.',
     '00000000-0000-0000-0000-000000000003',
     TRUE,
     31,
     TRUE),

    ('00000000-0000-0000-0000-000000000302',
     'MOBILITY_BALANCE',
     'Maintaining balance',
     'Difficulty maintaining balance while standing, walking, or changing position.',
     '00000000-0000-0000-0000-000000000003',
     TRUE,
     32,
     TRUE),

    ('00000000-0000-0000-0000-000000000303',
     'MOBILITY_ARMS_HANDS',
     'Moving or controlling arms and hands',
     'Difficulty moving, controlling, or using the arms, hands, or fingers for everyday activities.',
     '00000000-0000-0000-0000-000000000003',
     TRUE,
     33,
     TRUE),

    ('00000000-0000-0000-0000-000000000304',
     'MOBILITY_TRANSFERRING',
     'Getting up, sitting down, or transferring',
     'Difficulty getting up, sitting down, or moving between surfaces or positions.',
     '00000000-0000-0000-0000-000000000003',
     TRUE,
     34,
     TRUE);


-- ============================================================
-- COMMUNICATION
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000401',
     'COMMUNICATION_SPEECH',
     'Expressing needs or ideas through speech',
     'Difficulty communicating needs, ideas, or information through spoken language.',
     '00000000-0000-0000-0000-000000000004',
     TRUE,
     41,
     TRUE),

    ('00000000-0000-0000-0000-000000000402',
     'COMMUNICATION_UNDERSTANDING_SPOKEN',
     'Understanding spoken communication',
     'Difficulty understanding spoken questions, instructions, or conversations.',
     '00000000-0000-0000-0000-000000000004',
     TRUE,
     42,
     TRUE),

    ('00000000-0000-0000-0000-000000000403',
     'COMMUNICATION_WITHOUT_SPEECH',
     'Communicating without speech',
     'Difficulty communicating effectively when speech is not available or sufficient.',
     '00000000-0000-0000-0000-000000000004',
     TRUE,
     43,
     TRUE),

    ('00000000-0000-0000-0000-000000000404',
     'COMMUNICATION_AAC',
     'Using alternative or augmentative communication',
     'Difficulty communicating without support from alternative or augmentative communication methods.',
     '00000000-0000-0000-0000-000000000004',
     TRUE,
     44,
     TRUE);


-- ============================================================
-- COGNITION & LEARNING
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000501',
     'COGNITION_MEMORY',
     'Remembering information',
     'Difficulty remembering information, instructions, appointments, or routines.',
     '00000000-0000-0000-0000-000000000005',
     TRUE,
     51,
     TRUE),

    ('00000000-0000-0000-0000-000000000502',
     'COGNITION_ATTENTION',
     'Maintaining attention',
     'Difficulty maintaining focus or attention during tasks or activities.',
     '00000000-0000-0000-0000-000000000005',
     TRUE,
     52,
     TRUE),

    ('00000000-0000-0000-0000-000000000503',
     'COGNITION_PROCESSING',
     'Understanding or processing information',
     'Difficulty understanding, processing, or following information.',
     '00000000-0000-0000-0000-000000000005',
     TRUE,
     53,
     TRUE),

    ('00000000-0000-0000-0000-000000000504',
     'COGNITION_PLANNING',
     'Planning or organizing tasks',
     'Difficulty planning, sequencing, organizing, or completing everyday tasks.',
     '00000000-0000-0000-0000-000000000005',
     TRUE,
     54,
     TRUE);


-- ============================================================
-- SELF-CARE
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000601',
     'SELF_CARE_EATING_DRINKING',
     'Eating or drinking',
     'Difficulty eating or drinking independently.',
     '00000000-0000-0000-0000-000000000006',
     TRUE,
     61,
     TRUE),

    ('00000000-0000-0000-0000-000000000602',
     'SELF_CARE_DRESSING',
     'Dressing',
     'Difficulty putting on, removing, or managing clothing independently.',
     '00000000-0000-0000-0000-000000000006',
     TRUE,
     62,
     TRUE),

    ('00000000-0000-0000-0000-000000000603',
     'SELF_CARE_HYGIENE',
     'Personal hygiene',
     'Difficulty carrying out everyday personal hygiene activities independently.',
     '00000000-0000-0000-0000-000000000006',
     TRUE,
     63,
     TRUE),

    ('00000000-0000-0000-0000-000000000604',
     'SELF_CARE_TOILETING',
     'Toileting',
     'Difficulty using toilet facilities independently.',
     '00000000-0000-0000-0000-000000000006',
     TRUE,
     64,
     TRUE);


-- ============================================================
-- ENVIRONMENTAL ACCESS
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000701',
     'ENVIRONMENTAL_BUILDING_ACCESS',
     'Entering or moving through buildings',
     'Difficulty entering, exiting, or moving through buildings and physical spaces.',
     '00000000-0000-0000-0000-000000000007',
     TRUE,
     71,
     TRUE),

    ('00000000-0000-0000-0000-000000000702',
     'ENVIRONMENTAL_REACHING_HANDLING',
     'Reaching or handling objects in the environment',
     'Difficulty reaching, grasping, or handling objects in everyday surroundings.',
     '00000000-0000-0000-0000-000000000007',
     TRUE,
     72,
     TRUE),

    ('00000000-0000-0000-0000-000000000703',
     'ENVIRONMENTAL_FURNITURE_FIXTURES',
     'Using furniture or fixtures',
     'Difficulty using everyday furniture, fixtures, doors, switches, or similar environmental features.',
     '00000000-0000-0000-0000-000000000007',
     TRUE,
     73,
     TRUE),

    ('00000000-0000-0000-0000-000000000704',
     'ENVIRONMENTAL_PUBLIC_FACILITIES',
     'Accessing public facilities and spaces',
     'Difficulty accessing or using public facilities, services, and shared physical spaces.',
     '00000000-0000-0000-0000-000000000007',
     TRUE,
     74,
     TRUE);


-- ============================================================
-- DIGITAL ACCESS
-- ============================================================

INSERT INTO functional_needs
    (id, code, label, description, parent_need_id, is_selectable, display_order, active)
VALUES
    ('00000000-0000-0000-0000-000000000801',
     'DIGITAL_SMARTPHONE_TABLET',
     'Using smartphones or tablets',
     'Difficulty using smartphones, tablets, or touchscreen devices.',
     '00000000-0000-0000-0000-000000000008',
     TRUE,
     81,
     TRUE),

    ('00000000-0000-0000-0000-000000000802',
     'DIGITAL_COMPUTERS_KEYBOARDS',
     'Using computers or keyboards',
     'Difficulty using computers, keyboards, mice, or similar input devices.',
     '00000000-0000-0000-0000-000000000008',
     TRUE,
     82,
     TRUE),

    ('00000000-0000-0000-0000-000000000803',
     'DIGITAL_READING_CONTENT',
     'Reading digital content',
     'Difficulty reading or accessing information presented on digital screens.',
     '00000000-0000-0000-0000-000000000008',
     TRUE,
     83,
     TRUE),

    ('00000000-0000-0000-0000-000000000804',
     'DIGITAL_INTERFACES',
     'Interacting with digital interfaces',
     'Difficulty navigating, selecting, entering information, or completing tasks in digital interfaces.',
     '00000000-0000-0000-0000-000000000008',
     TRUE,
     84,
     TRUE);