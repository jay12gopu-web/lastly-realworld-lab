# Roadmap: from vertical slice to real-world engineering

## Phase 1 (included)
- Java 21, Spring Boot REST API, React UI, MySQL, Flyway.
- Inventory decrement inside a transaction with pessimistic row lock.
- Idempotency key to avoid duplicate orders on retries of same product/quantity.
- Error responses, bean validation, correlation IDs, Actuator health.
- Docker Compose, staging override, read-only production demo, GitHub Actions and tests.

## Phase 2 (security — starter authentication added, hardening outstanding)
- **Added:** Spring Security, BCrypt, JWT validation, CUSTOMER role and customer order access control.
- **Outstanding:** refresh-token strategy, ADMIN role management, full access policies and multi-factor protections.
- Limit API requests; securely handle secrets, audit events, and input validation.
- Product-admin CRUD with authorization, pagination, sorting, and SQL index analysis.

## Phase 3 (commerce domain)
- Users, carts, multi-item orders, normalized order lines, stock reservations/expiry.
- Payment gateway sandbox, webhooks, signature validation and reconciliation. NO real payment data in this project.
- Order state machine, refunds/cancellations, audit trail, email notification abstraction.

## Phase 4 (Kafka, Redis and reliability)
- Transactional outbox -> Kafka producer, idempotent consumer and retry/DLQ.
- Redis catalogue cache, cache invalidation, TTL, cache outage fallbacks.
- Background processing, scheduled jobs, rate limiting, resilience tests.

## Phase 5 (operations)
- Central structured logs, distributed traces, Prometheus/Grafana, SLOs and alerts.
- Integration, contract, API and load tests; dependency scanning.
- Staging release approval, build/push immutable images, CD deployment and rollback drills.
- Cloud: AWS infrastructure with managed MySQL, network isolation, secrets/backup management.

## Learning method
For every feature: design -> implement -> unit test -> integration test -> stage -> release -> observe -> simulate a safe failure -> document the fix.
