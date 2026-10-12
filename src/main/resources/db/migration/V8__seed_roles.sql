INSERT INTO roles (id, name)
VALUES
    (gen_random_uuid(), 'USER'),
    (gen_random_uuid(), 'ADMIN')
ON CONFLICT (name) DO NOTHING;
