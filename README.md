# InfraForge

**A UI-driven builder for Infrastructure as Code templates.**

InfraForge removes the syntax barrier from IaC tooling. Instead of hand-writing Terraform HCL, Ansible playbooks, or Vagrantfiles, you fill out a guided form and get a valid, ready-to-use configuration file — with a live code preview that updates as you type.

Built for engineers who know *what* infrastructure they want to build, but don't want to context-switch into documentation every time they need a new template.

---

## Features

- **Schema-driven form UI** — fields, dropdowns, and help text generated from a provider schema. No hardcoded forms.
- **Live code preview** — changes in the form reflect instantly in the output pane.
- **Template library** — save, load, duplicate, and delete named templates. Auto-saves after 30 seconds of inactivity.
- **Copy & download** — grab the generated file with one click.
- **Plugin provider model** — adding a new IaC tool (Pulumi, Crossplane, AWS CDK, etc.) requires zero code changes. Drop a folder, restart.
- **Docker-first** — runs locally with a single command. No external dependencies.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, FreeMarker |
| Frontend | React 18, TypeScript, Vite, CodeMirror 6 |
| Database | PostgreSQL 16, Flyway |
| Container | Docker, Docker Compose |
| Future | Kubernetes-ready (see roadmap) |

---

## Getting Started

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- Git

### Run with Docker

```bash
git clone https://github.com/<your-username>/infraforge.git
cd infraforge
docker compose up --build
```

Open [http://localhost:3000](http://localhost:3000)

The first build takes a few minutes while Maven and npm resolve dependencies. Subsequent builds use Docker layer cache and are significantly faster.

### Run without Docker (local development)

You will need Java 21+, Node.js 20+, and a running PostgreSQL instance.

**Terminal 1 — Backend**
```bash
cd backend
./mvnw spring-boot:run
# API available at http://localhost:8080
```

**Terminal 2 — Frontend**
```bash
cd frontend
npm install
npm run dev
# UI available at http://localhost:3000
# /api calls are proxied to the backend automatically via Vite config
```

---

## Project Structure

```
infraforge/
├── backend/
│   ├── src/main/java/com/infraforge/
│   │   ├── api/                    REST controllers (provider endpoints)
│   │   │   └── v1/                 Versioned REST controllers (template endpoints)
│   │   ├── config/                 CORS, exception handler, request ID filter
│   │   ├── engine/                 Provider registry and template renderer
│   │   ├── model/                  JPA entities, DTOs, repositories
│   │   └── service/                Business logic (TemplateService)
│   ├── src/main/resources/
│   │   ├── application.yml         Base configuration
│   │   ├── application-dev.yml     Development profile
│   │   ├── application-prod.yml    Production profile
│   │   ├── db/migration/           Flyway SQL migrations
│   │   └── providers/
│   │       ├── terraform/
│   │       │   ├── schema.json
│   │       │   └── template.ftl
│   │       ├── ansible/
│   │       │   ├── schema.json
│   │       │   └── template.ftl
│   │       └── vagrant/
│   │           ├── schema.json
│   │           └── template.ftl
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── api/                    Backend API client (provider + template)
│   │   ├── components/             ProviderTabs, SchemaForm, CodePreview,
│   │   │                           FieldRenderer, TemplateDrawer
│   │   ├── hooks/                  useDebounce, useAutoSave
│   │   └── types/                  TypeScript interfaces matching backend model
│   ├── nginx.conf                  Production proxy config
│   └── Dockerfile
├── logs/                           Log files written by the backend (git-ignored)
├── docker-compose.yml
├── CHECKLIST.md
└── README.md
```

---

## API Reference

### Provider API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/providers` | List all registered providers |
| `GET` | `/api/providers/{id}/schema` | Full field schema for a provider |
| `POST` | `/api/providers/{id}/generate` | Submit form values, receive rendered template |
| `GET` | `/api/health` | Health check |

### Template API (v1)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/templates` | List all saved templates (summary) |
| `GET` | `/api/v1/templates?providerId={id}` | Filter by provider |
| `POST` | `/api/v1/templates` | Save a new template |
| `GET` | `/api/v1/templates/{id}` | Load a template with full form state |
| `PUT` | `/api/v1/templates/{id}` | Update an existing template |
| `DELETE` | `/api/v1/templates/{id}` | Delete a template |
| `POST` | `/api/v1/templates/{id}/duplicate` | Clone a template |

### Actuator

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/health` | Application and DB health |
| `GET` | `/actuator/info` | Build info |

---

## Adding a Provider

InfraForge uses a file-based plugin model. Every provider lives in its own folder under `backend/src/main/resources/providers/`. The backend scans this directory at startup and registers everything it finds.

**Steps to add a new provider (e.g. Pulumi):**

1. Create the folder:
```
backend/src/main/resources/providers/pulumi/
```

2. Add `schema.json` — defines sections, fields, types, options, and help text:
```json
{
  "id": "pulumi",
  "label": "Pulumi",
  "version": "1.0",
  "fileExtension": ".ts",
  "sections": [
    {
      "id": "project",
      "label": "Project",
      "optional": false,
      "repeatable": false,
      "fields": [
        {
          "id": "project_name",
          "label": "Project name",
          "type": "text",
          "required": true,
          "placeholder": "my-infra",
          "help": "Name of the Pulumi project."
        }
      ]
    }
  ]
}
```

3. Add `template.ftl` — a FreeMarker template referencing field IDs via the sections model:
```
<#assign s = sections>
<#assign proj = s.project.instances[0]>
import * as pulumi from "@pulumi/pulumi";

const projectName = "${proj.project_name}";
```

4. Restart the backend. The new provider appears in the UI tab bar automatically.

**No Java or TypeScript changes required.**

---

## Roadmap

| Phase | Status | Scope |
|---|---|---|
| 1 | ✅ Complete | Terraform AWS builder, live preview, Docker |
| 2 | ✅ Complete | Ansible (17 sections), Vagrant (19 sections), full provider coverage |
| 3 | 🔲 In Progress | Security hardening, observability, PostgreSQL, template management |
| 4 | Future | AI-assisted field suggestions (hook points already in place) |
| 5 | Future | Kubernetes deployment, multi-user support, template sharing |

---

## Branching Model

```
main        ← stable, tagged releases only
develop     ← integration branch for active development
feature/*   ← one branch per feature or task
```

```bash
# Start a new feature
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name

# Commit your work
git add .
git commit -m "feat: description of change"

# Push and merge into develop when ready
git push origin feature/your-feature-name
git checkout develop
git merge feature/your-feature-name
git push origin develop
```

Merge to `main` and tag on phase completion:
```bash
git checkout main
git merge develop
git tag -a v0.3.0 -m "Phase 3: template management and hardening"
git push origin main --tags
```

---

## License

MIT
