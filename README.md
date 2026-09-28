# 🏦 Banking Core API (Production-Ready Architecture)

![CI/CD Pipeline](https://github.com/Rubenbc-lab/banking-api/actions/workflows/ci.yml/badge.svg)
![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)
![Docker](https://img.shields.io/badge/Docker-GHCR-2496ED?logo=docker)
![JaCoCo](https://img.shields.io/badge/Coverage-85%25%2B-green)

A production-grade core banking RESTful API built with **Spring Boot 4** and **Java 21**. This project serves as an enterprise financial engine implementing **Clean Architecture**, secure authentication via **HttpOnly Cookies + JWT**, advanced transaction management with **Transaction Templates**, high-performance caching & **Idempotency** via **Redis**, robust database migrations with **Flyway**, automated integration testing with **Testcontainers**, and a strict **CI/CD pipeline** targeting GitHub Container Registry (GHCR).

---

## 🎯 Project Objective & Scope

The goal of this project is to implement an enterprise-standard backend architecture ready for high-concurrency financial workloads:
- **Clean Architecture:** Strict domain-centric separation of concerns (Domain, Application, Infrastructure, and Presentation layers) ensuring high maintainability and testability.
- **Secure Authentication Model:** Advanced security flow using stateless **JWT stored in secure HttpOnly Cookies** combined with **BCrypt password hashing** to prevent XSS and CSRF vulnerabilities.
- **Transactional Consistency & Idempotency:** Explicit transaction management and idempotency guards to safely handle financial operations (Deposits, Withdrawals, Transfers) and prevent duplicate processing.
- **Resilient Testing Strategy:** Elimination of in-memory mocks (H2) in favor of real, isolated runtime dependencies (**Testcontainers** for PostgreSQL and Redis).
- **Quality Gates:** Strict code coverage validation enforced via **JaCoCo** ($\ge 85\%$) preventing failing builds from reaching production.
- **Automated Delivery (DevOps):** End-to-end continuous integration and deployment pipeline that builds, tests, verifies coverage, and publishes production-ready multi-stage Docker images to **GHCR**.
- **Observability:** Health checks, liveness/readiness probes, and telemetry endpoints via **Spring Boot Actuator**.

---

## 🛠️ Tech Stack

- **Core:** Java 21 (LTS), Spring Boot 4
- **Architecture Pattern:** Clean Architecture (Domain-Centric / Hexagonal approach)
- **Data Persistence:** Spring Data JPA, Hibernate, PostgreSQL 16
- **Database Versioning:** Flyway (Versioned SQL migrations)
- **Caching & Idempotency Layer:** Redis 7 / Lettuce
- **Security:** Spring Security 7, JJWT (HMAC-SHA256), HttpOnly Cookies
- **Testing:** JUnit 5, AssertJ, Mockito, Testcontainers (PostgreSQL & Redis)
- **Code Coverage:** JaCoCo (Enforced minimum threshold: 85%)
- **Observability:** Spring Boot Actuator
- **Containerization & CI/CD:** Docker (Multi-stage build), Docker Compose, GitHub Actions, GHCR

---

## 📦 Project Structure

The project is structured following strict modular boundaries separating account management, authentication, and core banking transactions:
- **`com.banking.accounts`**: Handles account creation, states, and balance queries.
- **`com.banking.customer`**: Manages customer profiles, registration, and credentials.
- **`com.banking.transactions`**: Implements deposit, withdrawal, and transfer workflows with strict transaction isolation.
- **`com.banking.shared`**: Houses cross-cutting concerns including security filters, JWT handling, global exception management, and Redis connectivity.
- **`com.banking.auth`**: Manages the authentication control (register/login) ensuring correct implementation.

---

## ⚡ Financial Idempotency & Transactions

To guarantee data safety, consistency, and protection against accidental request retries over faulty networks:
- **Idempotency Interceptors & Templates:** Intercepts state-changing operations using unique client request keys saved in Redis, safely rejecting duplicate executions.
- **Transactional Templates:** Ensures atomic boundaries across complex financial transfers, ensuring proper rollback behavior if any consistency check fails.

---

## 🚀 Local Deployment with Docker Compose

The complete architecture (API, PostgreSQL 16, and Redis 7) runs containerized out-of-the-box via Docker Compose:

1. Clone the repository.
2. Review your environment variables in `docker-compose.yml`.
3. Build and spin up all services locally in detached mode:

```bash
docker compose up --build -d
```
## 🏛️ Architecture Overview

```text
[ Client Requests ]
        │
        ▼
[ Presentation Layer ] ──── (REST Controllers / DTOs / Mappers)
        │
        ▼
[ Security Filter Chain ] ─ (JWT Verification via HttpOnly Cookies)
        │
        ▼
[ Application Layer ] ───── (Use Cases / Business Orchestration / Decorators)
        │
        ▼
[ Domain Layer ] ────────── (Pure Business Entities / Domain Rules / Repository Ports)
        │
        ▼
[ Infrastructure Layer ] ── (Spring Data JPA / PostgreSQL / Redis / Idempotency / Config)