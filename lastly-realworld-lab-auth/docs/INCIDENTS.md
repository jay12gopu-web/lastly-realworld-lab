# Practical production issues and drills

Use only disposable/local or authorized staging environments for disruptive exercises. Never create failures on someone else's system or a real live service.

| Incident | Common symptom | Inspect | First mitigation | Durable fix |
| --- | --- | --- | --- | --- |
| MySQL down | API 500 / cannot connect | `docker compose logs backend db` | Restore database connectivity | HA DB, probes, alerting |
| API starts before DB | startup failure | `docker compose ps` | Wait for healthy DB / restart app | readiness checks and startup orchestration |
| Frontend points to wrong backend | Nginx 502 | browser Network tab and Nginx logs | Correct reverse proxy target | contract tests + same-origin proxy |
| Multiple shoppers buy last item | negative or incorrect stock | order/stock records | block new orders if inventory corrupt | row locks, transaction, concurrency tests |
| Browser resends order | duplicate transactions | idempotency key, order ID | retry with SAME key | durable idempotency registry, request fingerprint |
| Wrong database migration | app refuses startup | Flyway history/logs | stop release, restore compatible version | backward-compatible expand/contract migration |
| Slow API | latency or timeouts | access logs, DB slow query logs | identify hot endpoint/query | indexes, pool sizing, cache and tracing |
| Port already used | container won't bind | `docker compose ps`, `netstat` | change local host port | environment-specific port maps |
| Out-of-memory | restarts / high GC | container metrics, memory | reduce traffic safely / scale | JVM heap sizing, profiling, budgets |
| Secrets committed | credentials exposed | Git history and alerts | revoke and rotate affected secrets | secrets manager, scanning, least privilege |
| Public POST endpoint abused | unexpected order traffic | request logs, rate, IP | disable writes (already prod default) | authentication, rate limiting, abuse protection |
| Deployment breaks compatibility | failing API after release | CI, release logs, health checks | roll back app / forward-fix schema | rolling releases, compatibility gates |

## Structured on-call exercise
1. Start dev stack and capture baseline `GET /api/products` response and p95 latency from repeated authorized requests.
2. In local stack only, stop MySQL; observe logs/status and application behavior.
3. Restore MySQL, verify recovery and order idempotency.
4. Write a small incident report: timeline, impact, contributing causes, containment, permanent fix, and test that prevents recurrence.

## Logging and traces
- Backend places X-Correlation-ID on responses and in the logging MDC.
- Never log passwords, tokens, payment details, or complete personal information.
- Actuator exposes health/info internally; configure more secure metrics endpoints before adding Prometheus.
- Do not claim that failure recovery is automatic or metrics/alerts are production-ready in this sample.
