# Lastly Real-World Lab — runnable Java full-stack starter

A hands-on quick-commerce demo designed to teach **engineering realities**, not just CRUD. Functional vertical slice: list products, submit a demo order, reserve stock transactionally, safely retry a request, view status, run tests and operate in multiple environments.

> **Build incident fix (Nov 18, 2026):** This revised archive updates Vite/React dependencies after a Rollup parser failure in `react-dom/client.js`. Follow [docs/BUILD_FIX.md](docs/BUILD_FIX.md) to generate a frontend lockfile and rebuild. **This fix is not confirmed until the Docker build runs on your machine.**

## Phase 3 update — customer authentication

**Added:** JWT registration/login, BCrypt hashes, per-user order ownership, Bearer-token checks, customer login UI, migration V3, and updated test scripts. See [docs/AUTH_LAB.md](docs/AUTH_LAB.md). This is a **local/private training release**, not yet suitable for public account creation.

## What's ready, and what isn't

**Included:** Java 21/Spring Boot 3.5.16, React + Vite, MySQL 8.4, Flyway migrations, validated REST API, transaction + row lock, idempotency key, API error handler, request correlation ID, Docker Compose, unit/integration/UI test source, CI workflow, staging override, and cloud demo guide.

**Not implemented yet:** refresh-token rotation, account verification, rate limiting, Kafka, Redis, payments, delivery tracking, automatic CD, distributed tracing, and production alerting. **Do not expose registration publicly yet.** See `docs/ROADMAP.md`.

**Validation limitation:** This package was generated in an environment without Docker, Maven or internet access for dependency installs, so tests and builds have **not been executed**. Run the commands below on your machine or in GitHub Actions, then fix any discovered compatibility issues before deployment.

## Option A — easiest: Docker Desktop

Requirements: Docker Desktop with Docker Compose.

```bash
docker compose up -d --build
docker compose ps
```

Open **http://localhost:8085**. API is http://localhost:8080/api/products and health is http://localhost:8080/actuator/health. MySQL host port is 3306, using a **local-development-only** password.

Run a cross-environment smoke test against the running application (requires Python 3):

```bash
python scripts/smoke_test.py --url http://localhost:8080
python scripts/concurrency_lab.py
```

The concurrency lab intentionally places **multiple local test orders** and uses up demo stock. Run it only on disposable development data.

Run all backend tests (Docker required for Testcontainers):

```bash
cd backend
mvn verify
```

Run frontend tests:

```bash
cd frontend
npm install
npm test
npm run build
```

## Option B — develop with hot reload in IntelliJ + VS Code

From root, start ONLY MySQL:

```bash
docker compose up -d db
```

In IntelliJ, open `backend/pom.xml` as a Maven project. Select Java 21 and set active profile `dev`. Terminal equivalent:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to localhost:8080; no cross-origin API URL is needed.

## API endpoints

| Method | Path | Use |
| --- | --- | --- |
| GET | `/api/products` | Catalogue with current stock |
| GET | `/api/config` | Safe public flag indicating whether demo order writes are enabled |
| POST | `/api/auth/register` | Create CUSTOMER account (private lab) |
| POST | `/api/auth/login` | Get 15-minute Bearer token |
| POST | `/api/orders` | Create authenticated test order (dev/staging only) |
| GET | `/api/orders/{id}` | Fetch **your own** order with Bearer token |
| GET | `/actuator/health` | Backend health (internal only in public deployment) |

Example POST payload:

```json
{ "productId": 1, "quantity": 1, "idempotencyKey": "retry-safe-order-123" }
```

A request with `Authorization: Bearer <accessToken>` is required for order endpoints. A repeated request with identical key, product and quantity from the **same signed-in customer** returns the existing order, without a second stock deduction. An attempt to reuse the key with different details produces a conflict. Client must keep the **same key** after uncertain network failures.

## Environment matrix

| Profile | UI | Backend | MySQL | Write access |
| --- | --- | --- | --- | --- |
| Dev | localhost:8085 or :5173 | localhost:8080 | localhost:3306 | Enabled, local only |
| Staging | localhost:8086 | localhost:8081 | localhost:3307 | Enabled, **private testers only** |
| Public demo | HTTPS domain via Caddy | Private Docker network | Private Docker network | **Disabled** |

For independent staging:

```bash
docker compose -p lastly-staging -f compose.staging.yml up -d --build
```

Production showcase setup is documented in `docs/DEPLOY_AWS.md`.

## Real troubleshooting practice

- `docker compose logs --tail=100 backend db frontend`
- `docker compose ps`
- `curl http://localhost:8080/actuator/health`
- `curl http://localhost:8080/api/products`
- `docker compose down` (does not delete named database volume)
- **Never run** `docker compose down -v` on a database you need to preserve: `-v` removes volumes.

Read `docs/TEST_MATRIX.md`, `docs/INCIDENTS.md`, and `docs/SECURITY_RELEASE_CHECKLIST.md` for failure scenarios, troubleshooting and release gates.

## Security reminder

This project is for learning. Dev/staging create orders without login, so keep them local or private. Production is READ-ONLY until proper authentication, rate limiting, monitoring and incident-response controls are implemented. No real customer data or card details should be used. Never publish your `.env`.
