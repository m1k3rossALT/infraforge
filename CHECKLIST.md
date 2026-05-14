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

## Phase 4 — AI-Assisted Field Suggestions (BYOK)
> Status: ✅ Complete — tagged v0.7.0

### 4a — Backend infrastructure ✅
- [x] Add `bucket4j-core` to pom.xml — in-memory rate limiting
- [x] V5 migration — add `ai_provider`, `ai_api_key_enc`, `ai_model` columns to users table
- [x] AiKeyEncryptionService — AES-256-GCM encryption/decryption of stored API keys
- [x] EnhancementService interface — provider-agnostic contract
- [x] NoOpEnhancementService — safe fallback, returns empty map
- [x] GeminiEnhancementService — Gemini REST API (gemini-2.0-flash default)
- [x] OpenAiEnhancementService — OpenAI Chat Completions API (gpt-4o-mini default)
- [x] AnthropicEnhancementService — Anthropic Messages API (claude-haiku default)
- [x] MistralEnhancementService — Mistral API (mistral-small-latest default)
- [x] GroqEnhancementService — Groq API (llama-3.3-70b default)
- [x] OpenAiCompatibleEnhancementService — abstract base for OpenAI-format providers
- [x] EnhancementServiceRouter — auto-discovers all provider beans, routes by name
- [x] PromptBuilder — provider-agnostic structured prompt from schema + description
- [x] AiRateLimiterService — Bucket4j per-user 10 req/min in-memory
- [x] POST /api/v1/ai/suggest/{providerId} — auth-required, rate limited
- [x] GET/PUT/DELETE /api/v1/ai/settings — save/retrieve/remove encrypted API key
- [x] GET /api/v1/ai/providers — list available provider names
- [x] SecurityConfig updated — /api/v1/ai/** requires authentication

### 4b — Schema integration
- [x] `aiHint` field added to Section and Field models in ProviderSchema.java
- [ ] Add `aiHint` values to terraform schema.json
- [ ] Add `aiHint` values to ansible schema.json
- [ ] Add `aiHint` values to vagrant schema.json

### 4c — Settings drawer ✅
- [x] SettingsDrawer component — slides in from the right, tabbed structure
- [x] Settings icon (⚙) in top-right bar
- [x] Profile tab — email display
- [x] AI Provider tab — provider dropdown, API key input (masked after save),
      model selector, Test connection button, Remove key action
- [x] API key masked after save — raw key never stored or displayed in frontend

### 4d — Natural language input bar ✅
- [x] AiBar component — collapsible above the form, collapsed by default
- [x] Shows configured provider badge or "Set up →" if not configured
- [x] Opens SettingsDrawer at AI tab if no key configured
- [x] Calls POST /api/v1/ai/suggest on submit
- [x] Merges suggestions into form state — enables sections with suggestions
- [x] FieldRenderer highlight — filled fields briefly highlighted with ✨
- [x] Live preview regenerates automatically from state change
- [x] Graceful degradation — inline error on failure, form remains functional
- [x] Rate limit error shown clearly (429)
- [x] Keyboard shortcut ⌘↵ / Ctrl+↵ to submit

### 4e — Docs ✅
- [x] AI API endpoints added to README
- [x] AI Feature (BYOK) section added to README
- [x] Environment variables table added to README
- [x] aiHint documented in Adding a Provider section

---

## Phase 5 — Multi-User, Auth & Kubernetes
> Status: ✅ Complete (5a, 5b, 5c) — tagged v0.5.0 / v0.6.0

### 5a — Authentication & authorisation ✅
- [x] 🔒 Spring Security dependency added
- [x] 🔒 JWT-based authentication — login endpoint, token issue and validation
- [x] 🔒 Refresh token flow — rotation on every refresh, revocation on logout
- [x] 🔒 BCrypt password hashing (cost factor 12)
- [x] 🔒 Stateless session (STATELESS policy, CSRF disabled — correct for JWT APIs)
- [x] 🔒 Endpoint protection — /api/v1/templates/** requires auth; providers + generate
      are public (guest mode)
- [x] 🔒 JSON 401/403 responses — no HTML redirects to break the React SPA
- [x] 🔒 V2 migration — users + refresh_tokens tables, user_id FK on templates
- [x] 🔒 V3 migration — fix user_role column type (VARCHAR replaces PostgreSQL native ENUM)
- [x] 🔒 GET /api/v1/templates/{id} ownership check — 403 if caller is not the owner
- [x] 🔒 Export endpoint ownership check — same rule as getById
- [x] Frontend — AuthContext with in-memory access token (never localStorage)
- [x] Frontend — refresh token persisted in sessionStorage (survives page reload)
- [x] Frontend — silent refresh on app load
- [x] Frontend — 401 interceptor in API client — transparent token refresh + retry
- [x] Frontend — AuthModal (login / register toggle)
- [x] Frontend — Sign in / Sign out in top bar, user email displayed
- [x] Frontend — auto-save and template library gated on isAuthenticated
- [ ] 🔒 RBAC — roles wired but not enforced — add @PreAuthorize guards before v1.0
- [ ] 🔒 HTTPS enforced — HTTP to HTTPS redirect in nginx
- [ ] 🔒 Security headers — HSTS, X-Frame-Options, Content-Security-Policy in nginx
- [ ] 🔒 Move refresh token from sessionStorage to httpOnly SameSite=Strict cookie

### 5b — Multi-user features ✅
- [x] Templates scoped to user — enforced (listAll and getById check ownership)
- [x] Template sharing — GET /api/v1/shared/{shareToken} public read-only endpoint
- [x] Share link generation — POST /api/v1/templates/{id}/share
- [x] Share link revocation — DELETE /api/v1/templates/{id}/share
- [x] V4 migration — add share_token UUID column to templates
- [x] Frontend — Share/Unshare/Copy link buttons in TemplateDrawer
- [x] Frontend — /shared/:token route — SharedView read-only code preview
- [x] Frontend — "Shared" badge on shared templates in library
- [x] main.tsx path-based routing — /shared/* renders without AuthProvider
- [ ] Team workspace — shared template library within a team (deferred to Phase 6+)

### 5c — Dependency security audit ✅
- [x] npm audit integrated into frontend Docker build — fail on high severity
- [x] mvn dependency-check:check (OWASP) added to backend build pipeline
- [x] Dependabot config removed — security alerts remain active via GitHub repo settings

### 5d — Kubernetes deployment
> Status: 🔲 Future

- [ ] Helm chart scaffolded — backend Deployment + Service, frontend Deployment + Service
- [ ] ConfigMap for application.yml values
- [ ] Secret for database credentials and API keys
- [ ] Horizontal Pod Autoscaler for backend
- [ ] Ingress controller config (nginx ingress)
- [ ] Liveness and readiness probes wired to /actuator/health
- [ ] PersistentVolumeClaim for PostgreSQL
- [ ] Resource requests and limits defined on all containers

---

## Phase 7 — Extended Providers (Kubernetes, Docker)
> Status: 🔲 Planned

### 7a — Kubernetes YAML provider
- [ ] kubernetes/schema.json — sections: Deployment, Service, ConfigMap, Secret,
      Ingress, PersistentVolumeClaim, HorizontalPodAutoscaler, Namespace, ServiceAccount
- [ ] kubernetes/template.ftl — multi-resource output separated by ---
- [ ] aiHint values added to all sections and fields
- [ ] Validated against real kubectl apply

### 7b — Dockerfile provider
- [ ] dockerfile/schema.json — sections: base image, working directory, environment
      variables, exposed ports, copy instructions, run commands, entrypoint/cmd
- [ ] dockerfile/template.ftl — generates valid Dockerfile
- [ ] fileExtension set appropriately so output file is named Dockerfile
- [ ] aiHint values added
- [ ] Validated with docker build

### 7c — Docker Compose provider
- [ ] docker-compose/schema.json — sections: services (repeatable), networks, volumes,
      environment variables per service, port mappings, depends_on, health checks
- [ ] docker-compose/template.ftl — generates valid docker-compose.yml
- [ ] aiHint values added
- [ ] Validated with docker compose up

### 7d — Docs
- [ ] README roadmap updated
- [ ] New providers documented in Adding a Provider section

---

## Phase 6 — Subscription & Managed AI
> Status: 🔲 Backlog — not started

### 6a — Subscription infrastructure
- [ ] Stripe integration — checkout session, webhook handler, subscription lifecycle
- [ ] V_migration — add `subscription_status` and `subscription_id` to users table
- [ ] SubscriptionService — create, cancel, check active subscription
- [ ] Webhook endpoint POST /api/v1/webhooks/stripe
- [ ] 🔒 Webhook signature verification — validate Stripe-Signature header

### 6b — Subscription tiers
- [ ] Free tier — template save/load/share, no AI
- [ ] Pro tier — all free features + platform-managed AI
- [ ] Middleware — check subscription before allowing suggest calls; return 402 if not pro
- [ ] Grace period — 3 days after payment failure before locking AI access

### 6c — Platform-managed AI
- [ ] Platform API keys stored in environment variables (not per-user)
- [ ] EnhancementServiceRouter updated — pro users use platform key, BYOK still available
- [ ] Usage metering — log AI call count per user per day for billing audit

### 6d — Frontend
- [ ] Upgrade prompt when non-pro user tries AI
- [ ] Billing page — current plan, next billing date, cancel subscription
- [ ] Stripe Checkout redirect flow
- [ ] Post-upgrade redirect with success banner

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
| 4 | Suggest endpoint live, AI bar fills form, settings drawer working, graceful degradation tested |
| 5 | Auth flow complete, share links working, all owned endpoints protected |
| 6 | Stripe webhooks tested, subscription gating verified, pro tier AI working end to end |
| 7 | All three providers validated with real tool CLIs, aiHint values complete |