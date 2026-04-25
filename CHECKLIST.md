# InfraForge — Master Development Checklist

This document tracks every planned feature, enhancement, and production-readiness task across all phases.
Each item is assigned to the phase where it makes the most logical sense — either because it is a
dependency for that phase, or because the risk of not having it grows significantly at that point.

---

## How to use this

- Check off items as they are completed and committed
- If something breaks, look at the last checked item in the current phase — that is the change to investigate
- Do not skip items within a phase without a documented reason
- Security and stability items marked 🔒 should not be deferred beyond their assigned phase

---

## Phase 1 — Foundation & Terraform Builder
> Status: ✅ Complete — tagged v0.1.0

- [x] Spring Boot 3 backend scaffold
- [x] React 18 + TypeScript + Vite frontend scaffold
- [x] Provider plugin model (schema.json + template.ftl per provider)
- [x] Schema-driven dynamic form rendering
- [x] Live code preview (CodeMirror, read-only)
- [x] Debounced auto-generation on form change
- [x] Copy and download generated file
- [x] Terraform AWS provider — initial schema and template
- [x] Docker + Docker Compose setup
- [x] nginx reverse proxy (frontend → backend API)
- [x] Basic SLF4J logging in controller and engine
- [x] CORS config for local development
- [x] README with setup instructions
- [x] Git repo initialised, .gitignore configured (target/ excluded)

---

## Phase 2 — Ansible & Vagrant Providers
> Status: ✅ Complete — tagged v0.2.0

