# InfraForge

A UI tool for building Infrastructure as Code templates without writing code from scratch.

## Phase 1 — Terraform Builder

Fill a form → get a valid `.tf` configuration file. Live preview updates as you type.

## Quick start

```bash
docker compose up --build
```

Open [http://localhost:3000](http://localhost:3000)

## Local development (without Docker)

**Backend**
```bash
cd backend
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:3000
# Proxies /api → localhost:8080 automatically
```

## Adding a new provider

1. Create `backend/src/main/resources/providers/<name>/schema.json`
2. Create `backend/src/main/resources/providers/<name>/template.ftl`
3. Restart the backend — the provider registers automatically.

No code changes required.

## Project structure

```
infraforge/
├── backend/          Spring Boot 3 + Java 21
│   └── src/main/resources/providers/
│       └── terraform/
│           ├── schema.json   field definitions
│           └── template.ftl  FreeMarker output template
├── frontend/         React 18 + TypeScript + Vite
└── docker-compose.yml
```

## Roadmap

| Phase | Scope |
|---|---|
| 1 (current) | Terraform AWS builder, live preview, Docker |
| 2 | Ansible + Vagrant providers |
| 3 | Template save/load, local file management |
| 4 | AI-assisted field suggestions |
| 5 | Kubernetes deployment, multi-user |
