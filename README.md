# InfraForge

**A UI-driven builder for Infrastructure as Code templates.**

InfraForge removes the syntax barrier from IaC tooling. Instead of hand-writing Terraform HCL, Ansible playbooks, or Vagrantfiles, you fill out a guided form and get a valid, ready-to-use configuration file — with a live code preview that updates as you type.

Built for engineers who know *what* infrastructure they want to build, but don't want to context-switch into documentation every time they need a new template.

---

## Features

- **Schema-driven form UI** — fields, dropdowns, and help text generated from a provider schema. No hardcoded forms.
- **Live code preview** — changes in the form reflect instantly in the output pane.
- **Authentication** — register, login, and JWT-based sessions. Guest mode for generate/preview; auth required to save templates.
- **Template library** — save, load, duplicate, delete, export, and import named templates. Auto-saves after 30 seconds of inactivity.
- **Template sharing** — generate a public share link for any saved template. Recipients see a read-only code view with no account required.
- **AI-assisted form filling** — describe what you want to build in plain English and let AI fill the form. Supports Gemini, OpenAI, Claude, Mistral, and Groq via your own API key (BYOK).
- **Copy & download** — grab the generated file with one click.
- **Plugin provider model** — adding a new IaC tool requires zero code changes. Drop a folder, restart.
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

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `INFRAFORGE_JWT_SECRET` | Prod | dev fallback | JWT signing secret — min 32 chars |
| `INFRAFORGE_AI_ENCRYPTION_KEY` | Prod | dev fallback | AES-256 key for stored API keys — exactly 32 chars |
| `INFRAFORGE_CORS_ORIGINS` | Prod | localhost | Comma-separated allowed origins |
| `DB_URL` | Prod | docker default | PostgreSQL JDBC URL |
| `DB_USERNAME` | Prod | docker default | PostgreSQL username |
| `DB_PASSWORD` | Prod | docker default | PostgreSQL password |

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
| `POST` | `/api/v1/templates/{id}/share` | Generate a public share token |
| `DELETE` | `/api/v1/templates/{id}/share` | Revoke share token |

### Shared View API (public)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/shared/{token}` | Fetch a shared template by token (no auth required) |

### AI API (v1)

> All endpoints require a valid `Authorization: Bearer <token>` header.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/ai/suggest/{providerId}` | Generate field suggestions from a natural language description |
| `GET` | `/api/v1/ai/settings` | Get current AI configuration (key never returned) |
| `PUT` | `/api/v1/ai/settings` | Save AI provider and API key |
| `DELETE` | `/api/v1/ai/settings` | Remove AI configuration |
| `GET` | `/api/v1/ai/providers` | List available AI provider names |

### Actuator

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/health` | Application and DB health |
| `GET` | `/actuator/info` | Build info |

---

## AI Feature (BYOK)

InfraForge supports AI-assisted form filling via your own API key. No platform key required.

**Supported providers:**

| Provider | Default model |
|---|---|
| Google Gemini | gemini-2.0-flash |
| OpenAI | gpt-4o-mini |
| Anthropic Claude | claude-haiku-4-5-20251001 |
| Mistral | mistral-small-latest |
| Groq (Llama) | llama-3.3-70b-versatile |

**How it works:**
1. Sign in and open Settings (⚙ icon in the top bar)
2. Go to the AI Provider tab, select your provider, and paste your API key
3. The key is encrypted with AES-256-GCM and stored server-side — never returned after saving
4. Click the ✨ bar above the form, describe what you want to build, and hit Fill
5. The form fills automatically and the live preview updates instantly

Rate limit: 10 suggestions per user per minute.

---

## Adding a Provider

InfraForge uses a file-based plugin model. Every provider lives in its own folder under `backend/src/main/resources/providers/`. The backend scans this directory at startup and registers everything it finds.

**Steps to add a new provider (e.g. Pulumi):**

1. Create the folder:
```
backend/src/main/resources/providers/pulumi/
```

2. Add `schema.json` — defines sections, fields, types, options, help text, and optional AI hints:
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
      "aiHint": "Configures the Pulumi project name and runtime.",
      "fields": [
        {
          "id": "project_name",
          "label": "Project name",
          "type": "text",
          "required": true,
          "placeholder": "my-infra",
          "help": "Name of the Pulumi project.",
          "aiHint": "The Pulumi project identifier used in Pulumi.yaml."
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
| 4 | ✅ Complete | AI-assisted form filling, BYOK (Gemini/OpenAI/Claude/Mistral/Groq), settings drawer |
| 5a | ✅ Complete | Spring Security, JWT auth, BCrypt, guest mode, template ownership |
| 5b | ✅ Complete | Template sharing (share links, revocation, read-only public view) |
| 5c | ✅ Complete | Dependency audit (npm audit, OWASP Maven plugin) |
| 5d | Future | Kubernetes (Helm chart, HPA, Ingress) |
| 7 | Planned | Kubernetes YAML, Dockerfile, and Docker Compose providers |
| 6 | Backlog | Subscription tiers, Stripe, platform-managed AI |

---

## License

MIT