- [x] Optional section model (toggle on/off)
- [x] Repeatable section model (multiple instances)
- [x] Toggle and number field types added to frontend
- [x] Section instance cards with add/remove UI
- [x] Terraform schema expanded — 15 sections, ~105 fields
- [x] Ansible provider — 17 sections, 116 fields
- [x] Vagrant provider — 19 sections, 143 fields (4 hypervisor providers)
- [x] All provider templates rewritten for sections-based data model
- [x] Duplicate FreeMarker bean conflict resolved
- [x] Null-safe field access pattern established in templates (`(field!"")?method`)
- [x] target/ removed from Git tracking
- [x] README updated — roadmap, project structure, provider model example
- [x] Branching model established (main / develop / feature/*)

---

## Phase 3 — Template Management & Hardening
> Status: 🔲 Planned

This phase has two tracks running in parallel: the primary feature (save/load) and the
engineering hardening that must be in place before the app handles persisted user data.

### 3a — Security fixes (do these first, before any persistence work) 🔒

- [ ] 🔒 Fix directory traversal — validate provider ID against registry whitelist before
      passing to file path in TemplateRenderer
- [ ] 🔒 Global exception handler (`@ControllerAdvice`) — strip internal stack traces from
      API error responses, return consistent `{ error, message, timestamp }` JSON
- [ ] 🔒 Request body size limit — set `spring.servlet.multipart.max-request-size` and
      `server.tomcat.max-http-form-post-size` in application.yml
- [ ] 🔒 Input validation — add `@Valid` + `@Size` / `@NotBlank` constraints on
      GenerateRequest fields, return 400 with field-level errors
- [ ] 🔒 Lock CORS to explicit allowed origins (not wildcard localhost:*) in WebConfig

### 3b — Observability

- [ ] Logback config (`logback-spring.xml`) — structured JSON logging, rolling file appender,
      30-day retention, separate error log file
- [ ] Spring Boot Actuator — add dependency, expose `/actuator/health` and `/actuator/info`,
      lock down all other actuator endpoints
- [ ] Correlation/request ID — MDC filter that generates a UUID per request and includes it
      in every log line for that request
- [ ] Environment profiles — create `application-dev.yml` and `application-prod.yml`,
      move environment-specific values out of the base config
- [ ] Graceful shutdown — set `server.shutdown=graceful` and
      `spring.lifecycle.timeout-per-shutdown-phase=20s` in application.yml

### 3c — Database

- [ ] Add H2 (dev) and PostgreSQL (prod) dependencies to pom.xml
- [ ] Configure datasource in application-dev.yml and application-prod.yml
- [ ] Flyway migration setup — baseline schema for template storage
- [ ] Template entity and JPA repository
- [ ] TemplateService — save, find all, find by id, delete, duplicate

### 3d — Template management features

- [ ] `POST /api/templates` — save a generated template with a name and provider ID
- [ ] `GET /api/templates` — list all saved templates (id, name, provider, created date)
- [ ] `GET /api/templates/{id}` — load a saved template (returns form state)
- [ ] `DELETE /api/templates/{id}` — delete a saved template
- [ ] `POST /api/templates/{id}/duplicate` — clone a template with a new name
- [ ] Frontend — template library panel (list, open, duplicate, delete)
- [ ] Frontend — save dialog (name input, provider shown, confirm)
- [ ] Frontend — load restores form state and regenerates live preview
- [ ] Export as zip — download template file + a metadata JSON in a single archive
- [ ] Import — upload an existing .tf / .yml / Vagrantfile and pre-fill the form

### 3e — API versioning (do before adding template endpoints)

- [ ] Rename all routes from `/api/*` to `/api/v1/*`
- [ ] Update nginx proxy config to match
- [ ] Update frontend API client base path
- [ ] Document versioning policy in README

---

## Phase 4 — AI-Assisted Field Suggestions
> Status: 🔲 Future

Hook points were designed into the architecture in Phase 1. This phase activates them.

### 4a — Infrastructure

- [ ] Choose LLM provider (OpenAI, Anthropic, local Ollama) and add client dependency
- [ ] Add API key config to application-prod.yml (never hardcoded)
- [ ] Implement `EnhancementService` interface — no-op in dev profile, real impl in prod
- [ ] Add `POST /api/v1/providers/{id}/suggest` — accepts partial form state,
      returns AI-suggested field values
- [ ] Rate limiting on suggest endpoint — Bucket4j, per-session limit

### 4b — Frontend

- [ ] "Suggest" button per section — calls suggest endpoint with current form state
- [ ] Suggestion diff view — show suggested values highlighted before applying
- [ ] Accept / reject per field
- [ ] Loading state and graceful degradation when AI is unavailable

### 4c — Schema integration

- [ ] `aiHint` field in schema.json activated — included in prompt context
- [ ] Natural language input field — "describe what you want to build" → pre-fills form

---

## Phase 5 — Multi-User, Auth & Kubernetes
> Status: 🔲 Future

### 5a — Authentication & authorisation 🔒

- [ ] 🔒 Spring Security dependency added
- [ ] 🔒 JWT-based authentication — login endpoint, token issue and validation
- [ ] 🔒 Refresh token flow
- [ ] 🔒 RBAC — roles: viewer, editor, admin
- [ ] 🔒 Endpoint protection — all `/api/v1/*` routes require valid token
- [ ] 🔒 CSRF protection enabled
- [ ] 🔒 Password hashing — BCrypt for stored credentials
- [ ] 🔒 HTTPS enforced — HTTP → HTTPS redirect in nginx
- [ ] 🔒 Security headers — HSTS, X-Frame-Options, Content-Security-Policy in nginx config

### 5b — Multi-user features

- [ ] User registration and login UI
- [ ] Templates scoped to user — users only see their own templates
- [ ] Template sharing — share a template read-only via a link
- [ ] Team workspace — shared template library within a team

### 5c — Dependency security audit

- [ ] `npm audit` integrated into frontend Docker build — fail on high severity
- [ ] `mvn dependency-check:check` (OWASP) added to backend build pipeline
- [ ] Dependabot or Renovate configured on GitHub repo for automated dependency PRs

### 5d — Kubernetes

- [ ] Helm chart scaffolded — backend Deployment + Service, frontend Deployment + Service
- [ ] ConfigMap for application.yml values
- [ ] Secret for database credentials and API keys
- [ ] Horizontal Pod Autoscaler for backend
- [ ] Ingress controller config (nginx ingress)
- [ ] Liveness and readiness probes wired to `/actuator/health`
- [ ] PersistentVolumeClaim for PostgreSQL
- [ ] Resource requests and limits defined on all containers

---

## Ongoing — applies to every phase

These are not phase-specific. They apply from now on every time a feature branch is merged.

- [ ] No secrets or credentials committed to Git (enforce with pre-commit hook or GitHub secret scanning)
- [ ] Docker build passes cleanly before merging any branch
- [ ] New providers must include both schema.json and template.ftl — partial additions are not merged
- [ ] All FreeMarker field references use null-safe defaults (`(field!"")?...`)
- [ ] README updated when a phase completes (roadmap status, structure, API reference)
- [ ] Git tag created on every phase completion (`vX.Y.0`)

---

## Phase completion criteria

| Phase | Merge to main when... |
|---|---|
| 3 | Security fixes 3a done, template save/load working end-to-end, logs writing to file |
| 4 | Suggest endpoint live, frontend diff view working, graceful degradation tested |
| 5 | Auth flow complete, all endpoints protected, Helm chart deploys cleanly to local k8s |