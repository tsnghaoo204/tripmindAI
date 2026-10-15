# System Overview — TripMind AI Travel Planner (internal evidence)

Scope: current working tree of `/mnt/hdd2tb/haots/repos/tripmindAI` on branch `main` (HEAD `3a6180c`) **including uncommitted changes** (Blog recap, community trip copy, `TripMap.tsx`, Leaflet, mobile header, AddActivityModal rewrite).
Evidence tags: **VERIFIED** (read in code/config), **PARTIAL** (partly confirmed / some caveat), **INFERRED** (deduced from code, not executed), **UNVERIFIED** (claimed only in docs or not checkable).
Secrets in `.env` / `application*.yml` are never copied: shown as `<redacted>`.

---

## 1. Problem the system solves

| Claim | Tag | Evidence |
|---|---|---|
| Web platform to plan, optimize and manage trips: create trip → itinerary per day → activities with places → budget/expenses → checklist → weather → AI assistant that proposes changes the user must approve. | VERIFIED | `TripMind_AI_Travel_Planner.md` §1–2; controllers under `backend/src/main/java/com/tripmind/controllers/` |
| Differentiator claimed by design doc: AI is an **Agent with tool calling**, not a chatbot; it reads trip data, calls business tools, and writes changes only through human-approved *proposals*. | VERIFIED (implemented) | `services/ai/AgentService.java` (own agent loop, `internalToolExecutionEnabled(false)`), `ToolRegistry`, `ProposalService`, `ProposalApplyService` |
| Anti-hallucination rule: every place the AI uses must be a real place returned by a search (Google Places) or already in DB; invented IDs are rejected (BR-509). | VERIFIED | `services/ai/PlaceVerifier.java`, `ItineraryGenerationService.verify/findReal/nameMatch` (MIN_NAME_MATCH = 0.5), test `ProposalApiTest.madeUpPlaceIsRejected`, `GenerationApiTest.generateItinerary` |
| Trip lifecycle in three phases BEFORE / DURING / AFTER (derived from dates in the destination timezone, or manually overridden). | VERIFIED | `enums/TripPhase.java`, `services/TripClock.java`, `TripController.updatePhase` |
| Community (blog) layer: share experiences, upvote, comment; admin distills posts into a "destination knowledge" dataset that is fed back into AI prompts. | VERIFIED | `BlogController`, `AdminBlogController`, `BlogService.extractKnowledgeFromPost/convertPostToKnowledge`, `DestinationKnowledgeService.getFormattedKnowledgeForPrompt`, tool `search_community_tips` |
| UI language is Vietnamese; domain targeted at Vietnamese travellers (VND default currency, "home countries" VN list, Vietnamese prompts). | VERIFIED | `application.yml` `tripmind.checklist.home-countries`, `TripEntity.currency` default `VND`, `prompts/*.md` |

Project title (from design doc): "AI Travel Planner – Intelligent Travel Planning & Trip Management Platform" / "Xây dựng nền tảng lập kế hoạch và quản lý chuyến du lịch thông minh sử dụng AI Agent và các dịch vụ API bên thứ ba". (VERIFIED — `TripMind_AI_Travel_Planner.md` lines 3–9.)

---

## 2. User roles / actors

