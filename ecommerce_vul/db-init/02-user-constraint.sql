-- Not enforced as UNIQUE on purpose in some paths (left nullable/loose) so
-- duplicate-username registration and enumeration stay possible for the lab.
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
