# Phase 3: JWT customer authentication

## What's included
- Email/password registration (`POST /api/auth/register`) with BCrypt password hashes.
- Login (`POST /api/auth/login`) issuing signed HS256 JWT access tokens expiring in 15 minutes.
- Public catalogue; private order endpoints require `Authorization: Bearer <token>`.
- Orders assigned to the customer; a different customer receives 404 for another customer's order.
- Stateless APIs; browser keeps token **in memory** (no refresh tokens yet). Reloading requires signing in again.
- Existing anonymous orders from the earlier lesson remain inaccessible after migration.

## Build and run (PowerShell)
From the project root:
```powershell
cd frontend
npm install --package-lock-only --ignore-scripts
cd ..
docker compose up -d --build
docker compose ps
python scripts/smoke_test.py --url http://localhost:8080
```
Go to http://localhost:8085, register with a **dummy** email and unique practice password (12+ characters), then place an order.

## Verify the security boundary
```powershell
# Anonymous request must return 401 Unauthorized
try { Invoke-WebRequest http://localhost:8080/api/orders/unknown } catch { $_.Exception.Response.StatusCode.value__ }
# Public catalogue still works
Invoke-RestMethod http://localhost:8080/api/products
```

## Real-world considerations
* Registration/login are disabled in `prod` by default, even for the public read-only demo.
* No default admin account; registrations create CUSTOMER only. Don't expose admin access via public signup.
* Local lab JWT signing secret is intentionally a **demo-only** default. Set a unique 32+ byte `JWT_SECRET` through secure secret management for any non-local deployment; never hard-code it publicly.
* JWTs aren't revoked on logout; logout only discards the browser's token. Short-lived access tokens reduce risk; refresh-token rotation, revocation and secure delivery are separate exercises.
* Registration currently does not include email verification, rate limiting or bot mitigation. Keep this lab local/private.
* Don't use real email addresses, reused passwords, personal details or payment information.
* Run `cd backend; mvn verify` with Docker available for MySQL Testcontainers integration tests. No actual build/test run was possible in the artifact generation environment.