| Actor | How defined | Tag | Evidence |
|---|---|---|---|
| **Guest (anonymous)** | No token. Backend allows only `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/health`, `GET /api/blog/**` (read posts/comments/destinations), Swagger, `/actuator/health`. | VERIFIED (backend) | `SecurityConfig.securityFilterChain` |
| Guest in the UI | Frontend wraps every page except `/login` and `/register` in `_layout` which redirects to `/login` when no token → **a guest cannot browse the community page in the UI** even though the API allows it. | VERIFIED | `frontend/src/routes/_layout.tsx` `beforeLoad` |
| **USER** (registered traveller) | Role enum value `USER`; default at registration. Owns trips; every trip-scoped call checks ownership (`findByIdAndUserId`) and returns **404 (not 403)** for other users' data (BR-104). | VERIFIED | `enums/UserRole.java`, `AuthService.register`, `TripServiceImpl.getOwnedTrip`, `CrossUserAccessApiTest` |
| **ADMIN** | Role enum value `ADMIN`; `/api/admin/**` requires `hasRole("ADMIN")`. Admins can also delete any blog post (`SecurityUtils.isAdmin()` in `BlogController.deletePost`). | VERIFIED | `SecurityConfig` line `.requestMatchers("/api/admin/**").hasRole("ADMIN")`, `AdminController`, `AdminKnowledgeController`, `AdminBlogController` |
| How an ADMIN is created | Not via registration. `AdminBootstrap` (ApplicationRunner) creates ADMIN accounts at startup for emails in env `ADMIN_EMAILS` with password `ADMIN_BOOTSTRAP_PASSWORD` (≥12 chars); promoting an existing USER requires `ADMIN_BOOTSTRAP_PROMOTE_EXISTING=true`. An admin can also create users with a role / change role via `POST/PUT /api/admin/users`. | VERIFIED | `configurations/AdminBootstrap.java`, `AdminService.createUser/updateUser` |
| Admin UI guard | `/admin` route redirects non-ADMIN to `/` (client-side, reads stored user role). | VERIFIED | `frontend/src/routes/_layout/admin.tsx` |
| External systems (secondary actors) | LLM provider (OpenAI-compatible API through "9router"), Google Places API (New) + Google Time Zone API, Open-Meteo (forecast, archive, geocoding), PostgreSQL, Redis, map tiles (CartoDB via Leaflet). | VERIFIED | see §3.4 |

No other roles (e.g., MODERATOR, GUEST enum) exist. DB CHECK `chk_users_role CHECK (role IN ('USER','ADMIN'))` (VERIFIED — `backend/src/main/resources/db/schema.sql`).

---

## 3. Technology stack (exact versions)

### 3.1 Backend (`backend/pom.xml`)

