# Security and release gate

This starter deliberately **does not** implement user login, JWT or payment handling. It is not safe to accept public orders. The production profile rejects order writes.

Before an application can accept **real** customer actions, verify each item below:

- [ ] Implement Spring Security and an audited login strategy; enforce role permissions per endpoint and resource.
- [ ] Enforce ownership checks so a customer cannot read someone else's order ID.
- [ ] Add authentication and request throttling on order creation, and anti-automation controls.
- [ ] Consider CSRF defenses if browser session-cookie authentication is used; apply appropriate CORS rules.
- [ ] Never collect payment card details directly in this demo; use a properly authorized sandbox/payment provider, webhook signature verification and reconciliation.
- [ ] Remove seeded demo catalogue and handle product administration with authorized roles.
- [ ] Replace local Compose database with managed, isolated MySQL, encryption, backup and tested restore.
- [ ] Use a managed secrets store, least-privilege service credentials and automated secret scanning.
- [ ] Pin and routinely update container image digests and dependencies, scan vulnerabilities and license issues.
- [ ] Make migrations backward compatible with both old and new app during rolling deploys.
- [ ] Add privacy notice, data retention/deletion and secure handling of logs and support data.
- [ ] Add meaningful health/readiness checks, observability, uptime alerts and on-call instructions.
- [ ] Perform load, failure-recovery, authorization and security tests on a **private staging** system.
- [ ] Prepare a real deployment rollback plan and prohibit destructive DB rollback shortcuts.

## Operational definition of "live"

`docker compose up` on your laptop means **local**. A domain with HTTPS and accessible hosted services means **deployed demo**. A commercial production system additionally needs security, reliability, monitoring and ongoing operational responsibility. These are different milestones.
