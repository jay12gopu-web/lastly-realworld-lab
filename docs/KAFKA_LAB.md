# Phase 4 — Kafka, transactional outbox, at-least-once delivery

## Why this design

A naïve service that saves the order to MySQL and then calls Kafka may lose the event if the application crashes between those actions. In this lab, the order transaction also INSERTs a PENDING event in `order_event_outbox` (Flyway V4). A scheduled publisher sends that event to Kafka and marks it SENT **only after the broker acknowledges it**. The consumer processes the message and records its event ID in `processed_order_events`, ignoring duplicate deliveries.

This is **at least once**, not exactly once. Duplicate Kafka messages can still occur (e.g. crash after publish but before marking SENT). The consumer SQL unique key makes its current database side effect idempotent. New email/payment/delivery side effects must have **their own** deduplication strategy.

## Windows PowerShell — from project root

```powershell
# Keep your existing MySQL volume; never run docker compose down -v.
docker compose up -d --build
# Inspect containers
docker compose ps
# Run end-to-end order smoke test (uses a dummy account and a unit of demo inventory)
py scripts/smoke_test.py --url http://localhost:8080
# Check asynchronous publisher + consumer logs
docker compose logs --tail=100 backend
# Broker topic discovery (Kafka is only accessible from the Docker network)
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
# Inspect durable outbox and deduplicated receipts
docker compose exec db mysql -ulastly -plastly_dev lastly -e "SELECT event_id, aggregate_id, status, attempts FROM order_event_outbox ORDER BY created_at DESC LIMIT 5; SELECT event_id, order_id FROM processed_order_events ORDER BY processed_at DESC LIMIT 5;"
```

A local live message is observable in `docker compose logs -f backend` as `OUTBOX_SENT` and `EVENT_PROCESSED`. Publisher polls every 3 seconds; The Kafka service has a healthcheck, so Spring Boot starts only after the broker responds. If startup fails, inspect `docker compose logs kafka` first.

## Real incident drill: broker outage and recovery

```powershell
# 1. Stop only Kafka
docker compose stop kafka
# 2. Place ONE demo order using the frontend at http://localhost:8085
# 3. Check that order was saved and that outbox status remains PENDING
docker compose exec db mysql -ulastly -plastly_dev lastly -e "SELECT aggregate_id, status, attempts FROM order_event_outbox ORDER BY created_at DESC LIMIT 5;"
# 4. Restart Kafka
docker compose start kafka
# 5. Watch retries and eventual delivery
docker compose logs -f backend
```

Success means: order remains PLACED even while Kafka is offline; pending event is eventually marked SENT when Kafka recovers; exactly one row exists in `processed_order_events` for the event ID.

## Test matrix

- `cd backend; mvn verify`: JUnit, Mockito and MySQL Testcontainers tests. Tests deliberately disable Kafka networking; they verify transactional outbox persistence and publisher retry logic with a mocked broker.
- `py scripts/smoke_test.py --url http://localhost:8080`: API + JWT + idempotency + stock.
- Stop broker: expect PENDING outbox, order still succeeds.
- Restart broker: expect SENT and processed receipt.
- Retry same order idempotency key: expect no second order or outbox event.

## Boundaries and next hardening

This is a **single-node development** Kafka broker with no auth/TLS and no persistent Kafka log volume: recreating the Kafka container can lose broker messages. Do not expose it to public networks or treat it as production-durable. A SENT outbox event is not automatically replayed after Kafka data loss; production requires broker replication, durable storage and operational recovery. The outbox poller is single-instance-oriented; multiple backend replicas can publish duplicates and cause contention. A more advanced outbox uses `SKIP LOCKED` claim/lease, backoff and dead-letter handling, or CDC (Debezium). This demo event consumer records a receipt; it **does not** send real messages or initiate deliveries. The staging and public-read-only profiles intentionally leave Kafka workers disabled.