| Component | Version | Tag | Notes / evidence |
|---|---|---|---|
| Java | 21 (`<java.version>21`) | VERIFIED | pom; Dockerfile `eclipse-temurin:21.0.8_9-jre`; CI `java-version: '21'`. Virtual threads enabled (`spring.threads.virtual.enabled: true`). |
| Spring Boot (parent) | **3.4.3** | VERIFIED | `spring-boot-starter-parent` |
| Starters | web, security, data-jpa, validation, data-redis, actuator, webflux (WebClient for external APIs) | VERIFIED | pom |
| Spring Security | 6.4.3 (managed by Boot BOM) | PARTIAL | only version present in local `~/.m2` for `spring-security-core`; not pinned in pom |
| JPA / Hibernate | Hibernate ORM 6.6.8.Final (Boot-managed) | PARTIAL | local `~/.m2/repository/org/hibernate/orm/hibernate-core/6.6.8.Final`; `ddl-auto: validate`, `open-in-view: false` |
| Database driver | PostgreSQL JDBC 42.7.5 (Boot-managed, runtime) | PARTIAL | pom (no version) + local m2 |
| Database server | PostgreSQL **16.10-alpine** (docker-compose); tests use `postgres:16` via Testcontainers; schema header says "PostgreSQL 14+" | VERIFIED | `docker-compose.yml`, `IntegrationTest.POSTGRES`, `schema.sql` header |
| JWT library | **jjwt 0.12.6** (api / impl / jackson), HS256 via `Keys.hmacShaKeyFor` | VERIFIED | pom `jjwt.version`, `JwtTokenProvider` |
| Password hashing | BCrypt (`BCryptPasswordEncoder`) | VERIFIED | `SecurityConfig.passwordEncoder` |
| AI framework | **Spring AI 1.0.9** (`spring-ai-bom`), module `spring-ai-openai` only (no starter) | VERIFIED | pom; `OpenAiLlmGateway` builds `OpenAiApi`/`OpenAiChatModel` manually |
| Redis client | spring-data-redis (Lettuce, Boot-managed) + **Redisson 3.45.0** | VERIFIED | pom; `RedissonConfig` |
| Redis server | `redis:7.4.5-alpine` (compose, password-protected); tests `redis:7.4-alpine` | VERIFIED | `docker-compose.yml`, `IntegrationTest.REDIS` |
| API docs | springdoc-openapi-starter-webmvc-ui **2.8.5** (`/swagger-ui.html`, `/api-docs`, toggle `SWAGGER_ENABLED`) | VERIFIED | pom, `application.yml`, `OpenApiConfiguration` |
| Lombok | annotation processor 1.18.36 | VERIFIED | pom `maven-compiler-plugin` |
| Testing | spring-boot-starter-test (JUnit 5, Mockito, AssertJ), spring-security-test, **Testcontainers 1.21.4** (junit-jupiter, postgresql), spring-boot-testcontainers | VERIFIED | pom |
| Flyway | **Removed.** No Flyway dependency/plugin in pom; no `spring.flyway.*`. Schema is applied manually from `schema.sql` (identical copies in `scripts/schema.sql` and `backend/src/main/resources/db/schema.sql`). The 12 old `db/migration/V1..V12__*.sql` files are still on disk but nothing executes them. `schema.sql` even contains `DROP TABLE IF EXISTS flyway_schema_history`. | VERIFIED | commit `3a6180c` "remove flyway…"; `grep -i flyway` finds only that DROP line; `diff scripts/schema.sql backend/.../db/schema.sql` = identical |
| Schema init in tests | `spring.sql.init.mode: always`, `schema-locations: classpath:db/schema.sql` (test profile only) | VERIFIED | `backend/src/test/resources/application-test.yml` |
| Build | Maven (wrapper `mvnw`), Dockerfile multi-stage `maven:3.9.11-eclipse-temurin-21` → `eclipse-temurin:21.0.8_9-jre`, non-root user uid 1001, healthcheck on `/actuator/health` | VERIFIED | `backend/Dockerfile` |

### 3.2 AI / LLM integration

| Claim | Tag | Evidence |
|---|---|---|
| The backend talks to **any OpenAI-compatible Chat Completions endpoint** configured by `LLM_BASE_URL` (default `http://localhost:20128/v1`) + `LLM_COMPLETIONS_PATH` (default `/chat/completions`) + `LLM_API_KEY` (`<redacted>`). Comments call this gateway "9router". | VERIFIED | `application.yml` `tripmind.ai.*`, `AiProperties`, `OpenAiLlmGateway` |
| `LLM_MODEL` is a **comma-separated fallback list**; `OpenAiLlmGateway.call` tries models in order, retrying transient errors (2 attempts, 1 s backoff) and bounded by a deadline (default 2 min; chat turn 60 s). | VERIFIED | `OpenAiLlmGateway` |
| Which concrete LLM vendor is used is **deployment configuration, not code**. pom comment says the OpenAI module is used "to point at Gemini's OpenAI-compatible layer"; the footer text in the UI says "Hỗ trợ Gemini ReAct Agent"; log messages in `TripPromptParserService` mention "Gemini". However the local `.env` `LLM_MODEL` lists GPT-family / router model names (e.g. `cx/gpt-5.5`, `gh/gpt-4.1-…`, `gh/gpt-4o`, `ollama/gpt-oss:120b`) behind a localhost router. → Thesis should describe it as "LLM via OpenAI-compatible API (provider-agnostic, e.g., Gemini/GPT)". | PARTIAL / ambiguous | pom comment, `routes/_layout.tsx` footer, `TripPromptParserService` log, `.env` (model names only; key not read) |
| Temperature 0.3 (assistant), 0.1 (prompt parser); max 8 agent rounds, 60 s turn timeout, 20 history messages, tool result cut at 4000 chars, max 5 search results, 20 AI calls/hour/user, proposal TTL 30 min, undo window 10 min. | VERIFIED | `application.yml` `tripmind.ai` |
| If key/model missing → AI endpoints return **503 AI_NOT_CONFIGURED**; rest of app runs. Prompt parser falls back to regex parsing; trip recap falls back to a template. | VERIFIED | `AiProperties.isConfigured`, `AgentService.precheck`, `TripPromptParserService.dynamicFallbackParse`, `BlogService.generateTripRecap` |
| No embeddings / vector DB. "Knowledge" retrieval is SQL keyword/destination lookup in `destination_knowledge` (the code comments call it "RAG", but it is not vector RAG). | VERIFIED | `DestinationKnowledgeService.searchKnowledge`, `DestinationKnowledgeRepository` |
| Prompt templates as classpath resources: `prompts/assistant-system.md`, `prompts/generate-itinerary.md`. | VERIFIED | `SystemPromptBuilder`, `ItineraryGenerationService` constructor |
| Agent tools (13, Spring AI `@Tool`): `get_current_trip`, `get_itinerary`, `get_user_preferences`, `get_saved_places`, `calculate_trip_budget`, `get_weather`, `search_community_tips`, `search_places`, `calculate_distance`, `propose_places`, `suggest_checklist`, `propose_itinerary_changes`, `optimize_day_order`. | VERIFIED | `services/ai/tools/*.java` |

