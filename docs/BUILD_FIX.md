# Docker build incident 001: React DOM / Vite Rollup parser failure

**Observed:** Vite 7.3.8 exited during `npm run build` with `Code is nested too deeply` in `node_modules/react-dom/client.js`. The backend `CANCELED` message was a consequence of the frontend build failure, not evidence of a Java/Maven error.

**Probable area:** Vite/Rollup dependency handling (the logged location is a dependency, not the application code). The exact parser failure has not been reproduced here.

**Mitigation in this archive:** Updated the frontend to an aligned Vite 8.3.8 + `@vitejs/plugin-react` 6.1.1 + Vitest 4.1.11 stack (Vite 8 changes the production bundler to Rolldown), and pinned React and React DOM to the same exact version. This is a candidate fix requiring testing on the user's machine.

## PowerShell fix and reproduce

From the root folder containing `compose.yml`:

```powershell
cd frontend
npm install --package-lock-only --ignore-scripts
cd ..
docker compose build --no-cache frontend
docker compose up -d --build
docker compose ps
```

You should commit `frontend/package-lock.json` after install so Docker and GitHub Actions both resolve the same dependency tree. Once it is present the Dockerfile uses `npm ci`. For CI, update its frontend install step to `npm ci --no-audit --no-fund` and commit the lockfile in the same change.

If Docker still fails, isolate the error:

```powershell
docker compose build --no-cache --progress=plain frontend
docker compose logs --tail=100 frontend backend db
```

If frontend now builds but backend fails, debug the new backend error separately.

When running the application, open http://localhost:8085 and verify that the API responds at http://localhost:8080/api/products.
