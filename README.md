# InfraForge

**A UI-driven builder for Infrastructure as Code templates.**

InfraForge removes the syntax barrier from IaC tooling. Instead of hand-writing Terraform HCL, Ansible playbooks, or Vagrantfiles, you fill out a guided form and get a valid, ready-to-use configuration file — with a live code preview that updates as you type.

Built for engineers who know *what* infrastructure they want to build, but don't want to context-switch into documentation every time they need a new template.

---

## Features

- **Schema-driven form UI** — fields, dropdowns, and help text generated from a provider schema. No hardcoded forms.
- **Live code preview** — changes in the form reflect instantly in the output pane.
- **Authentication** — register, login, and JWT-based sessions. Guest mode for generate/preview; auth required to save templates.
- **Template library** — save, load, duplicate, and delete named templates. Auto-saves after 30 seconds of inactivity.
- **Copy & download** — grab the generated file with one click.
- **Plugin provider model** — adding a new IaC tool (Pulumi, Crossplane, AWS CDK, etc.) requires zero code changes. Drop a folder, restart.
- **Docker-first** — runs locally with a single command. No external dependencies.

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

## API Reference

### Auth API (v1)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Create a new account |
| `POST` | `/api/v1/auth/login` | Login, returns access + refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Exchange refresh token for a new access token |
| `POST` | `/api/v1/auth/logout` | Revoke refresh token |
| `GET` | `/api/v1/auth/me` | Returns current authenticated user |

### Provider API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/providers` | List all registered providers |
| `GET` | `/api/v1/providers/{id}/schema` | Full field schema for a provider |
| `POST` | `/api/v1/providers/{id}/generate` | Submit form values, receive rendered template |

### Template API (v1)

> All endpoints require a valid `Authorization: Bearer <token>` header.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/templates` | List saved templates for the authenticated user |
| `GET` | `/api/v1/templates?providerId={id}` | Filter by provider |
| `POST` | `/api/v1/templates` | Save a new template |
| `GET` | `/api/v1/templates/{id}` | Load a template with full form state |
| `PUT` | `/api/v1/templates/{id}` | Update an existing template |
| `DELETE` | `/api/v1/templates/{id}` | Delete a template |
| `POST` | `/api/v1/templates/{id}/duplicate` | Clone a template |
| `GET` | `/api/v1/templates/{id}/export` | Download template as a zip archive |
| `POST` | `/api/v1/templates/import` | Import a `.tf`, `.yml`, or `Vagrantfile` |

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
| 3 | ✅ Complete | Security hardening, observability, PostgreSQL, template management |
| 4 | Future | AI-assisted field suggestions (hook points already in place) |
| 5a | ✅ Complete | Spring Security, JWT auth, BCrypt, guest mode, template ownership |
| 5b | ✅ Complete | Template sharing (share links), multi-user scoping |
| 5c | ✅ Complete | Dependency audit (npm audit, OWASP Maven plugin) |
| 5d | Future | Kubernetes (Helm chart, HPA, Ingress) |

---

## License

MIT