### 3.3 External services

| Service | Use | Auth | Tag | Evidence |
|---|---|---|---|---|
| **Google Places API (New) v1** `https://places.googleapis.com/v1` — Text Search, Place Details | place search, destination search/resolve, AI place pool, alternatives | API key `GOOGLE_PLACES_API_KEY` (`<redacted>`); `USE_MOCK_PLACES=true` disables calls (returns empty → DB catalog only) | VERIFIED | `GooglePlacesClient`, `application.yml tripmind.google.places` |
| **Google Time Zone API** | IANA timezone for a new destination | same key | VERIFIED | `GooglePlacesClient.resolveTimezone` |
| **Open-Meteo** forecast `api.open-meteo.com/v1`, archive `archive-api.open-meteo.com/v1`, geocoding `geocoding-api.open-meteo.com/v1` | daily weather per trip day (forecast ≤16 days, observed, climate normal from 3 past years), geocoding manually typed destinations | no key | VERIFIED | `OpenMeteoClient`, `GeocodingClient`, `WeatherService`, `WeatherProperties` |
| LLM (OpenAI-compatible) | see 3.2 | key | VERIFIED | |
| Map tiles: CartoDB Voyager raster tiles via Leaflet | frontend map | none | VERIFIED | `frontend/src/components/common/TripMap.tsx` |
| Google Maps deep links (`google.com/maps/search/?api=1&query=`) | "open in Google Maps" link on place cards | none | VERIFIED | `routes/_layout/places.tsx` |
| Currency API (Frankfurter) | proposed in design doc | — | **PROPOSED-ONLY / not implemented** (no client; currency change is blocked instead of converted) | design doc §28; `TripServiceImpl.updateTrip` CURRENCY_CHANGE_BLOCKED |
| Email (SMTP, verification, reset) | — | — | **Not implemented** (no mail dependency; `.env.example` explicitly says no email verification) | pom; `.env.example` comment |
| File storage / uploads | — | — | **Not implemented**. Multipart limited to 1 MB with comment "Không có API tải tệp". Blog photos & avatar are **https URLs only** (validated by regex). Tables `photos`, `journal_entries` exist in schema but have no entity/API. | VERIFIED | `application.yml spring.servlet.multipart`, `ValidationLimits.HTTPS_URL`, `schema.sql` |

### 3.4 Redis usage (VERIFIED)

