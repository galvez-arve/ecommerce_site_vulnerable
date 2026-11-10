-- VulnLab migration: adds auth (plaintext, on purpose), product images, and an
-- order "notes" field used to demonstrate stored XSS. Run after 00-restore.sh.

ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'user';

ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url TEXT;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_notes TEXT;

-- Backfill: every existing customer gets a guessable username/password so
-- students can harvest or brute-force real-looking accounts.
UPDATE users
SET username = lower(split_part(email_address, '@', 1)),
    password = 'Password123',
    role = 'user'
WHERE username IS NULL;

-- Deliberately weak/default admin and staff accounts.
INSERT INTO users (id, name, lastname, address, contact_number, email_address, username, password, role)
VALUES
  (gen_random_uuid(), 'Site', 'Admin', 'HQ Warehouse, Manila, Philippines', '+639170000001', 'admin@vulnlab.local', 'admin', 'admin123', 'admin'),
  (gen_random_uuid(), 'Store', 'Manager', 'HQ Warehouse, Manila, Philippines', '+639170000002', 'manager@vulnlab.local', 'manager', 'manager123', 'admin'),
  (gen_random_uuid(), 'Test', 'User', '1 Demo Street, Cagayan de Oro, Philippines', '+639170000003', 'test@vulnlab.local', 'testuser', 'password1', 'user')
ON CONFLICT DO NOTHING;

-- Deterministic placeholder images by category, applied to every product by
-- matching its name against a keyword list (students aren't meant to attack
-- this; it just makes the catalog look real).
UPDATE products SET image_url = 'https://picsum.photos/seed/' || md5(product_name) || '/500/500';
