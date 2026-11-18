# Test matrix

| Environment | Infrastructure | Test class | Purpose | Status |
| --- | --- | --- | --- | --- |
| Unit | Local JVM | JUnit + Mockito | Rules, no database | Included, not executed here |
| Integration | Docker/CI runner | Testcontainers MySQL 8.4 + MockMvc | Real schema, API, transaction | Included, not executed here |
| Frontend | Node + jsdom | Vitest + Testing Library | Screen and error handling | Included, not executed here |
| Development | Docker Compose / localhost:8085 | Manual smoke | Full frontend -> backend -> database | Configuration included, not run |
| Staging | Separate Compose project / localhost:8086 | Regression, failure drills | Environment-specific behavior | Configuration included, not run |
| Demo production | Cloud VM + domain | TLS, health, uptime, smoke | Read-only public showcase | Instructions only, not deployed |

## Repeatable manual tests
1. Open localhost:8085; catalogue shows 3 products.
2. Submit order quantity=1; response shows `PLACED`; stock decreases by 1.
3. Send identical POST (same `idempotencyKey`, productId, quantity). Confirm same order ID and no second stock decrement.
4. Reuse same idempotency key with different quantity. Expect HTTP 409.
5. Request quantity greater than stock. Expect HTTP 409; stock remains unchanged.
6. Missing productId/quantity=0: HTTP 400. Unknown order ID: 404.
7. Run simultaneous requests for final stock in a disposable test database. Exactly available units should sell; no stock below zero.
8. `docker compose stop db` in LOCAL ONLY. API errors should be observable; restore with `docker compose start db`.
9. Stage with port mapping 8086/8081/3307. Verify dev and staging volumes are separate.
10. In public read-only profile, POST /api/orders must return 403; GET /api/products should work; `/api/config` reports `ordersEnabled=false`.
11. Production DNS and TLS: HTTPS only, correct domain, no public DB/backend port.
12. Verify the health endpoint is intentionally unavailable from the public frontend proxy.

## Important test gaps and next steps
- True concurrent requests and high-load performance need a dedicated integration/load test.
- Cross-product idempotency-key races currently produce a 409 rather than replaying the original response; improve with a separate idempotency registry.
- Authentication, role permissions, rate limiting, payment handling, audit trails, and security scans are **not implemented** in the starter.
- No environment has been actually deployed by this archive; the matrix tracks included checks, not claimed results.