| Use | Key pattern | Evidence |
|---|---|---|
| Rate limit (fixed window, Lua INCR+EXPIRE, fail-open) — 120 req/min/user-or-IP for `/api/**`; 20 AI calls/hour/user | `rl:{scope}:{subject}:{bucket}` | `RateLimiter`, `ApiRateLimitFilter` |
| Login brute-force lockout: 5 failures/email, 20/IP per 5 min → 429 | `login-fail:email:*`, `login-fail:ip:*` | `LoginAttemptLimiter` |
| Weather cache (TTL 3 h) | `weather:{lat}:{lng}:…` | `WeatherService.readCache/writeCache` |
| Place search candidate cache (default 1 h) used to verify AI-chosen places | (see class) | `PlaceCandidateCache` |
| Background itinerary-generation job queue (Redisson `RBlockingDeque` + processing list, `RBucket` job state, `RTopic` pub/sub progress, `RLock` per trip) | `tripmind:itinerary:{queue,processing,active:*,job:*,topic:*,lock:*}` | `ItineraryGenerationService` |

### 3.5 Frontend (`frontend/package.json`; installed versions from `package-lock.json`)

| Component | Declared | Installed | Tag / usage |
|---|---|---|---|
| React / React DOM | ^19.0.0 | 19.3.0 | VERIFIED |
| TypeScript | ^5.7.2 | 5.9.3 | VERIFIED |
| Build tool | Vite ^6.0.3 (+ `@vitejs/plugin-react-swc`) | 6.4.4 | VERIFIED; dev proxy `/api` → `VITE_BACKEND_URL` or `http://localhost:8088` (`vite.config.ts`) |
| Router | **TanStack Router** ^1.88.2 (file-based, `@tanstack/router-plugin`, `routeTree.gen.ts`) | 1.170.41 | VERIFIED |
| Server state | **TanStack Query** ^5.62.7 (+ devtools) | 5.104.1 | VERIFIED (`main.tsx` QueryClient staleTime 5 min, retry 1) |
| Client/auth state | React Context (`contexts/auth-context.tsx`) + `localStorage` (`lib/auth-storage.ts`, keys `access_token`, `user_profile`) | — | VERIFIED. **Zustand (proposed in design doc) is NOT used.** |
| HTTP | axios ^1.7.9 (interceptors add Bearer token; 401 → logout); SSE via `fetch` + custom parser (`client/sse.ts`) | 1.20.0 | VERIFIED |
| UI | Tailwind CSS v4 (`@tailwindcss/vite`), Radix UI primitives (dialog, tabs actually imported; others declared), shadcn-style components in `components/ui/*`, `class-variance-authority`, `tailwind-merge`, `lucide-react` icons, `sonner` toasts, `next-themes` (dark mode) | tailwind 4.3.3 | VERIFIED |
| Map | **Leaflet ^1.9.4** + `@types/leaflet` (added in uncommitted change) | 1.9.4 | VERIFIED (`TripMap.tsx`, used in `places.tsx` and `DayTimeline.tsx`) |
| Declared but unused | `react-hook-form`, `zod`, `@hookform/resolvers`, `date-fns`, several Radix packages (select, dropdown-menu, popover…) | — | VERIFIED (0 imports found) |
| Lint/format | Biome 1.9.4 | — | VERIFIED |
| Tests | Vitest ^3.2.7 (node env, `src/**/*.test.ts`) | 3.2.7 | VERIFIED |

### 3.6 Deployment & CI

