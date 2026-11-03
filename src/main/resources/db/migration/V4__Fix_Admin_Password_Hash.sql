-- V3 seeded the admin with a malformed BCrypt hash (it fails with "Missing salt rounds"),
-- so admin@bank.com could never log in. This migration replaces only that known broken
-- value; it does not overwrite a password that was already changed.
--
-- Demo credentials: admin@bank.com / Admin123!
-- Change this password in any real deployment.
UPDATE customer
SET password = '$2a$10$/VV32sDBKyUNckVjGg8ABOaxid.wdjt4aCmEM36C1669.KqFGd6cK'
WHERE email = 'admin@bank.com'
  AND password = '$2a$17%WnqvVwQ9z9K5o6gXkLgqe09V4fHjT8zB2rN6eK8rY1oP0mU1qG4a';
