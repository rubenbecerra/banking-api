CREATE TABLE customer (
                          id SERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          age INT NOT NULL,
                          gender VARCHAR(20) NOT NULL DEFAULT 'NOT_SPECIFIED',
                          password VARCHAR(255) NOT NULL DEFAULT '',
                          role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER'
);

INSERT INTO customer (name, email, age, gender, password, role)
VALUES (
           'Admin User',
           'admin@bank.com',
           30,
           'MALE',
           '$2a$17%WnqvVwQ9z9K5o6gXkLgqe09V4fHjT8zB2rN6eK8rY1oP0mU1qG4a',
           'ROLE_ADMIN'
       );