| Item | Content | Tag | Evidence |
|---|---|---|---|
| `docker-compose.yml` | 3 services: `postgres` (16.10-alpine, port bound to 127.0.0.1, healthcheck `pg_isready`), `redis` (7.4.5-alpine, `--requirepass`), `backend` (build `./backend`, port 8088, depends on healthy db/redis, healthcheck actuator). Volumes `pgdata`, `redisdata`. **No frontend, no nginx service** (design doc proposed nginx + frontend containers). | VERIFIED | `docker-compose.yml`; design doc §39 |
| DB initialisation in compose | Not automatic: schema must be applied manually (`psql -f schema.sql`) — no init script mounted; backend uses `ddl-auto: validate`, so it would fail to start on an empty DB. | INFERRED | `docker-compose.yml` (no `/docker-entrypoint-initdb.d` mount), `application.yml` |
| Frontend deployment | Vite build (`npm run build` → `frontend/dist`); no Dockerfile; served by Vite dev server in development. | VERIFIED (no Dockerfile) | `frontend/` listing |
| GitHub Actions `.github/workflows/ci.yml` | On push to `main` and PRs. Job **backend**: Temurin 21, `./mvnw -B verify` (runs unit + Testcontainers integration tests). Job **frontend** (matrix Node 20 and 22): `npm ci`, `npm run typecheck`, `npm run lint:ci`, `npm test`, `npm run build`. | VERIFIED | `ci.yml` |
| CD (automatic deploy) | None. | VERIFIED (absent) | |
| Config | Secrets via root `.env` (imported by Spring `spring.config.import: optional:file:.env[.properties]`); template `.env.example`. JWT secret must be ≥32 chars or startup fails. | VERIFIED | `application.yml`, `JwtProperties` |

---

## 4. Repository map

| Path | Content |
|---|---|
| `backend/` | Spring Boot app (`com.tripmind`), 19 controllers, ~45 services, 21 entities, 21 repositories, 31 test classes |
| `frontend/` | React SPA (7 routes, ~60 components) |
| `scripts/schema.sql` | Full PostgreSQL schema + seed of 15 popular destinations (manual apply) |
| `docs/` | Internal design docs (Vietnamese): `ADS-01…ADS-40` (requirements FR/BR codes, domain dictionary, architecture, DB design, AI agent integration, API contract, UI flow), `ARCHITECTURE.md`, `FEATURES_AND_FLOWS.md`, `FEATURE-REVIEW.md`, `BE-TODO.md`, etc. Code comments reference these FR-/BR-/NFR- codes. Untracked `docs/KTPM_2022603085_BuiMinhHieu_Baocao.pdf` is an unrelated/other report (not analysed). |
| `demo/tripmind-demo.html` | Stand-alone HTML mock-up that runs entirely in the browser (`localStorage`/IndexedDB); **not the real app** — it shows ideas (journal, photos, "today" strip) that are not all implemented in the real backend. |
| `scratch/` | Ad-hoc test scripts (`test_all_endpoints.py`, `test_full_dom_flows.js`) — not part of CI. |
| `TripMind_AI_Travel_Planner.md` | Original design/proposal document (vision, features, ERD, API, MVP phases). Several items there are not implemented — see `feature-catalog.md` §Z. |

---

## 5. Notable cross-cutting facts for the thesis

1. **Stateless JWT, single access token (24 h)**, no refresh token, no server-side logout. Revocation is done by `users.token_version` (claim `tv`), incremented on password change or deactivation. (VERIFIED — `JwtTokenProvider`, `JwtAuthenticationFilter`, `UserEntity.setPasswordHash/setActive`, `BE-TODO.md` BE-1.7/1.8.)
2. **Ownership → 404 policy** (never leaks existence). (VERIFIED.)
3. **Human-in-the-loop AI**: all AI writes go through `ai_proposals` (PENDING → APPLIED/REJECTED/EXPIRED/REVERTED), with undo within 10 min that preserves manual edits via content fingerprints. (VERIFIED.)
4. **Background job + SSE** for itinerary generation; **SSE streaming** for chat. (VERIFIED.)
5. **Rate limiting & hardening**: global 120 req/min, AI 20/h, login lockout, 1 MB body limit (413), strict CORS list (no `*`), Swagger toggle, actuator exposes health only. (VERIFIED.)
6. Timezone correctness: "today" computed in destination timezone (`TripClock`). (VERIFIED.)
