INSERT INTO posttrack.users (id, email, password_hash, roles, status, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'client@example.test', 'LOGIN_DISABLED', ARRAY['CLIENT'], 'ACTIVE', now()),
    ('00000000-0000-0000-0000-000000000002', 'operator@example.test', 'LOGIN_DISABLED', ARRAY['OPERATOR'], 'ACTIVE', now()),
    ('00000000-0000-0000-0000-000000000003', 'courier@example.test', 'LOGIN_DISABLED', ARRAY['COURIER'], 'ACTIVE', now())
ON CONFLICT DO NOTHING;
--docker compose cp scripts/demo-users.sql postgres:/tmp/demo-users.sql
--docker compose exec postgres psql -U posttrack -d posttrack -f /tmp/demo-users.sql