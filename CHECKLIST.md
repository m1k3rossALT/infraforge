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
> Status: ✅ Complete — tagged v0.3.0

### 3a — Security fixes ✅
- [x] 🔒 Fix directory traversal — validate provider ID against registry whitelist before
      passing to file path in TemplateRenderer
- [x] 🔒 Global exception handler (@ControllerAdvice) — strip internal stack traces from
      API error responses, return consistent { error, message, timestamp } JSON
- [x] 🔒 Request body size limit — set spring.servlet.multipart.max-request-size and
      server.tomcat.max-http-form-post-size in application.yml
- [x] 🔒 Input validation — add @Valid + @Size / @NotBlank constraints on
      GenerateRequest fields, return 400 with field-level errors
- [x] 🔒 Lock CORS to explicit allowed origins via environment variable (INFRAFORGE_CORS_ORIGINS)

### 3b — Observability ✅
- [x] Logback config (logback-spring.xml) — rolling file appender, 30-day retention,
      separate error log file, async writers
- [x] Spring Boot Actuator — health and info endpoints exposed, all others locked down
- [x] Correlation/request ID — MDC filter (RequestIdFilter) generates UUID per request,
      included in every log line, echoed in X-Request-ID response header
- [x] Environment profiles — application-dev.yml and application-prod.yml created,
      environment-specific values separated from base config
- [x] Graceful shutdown — server.shutdown=graceful and lifecycle timeout configured

### 3c — Database ✅
- [x] PostgreSQL 16 for both dev and prod (chosen over H2 for consistency)
- [x] PostgreSQL container added to docker-compose.yml with health check and named volume
- [x] Configure datasource in application-dev.yml and application-prod.yml
- [x] Flyway migration setup — V1__baseline_schema.sql with templates table, indexes,
      JSONB form_state column, auto-updated updated_at trigger
- [x] Template JPA entity with UUID primary key, JSONB form_state, tags array
- [x] TemplateRepository with provider-filtered and date-sorted queries
- [x] TemplateService — save, update, find all, find by id, delete, duplicate

### 3d — Template management features ✅
- [x] POST /api/v1/templates — create a new saved template
- [x] PUT /api/v1/templates/{id} — update existing template (used by auto-save)
- [x] GET /api/v1/templates — list all templates (summary only, no formState)
- [x] GET /api/v1/templates/{id} — load a single template with full form state
- [x] DELETE /api/v1/templates/{id} — delete a template with confirmation UI
- [x] POST /api/v1/templates/{id}/duplicate — clone with "Copy of <n>"
- [x] Frontend — sliding left drawer (TemplateDrawer) with template library
- [x] Frontend — document title field in top bar, triggers save when named
- [x] Frontend — manual Save button + "Saved" feedback
- [x] Frontend — auto-save after 30s inactivity when template is named (useAutoSave hook)
- [x] Frontend — "Unsaved changes" / "Saved" status indicator in top bar
- [x] Frontend — load restores full form state and regenerates live preview
- [x] Frontend — duplicate shows "Duplicated" feedback, refreshes library list
- [x] Frontend — delete with two-step confirmation (confirm / cancel)
- [x] Export as zip — download template file + metadata JSON in a single archive
- [x] Import — upload an existing .tf / .yml / Vagrantfile and pre-fill the form

### 3e — API versioning ✅
- [x] Template endpoints versioned under /api/v1/
- [x] Provider endpoints migrated from /api/ to /api/v1/
- [x] nginx proxy config updated to match v1 routes
- [x] Versioning policy documented in README

---

## Phase 4 — AI-Assisted Field Suggestions
> Status: 🔲 Future

### 4a — Infrastructure
- [ ] Choose LLM provider (OpenAI, Anthropic, local Ollama) and add client dependency
- [ ] Add API key config to application-prod.yml (never hardcoded)
- [ ] Implement EnhancementService interface — no-op in dev profile, real impl in prod
- [ ] Add POST /api/v1/providers/{id}/suggest — accepts partial form state,
      returns AI-suggested field values
- [ ] Rate limiting on suggest endpoint — Bucket4j, per-session limit

### 4b — Frontend
- [ ] "Suggest" button per section — calls suggest endpoint with current form state
- [ ] Suggestion diff view — show suggested values highlighted before applying
- [ ] Accept / reject per field
- [ ] Loading state and graceful degradation when AI is unavailable

