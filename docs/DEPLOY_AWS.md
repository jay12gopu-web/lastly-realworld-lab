# Deploy the read-only demo to a cloud VM (AWS EC2 example)

**Important:** This starter is not a fully secure commerce product. Production profile disables order writes. No authentication or payments are implemented. Deploy as a read-only portfolio/showcase, or restrict access to trusted testers.

1. Create an AWS account only if you are authorized to do so and understand potential costs. Use a trusted adult/account owner where required.
2. Provision a suitably sized Ubuntu EC2 instance with public IP. Use SSH keys; allow SSH **only from your own IP**. Expose TCP 80 and 443 in the security group; do not expose 3306 or 8080.
3. Point your chosen domain's DNS A record at the instance's public IP. Wait until DNS resolves.
4. Install Docker Engine and its Compose plugin from official Docker documentation. Confirm `docker compose version`.
5. Transfer this repository to the machine using an authenticated Git repo or SCP. `cp .env.example .env`; set `APP_DOMAIN` to your domain and set unique, strong `MYSQL_ROOT_PASSWORD` and `DB_PASSWORD` in `.env` (keep `DB_USERNAME=lastly`). Do **not** commit .env.
6. Run: `docker compose -f compose.production.yml config` to inspect deployment settings (redact secrets before sharing output). Then: `docker compose -f compose.production.yml up -d --build`.
7. Inspect: `docker compose -f compose.production.yml ps` and `docker compose -f compose.production.yml logs --tail=100 backend caddy frontend` (redact secrets if pasting logs).
8. Visit `https://YOUR_DOMAIN/`. Caddy obtains/renews HTTPS certificates if DNS, networking, domain, and ACME validation are correct. Check `GET /api/products` and that order POST returns 403.
9. Set up automated encrypted database backups with off-instance retention, restricted IAM/access, uptime monitoring, alerts, DNS renewal and OS patching. Verify restore **on a separate machine**.
10. Use a staging gate before every release. Tag image/release versions. If broken, roll back compatible app image and follow the database migration rollback plan; avoid blindly reverting a schema migration.

For a **real commercial application**, migrate to managed RDS MySQL, ECS/EKS or app hosting, a load balancer, proper secret storage, observability, backups and multi-AZ design. Add authentication/authorization, abuse controls, privacy/security review and load tests before accepting public orders.

## Example local commands
- Development: `docker compose up -d --build` -> http://localhost:8085
- Independent staging: `docker compose -p lastly-staging -f compose.staging.yml up -d --build` -> http://localhost:8086
- Production demo: `docker compose -f compose.production.yml up -d --build` -> configured HTTPS domain

**Separate database caution:** The development and staging Compose projects use different named volumes and different host ports. Treat them as distinct environments; never connect staging to production credentials.
