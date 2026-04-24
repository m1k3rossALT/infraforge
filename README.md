# InfraForge

**A UI-driven builder for Infrastructure as Code templates.**

InfraForge removes the syntax barrier from IaC tooling. Instead of hand-writing Terraform HCL, Ansible playbooks, or Vagrantfiles, you fill out a guided form and get a valid, ready-to-use configuration file — with a live code preview that updates as you type.

Built for engineers who know *what* infrastructure they want to build, but don't want to context-switch into documentation every time they need a new template.

---

## Features

- **Schema-driven form UI** — fields, dropdowns, and help text generated from a provider schema. No hardcoded forms.
- **Live code preview** — changes in the form reflect instantly in the output pane.
- **Copy & download** — grab the generated file with one click.
- **Plugin provider model** — adding a new IaC tool (Pulumi, Crossplane, AWS CDK, etc.) requires zero code changes. Drop a folder, restart.
- **Docker-first** — runs locally with a single command. No external dependencies.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, FreeMarker |
| Frontend | React 18, TypeScript, Vite, CodeMirror 6 |
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

You will need Java 21+ and Node.js 20+.

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
│   │   ├── api/                    REST controllers
│   │   ├── config/                 CORS and web config
│   │   ├── engine/                 Provider registry and template renderer
│   │   └── model/                  Schema POJOs and request/response models
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── providers/
│   │       └── terraform/
│   │           ├── schema.json     Field definitions, dropdowns, help text
│   │           └── template.ftl    FreeMarker output template
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── api/                    Backend API client
│   │   ├── components/             ProviderTabs, SchemaForm, CodePreview, FieldRenderer
│   │   ├── hooks/                  useDebounce
│   │   └── types/                  TypeScript interfaces matching backend model
│   ├── nginx.conf                  Production proxy config
│   └── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/providers` | List all registered providers |
| `GET` | `/api/providers/{id}/schema` | Full field schema for a provider |
| `POST` | `/api/providers/{id}/generate` | Submit values, receive rendered template |
| `POST` | `/api/providers/{id}/suggest` | AI field suggestions (reserved, no-op in v0.x) |
| `GET` | `/api/health` | Health check |

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

3. Add `template.ftl` — a FreeMarker template referencing field IDs as variables:
```
import * as pulumi from "@pulumi/pulumi";

const projectName = "${project_name}";
```

4. Restart the backend. The new provider appears in the UI tab bar automatically.

**No Java or TypeScript changes required.**

---

## Roadmap

| Phase | Status | Scope |
|---|---|---|
| 1 | ✅ Current | Terraform AWS builder, live preview, Docker |
| 2 | Planned | Ansible playbook builder, Vagrant provider |
| 3 | Planned | Save/load templates, local file management, export as zip |
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
git checkout -b feature/your-feature-name

# Commit your work
git add .
git commit -m "feat: description of change"

# Push and merge into develop when ready
git push origin feature/your-feature-name
```

Merge to `main` and tag on phase completion:
```bash
git checkout main
git merge develop
git tag -a v0.2.0 -m "Phase 2: Ansible and Vagrant providers"
git push origin main --tags
```

---

## License

MIT