### 4c — Schema integration
- [ ] aiHint field in schema.json activated — included in prompt context
- [ ] Natural language input field — "describe what you want to build" pre-fills form

---

## Phase 5 — Multi-User, Auth & Kubernetes
> Status: 🔲 In Progress

### 5a — Authentication & authorisation 🔒
- [x] 🔒 Spring Security dependency added
- [x] 🔒 JWT-based authentication — login endpoint, token issue and validation
- [x] 🔒 Refresh token flow — rotation on every refresh, revocation on logout
- [x] 🔒 BCrypt password hashing (cost factor 12)
- [x] 🔒 Stateless session (STATELESS policy, CSRF disabled — correct for JWT APIs)
- [x] 🔒 Endpoint protection — /api/v1/templates/** requires auth; providers + generate
      are public (guest mode)
- [x] 🔒 JSON 401/403 responses — no HTML redirects to break the React SPA
- [x] 🔒 V2 migration — users + refresh_tokens tables, user_id FK on templates
- [x] 🔒 V3 migration — fix user_role column type (VARCHAR replaces PostgreSQL native
      ENUM to resolve JDBC prepared statement cast failure on register)
- [x] 🔒 GET /api/v1/templates/{id} ownership check — 403 if caller is not the owner
      (templates with null user_id are accessible — backward compat with Phase 3 data)
- [x] 🔒 Export endpoint ownership check — same rule as getById
- [x] Frontend — AuthContext with in-memory access token (never localStorage)
- [x] Frontend — refresh token persisted in sessionStorage (survives page reload)
- [x] Frontend — silent refresh on app load (reads sessionStorage on mount)
- [x] Frontend — 401 interceptor in API client — transparent token refresh + retry
- [x] Frontend — AuthModal (login / register toggle)
- [x] Frontend — Sign in / Sign out in top bar, user email displayed
- [x] Frontend — auto-save and template library gated on isAuthenticated
- [ ] 🔒 RBAC — roles currently wired (VIEWER / EDITOR / ADMIN) but not enforced
      at the endpoint level — add @PreAuthorize guards before v1.0
- [ ] 🔒 HTTPS enforced — HTTP to HTTPS redirect in nginx
- [ ] 🔒 Security headers — HSTS, X-Frame-Options, Content-Security-Policy in nginx
- [ ] 🔒 Move refresh token from sessionStorage to httpOnly SameSite=Strict cookie
      (requires backend Set-Cookie on login/refresh, cookie-based logout endpoint;
      deferred from v0.5.0 — acceptable for dev/staging, required before public launch)

### 5b — Multi-user features
- [ ] Templates scoped to user — enforced (listAll and getById already done in 5a)
- [ ] Template sharing — GET /api/v1/shared/{shareToken} public read-only endpoint
- [ ] Share link generation — POST /api/v1/templates/{id}/share, stores UUID token
- [ ] Share link revocation — DELETE /api/v1/templates/{id}/share
- [ ] V5 migration — add share_token UUID column to templates (nullable)
- [ ] Frontend — "Share" button on saved templates, copies link to clipboard
- [ ] Frontend — /shared/:token route, read-only builder view
- [ ] Team workspace — shared template library within a team (future)

### 5c — Dependency security audit
- [x] npm audit integrated into frontend Docker build — fail on high severity
- [x] mvn dependency-check:check (OWASP) added to backend build pipeline
- [x] Dependabot or Renovate configured on GitHub repo for automated dependency PRs

### 5d — Kubernetes
- [ ] Helm chart scaffolded — backend Deployment + Service, frontend Deployment + Service
- [ ] ConfigMap for application.yml values
- [ ] Secret for database credentials and API keys
- [ ] Horizontal Pod Autoscaler for backend
- [ ] Ingress controller config (nginx ingress)
- [ ] Liveness and readiness probes wired to /actuator/health
- [ ] PersistentVolumeClaim for PostgreSQL
- [ ] Resource requests and limits defined on all containers

---

## Ongoing — applies to every phase

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
| 3 | All 3d and 3e items complete, export/import working, provider routes on v1 |
| 4 | Suggest endpoint live, frontend diff view working, graceful degradation tested |
| 5 | Auth flow complete, all endpoints protected, Helm chart deploys cleanly to local k8s |