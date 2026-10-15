# Architecture — TripMind (internal evidence)

Tags: VERIFIED / PARTIAL / INFERRED / UNVERIFIED. Backend root package `com.tripmind` (`backend/src/main/java/com/tripmind/`), frontend root `frontend/src/`.

## 1. Overall style

| Claim | Tag | Evidence |
|---|---|---|
| Client–server SPA + REST API. React SPA calls a single Spring Boot monolith over HTTP/JSON; streaming via Server-Sent Events (chat, generation progress). | VERIFIED | `client/client.ts`, `client/sse.ts`, `AiController`, `ProposalController.streamGenerationStatus` |
| Backend is a **layered monolith**: Controller → Service → Repository → PostgreSQL, plus integration clients (Google, Open-Meteo, LLM) and Redis. No microservices, no message broker other than Redis (Redisson). | VERIFIED | package structure below |
| AI subsystem is an **agent loop with tool calling** implemented manually on top of Spring AI `ChatModel` (tools are Spring beans with `@Tool` methods; executions are audited; writes go through approval). | VERIFIED | `services/ai/*` |
| Deployment topology (compose): backend container + PostgreSQL + Redis; frontend served separately (Vite dev server / static build). External SaaS: LLM router, Google APIs, Open-Meteo. | VERIFIED | `docker-compose.yml`, `vite.config.ts` |

Component diagram (text):
```
[Browser: React SPA] --HTTPS/JSON + SSE (Bearer JWT)--> [Spring Boot backend :8088]
    backend --JDBC (Hikari, max 10)--> [PostgreSQL 16]
    backend --Lettuce / Redisson--> [Redis 7.4]  (rate limits, caches, job queue, pub/sub, locks)
    backend --WebClient/RestClient--> [LLM (OpenAI-compatible, "9router")]
    backend --WebClient--> [Google Places API (New) + Time Zone API]
    backend --WebClient--> [Open-Meteo forecast / archive / geocoding]
    Browser --tiles--> [CartoDB basemaps via Leaflet]
```

## 2. Backend packages and responsibilities (file counts VERIFIED)

| Package | # files | Responsibility |
|---|---|---|
| `TripMindApplication` | 1 | Spring Boot entry point |
| `configurations` | 5 | `AiConfiguration` (`@EnableScheduling`, `aiExecutor` = virtual-thread-per-task executor), `RedissonConfig` (RedissonClient from `RedisConnectionDetails`), `OpenApiConfiguration` (bearer scheme), `TimeConfiguration` (`Clock` bean, UTC, injectable for tests), `AdminBootstrap` (ApplicationRunner creating ADMIN accounts) |
| `configurations.properties` | 6 | Typed `@ConfigurationProperties` records: `AiProperties` (`tripmind.ai`), `TripProperties` (max-days 30, planned-day threshold), `WeatherProperties`, `BudgetProperties`, `ChecklistProperties`, `CostEstimateProperties` |
| `configurations.security` | 10 | `SecurityConfig` (filter chain, CORS, BCrypt, 401/403 JSON handlers), `JwtTokenProvider`, `JwtAuthenticationFilter`, `JwtProperties` (secret ≥32 chars validated at startup), `CustomUserDetailsService`, `UserPrincipal`, `SecurityUtils` (current user id / isAdmin), `ApiRateLimitFilter`, `LoginAttemptLimiter`, `RequestSizeLimitFilter` |
| `controllers` | 19 | REST endpoints (see `api-inventory.md`); thin: read current user id via `SecurityUtils`, call one service, wrap in `ApiResponse` |
| `services` | 35 | Business logic. Interfaces + impls for core modules (`TripService/TripServiceImpl`, `ItineraryService/…Impl`, `PlaceService`, `BudgetService`, `ExpenseService`, `SavedPlaceService`); concrete services for auth, user, preferences, checklist (+ `ChecklistSuggester` rule engine), weather, destinations, knowledge, blog, admin, duplication, alternatives, place adoption, place rating; pure helpers: `DistanceService` (Haversine + speed table), `ItineraryOptimizer` (nearest-neighbour + 2-opt), `DayOrderService`, `CostEstimator`, `TripClock` (destination-timezone "today", phase), `TripMapper` (entity→DTO), `IcsExporter` (RFC 5545), `RateLimiter` (Redis Lua), `PlaceCandidateCache` |
| `services.clients` | 3 | `GooglePlacesClient` (Places API New: searchText, findPlace, getPlaceDetails; Time Zone API; mock switch), `OpenMeteoClient` (forecast, archive), `GeocodingClient` (Open-Meteo geocoding) |
| `services.ai` | 16 | `LlmGateway` (port) / `OpenAiLlmGateway` (adapter: model fallback list, retries, deadlines), `AgentService` (agent loop), `AgentTurn` (per-turn context: deadline, emitted events, execution ids, proposal ids, attachments), `AgentEventSink` (SSE callback), `ToolRegistry` (scans `AgentToolSet` beans for `@Tool`), `ToolExecutor` (dedupe, timeout, truncation, audit row), `SystemPromptBuilder`, `ConversationService`, `ProposalService` (build/validate/expire proposals), `ProposalApplyService` (apply/undo with row lock and fingerprints), `ExplanationService`, `ItineraryGenerationService` (Redis job queue workers, pool building, LLM plan, verification), `PlaceVerifier`, `TripPromptParserService` |
| `services.ai.tools` | 7 | Tool sets: `TripReadTools` (get_current_trip, get_itinerary, get_user_preferences, get_saved_places, calculate_trip_budget, get_weather, search_community_tips), `PlaceTools` (search_places, calculate_distance, propose_places), `ProposalTools` (propose_itinerary_changes, optimize_day_order), `ChecklistTools` (suggest_checklist); helpers `ToolSupport`, `PlaceView`, `PlaceRatingLookup` |
| `repositories` | 21 | Spring Data JPA interfaces (derived queries, JPQL `@Query`, a few native queries: upvote `INSERT … ON CONFLICT`, `syncUpvoteCount`, sums); `ExpenseRepository` also uses `JpaSpecificationExecutor`. `AdminService` uses `JdbcTemplate` for aggregate SQL. |
| `entities` | 21 | JPA entities (Lombok `@Builder`), see `data-model.md` |
| `enums` | 23 | Domain enums mirrored by DB CHECKs |
| `domains.requests` | 27 | Input DTOs with Bean Validation (`ValidationLimits` constants) |
| `domains.responses` | 34 | Output DTOs incl. `ApiResponse<T>` envelope, `PageResponse<T>` |
| `domains.models` | 2 | JSONB value objects `GroupProfile`, `TravelPlan` (records) |
| `domains.ai` / `domains.proposals` | 1 / 1 | `ItineraryJobPayload` (job state in Redis), `ProposalChange` (sealed ADD/REMOVE/UPDATE/REORDER + JSON (de)serialisation) |
| `exceptions` | 3 | `AppException(ErrorCode, message, details)`, `ErrorCode` (29 codes → HTTP status), `GlobalExceptionHandler` (`@RestControllerAdvice`) |
| `utils` | 2 | `MoneyFormat`, `WeatherCodes` |
| `resources` | — | `application.yml`, `prompts/assistant-system.md`, `prompts/generate-itinerary.md`, `db/schema.sql`, legacy `db/migration/*.sql` |

Not present (VERIFIED absence): separate `dto`/`mapper` libraries (MapStruct), `@Async` annotations, Spring Cache abstraction (`@Cacheable`), WebSocket, message broker, multi-module build.

## 3. Request lifecycle (synchronous JSON request)

1. **Servlet filters** (VERIFIED): `RequestSizeLimitFilter` (`@Order(HIGHEST_PRECEDENCE+10)`) rejects bodies > 1 MB with 413 JSON (`Content-Length` check + counting wrapper for chunked bodies).
2. **Spring Security filter chain** (`SecurityConfig.securityFilterChain`):
   - CSRF disabled (stateless API), CORS with explicit origin list `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173,http://localhost:3000`, `*` filtered out), methods GET/POST/PUT/PATCH/DELETE/OPTIONS, headers Authorization/Content-Type/Accept/X-Requested-With, credentials allowed, max-age 3600.
   - Session policy STATELESS.
   - `JwtAuthenticationFilter` (added before `UsernamePasswordAuthenticationFilter`): reads `Authorization: Bearer` only (query-string tokens deliberately unsupported) → `JwtTokenProvider.getClaims` (HS256 signature + expiry) → `CustomUserDetailsService.loadUserById(sub)` (DB hit every request) → reject silently if user inactive or claim `tv` ≠ `users.token_version` → set `UsernamePasswordAuthenticationToken` with authority `ROLE_<role>`. Invalid tokens are ignored (request continues anonymous → later 401).
   - `ApiRateLimitFilter` (after JWT filter; its auto servlet registration is disabled via `FilterRegistrationBean.setEnabled(false)`): for `/api/**` except `/api-docs`, `RateLimiter.check("api", "u<id>" | "ip<remoteAddr>", 120, 1 min)` → 429 JSON with `retryAfterSeconds`.
   - Authorization rules: ASYNC/ERROR dispatches permitted (SSE); public: register, login, auth/health, Swagger, actuator health, `GET /api/blog/**`; `/api/admin/**` → `hasRole("ADMIN")`; everything else authenticated. `@EnableMethodSecurity` is on but no `@PreAuthorize` is used (INFERRED from grep: none found).
   - Failures: `authenticationEntryPoint` → 401 `{code:"UNAUTHORIZED"}`; `accessDeniedHandler` → 403 `{code:"FORBIDDEN"}`.
3. **DispatcherServlet → Controller**: Bean Validation (`@Valid`, `@Validated` + `@Min/@Max` on params) → `SecurityUtils.getCurrentUserId()`.
4. **Service** (`@Transactional`, read-only where possible; `open-in-view=false` so DTOs are built inside the transaction). Ownership check pattern: `repository.findByIdAndUserId(...)` → `AppException(RESOURCE_NOT_FOUND)` → 404 (never 403, BR-104). Long external calls (Google, Open-Meteo, LLM) are intentionally done **outside** DB transactions in most services (`WeatherService`, `ChecklistService.suggestions`, `ProposalApplyService` prefetch, `BlogService.extractKnowledgeFromPost` via `TransactionTemplate` snapshot) — exception: uncommitted `BlogService.generateTripRecap` calls the LLM inside `@Transactional(readOnly=true)` (INFERRED connection-holding).
5. **Repository** (Spring Data JPA / Hibernate 6) → PostgreSQL. Concurrency: pessimistic row lock on trip when applying proposals (`TripRepository.findByIdAndUserIdForUpdate`), deferrable unique constraints for reorder/shift, `INSERT … ON CONFLICT` for upvotes, unique violations mapped to 409 by `GlobalExceptionHandler.handleDataIntegrity`.
6. **Response**: `ResponseEntity<ApiResponse<T>>`. Errors → `GlobalExceptionHandler`: `AppException` → its `ErrorCode.status` + `{code, message, details}`; `MethodArgumentNotValidException`/`ConstraintViolationException`/unreadable body/type mismatch/missing param → 400 `VALIDATION_ERROR`; `AccessDeniedException` → 403; `NoResourceFoundException` → 404; method not supported → 405 (code VALIDATION_ERROR); unreadable body whose cause is `RequestSizeLimitFilter.PayloadTooLargeException` (chunked body over limit) → 413; `DataIntegrityViolationException` → 409 `CONFLICT`; anything else → 500 `INTERNAL_SERVER_ERROR` (no stack trace leaked).

Error codes (VERIFIED `ErrorCode`): RESOURCE_NOT_FOUND 404, PROPOSAL_NOT_FOUND 404, UNAUTHORIZED 401, FORBIDDEN 403, VALIDATION_ERROR 400, INVALID_DATE_RANGE 400, INVALID_TIME_RANGE 400, TRIP_TOO_LONG 422, DAYS_HAVE_ACTIVITIES 422, REORDER_SET_MISMATCH 422, CURRENCY_MISMATCH 422, TRIP_NOT_ENDED 409, TRIP_NOT_EMPTY 409, PROPOSAL_NOT_PENDING 409, PROPOSAL_EXPIRED 409, PROPOSAL_STALE 409, PROPOSAL_NOT_APPLIED 409, UNDO_WINDOW_CLOSED 409, UNDO_BLOCKED_BY_LATER 409, UNDO_NOT_AVAILABLE 409, CONFLICT 409, RATE_LIMITED 429, AI_NOT_CONFIGURED 503, AI_PROVIDER_ERROR 502, EXTERNAL_SERVICE_ERROR 502, DUPLICATE_TARGET 422, CURRENCY_CHANGE_BLOCKED 422, AI_TIMEOUT 504, INTERNAL_SERVER_ERROR 500.

## 4. Streaming & asynchronous processing

| Mechanism | Where | Tag |
|---|---|---|
| SSE chat: controller returns `SseEmitter` immediately; turn runs on `aiExecutor` (virtual threads). Prechecks run before opening the stream so errors are proper JSON HTTP codes. Events: tool_start, tool_end, proposal, places, checklist, token, done, error. Async dispatch is `permitAll` because the original request was authenticated. | `AiController.chat`, `AgentService` | VERIFIED |
| Background itinerary generation: Redis (Redisson) reliable queue (BLMOVE pattern `pollLastAndOfferFirstTo` into a processing list), 2 worker loops started `@PostConstruct` on `aiExecutor`, per-trip `RLock` (watchdog), job state in `RBucket` (2 h TTL), progress via `RTopic` pub/sub to SSE subscribers (works across instances), graceful shutdown `@PreDestroy` waits ≤30 s. | `ItineraryGenerationService` | VERIFIED |
| Scheduled jobs (`@EnableScheduling`): `ProposalService.expirePending` every 1 min (PENDING past `expires_at` → EXPIRED); `ItineraryGenerationService.recoverStaleJobs` every 1 min (initial delay 1 min) re-queues jobs stuck in processing. | `ProposalService`, `ItineraryGenerationService` | VERIFIED |
| LLM call timeouts: each model call runs on a virtual-thread executor and is cancelled at the caller's deadline (`Future.get(remaining)`); tool executions also have deadlines. | `OpenAiLlmGateway`, `ToolExecutor` | VERIFIED |
| `@Async` | not used | VERIFIED |

## 5. External integrations (adapters)

| Integration | Adapter | Failure handling | Tag |
|---|---|---|---|
| LLM | `LlmGateway` interface ← `OpenAiLlmGateway` (Spring AI `OpenAiApi` + `OpenAiChatModel` per model name); tests replace with `support/FakeLlmGateway` (`@Primary`) | not configured → 503; all models fail → 502 `AI_PROVIDER_ERROR`; deadline → 504 `AI_TIMEOUT`; agent loop degrades to "wrap-up" answer | VERIFIED |
| Google Places / Time Zone | `GooglePlacesClient` (WebClient, field masks, language `vi`, max 10 results) | errors → `EXTERNAL_SERVICE_ERROR`; most callers catch and fall back to DB catalog; `USE_MOCK_PLACES=true` returns empty | VERIFIED |
| Open-Meteo | `OpenMeteoClient`, `GeocodingClient` (timeout 5 s) | per-group failure → partial weather; Redis cache 3 h | VERIFIED |
| Redis | `StringRedisTemplate` (Lettuce) + `RedissonClient` | rate limiter/login limiter/caches fail-open; generation queue requires Redis | VERIFIED |

## 6. Security architecture summary

| Control | Implementation | Tag |
|---|---|---|
| Authentication | JWT HS256 (jjwt 0.12.6), 24 h, claims sub/email/role/name/tv; stored in browser localStorage | VERIFIED |
| Revocation | `users.token_version` compared on every request (password change, deactivation) | VERIFIED |
| Authorization | Role-based URL rule for `/api/admin/**`; ownership checks in services (404) | VERIFIED |
| Brute force | `LoginAttemptLimiter` (Redis) | VERIFIED |
| Rate limiting | 120 req/min global; 20 AI calls/hour | VERIFIED |
| Input limits | Bean Validation sizes, 1 MB body, multipart 1 MB, https-only URLs | VERIFIED |
| Prompt-injection mitigation | community content wrapped in `<untrusted_…>` tags with delimiter stripping (`BlogService.extractKnowledgeFromPost`); AI tools take trip/user ids from server-side `AgentTurn`, never from model arguments (test `AiAssistantApiTest.toolsIgnoreTripIdFromModel`) | VERIFIED |
| AI write safety | proposals + human approval + apply by id only + undo; invented place ids rejected | VERIFIED |
| Secrets | `.env` (git-ignored) imported by Spring; `JWT_SECRET` mandatory ≥32 chars; no default credentials | VERIFIED |
| Hardening | CORS allow-list, Swagger toggle, actuator health only (no details), `forward-headers-strategy: none` by default (IP spoofing), non-root Docker user, DB/Redis ports bound to 127.0.0.1 | VERIFIED |
| Gaps | no refresh/logout endpoint; token in localStorage (XSS exposure); `/api/blog/trips/{id}/recap` lacks AI rate limit; community trip copy exposes full trip data of any post-linked trip | INFERRED |

## 7. Frontend architecture

| Aspect | Implementation | Tag |
|---|---|---|
| Entry | `main.tsx`: creates `QueryClient` (staleTime 5 min, retry 1), TanStack `createRouter({routeTree, defaultPreload:"intent"})`, registers `setUnauthorizedHandler` (clear cache + go to `/login`) | VERIFIED |
| Routing (file-based, TanStack Router, `routeTree.gen.ts`) | `__root.tsx` (providers: Auth, Theme, Toaster); `login.tsx`, `register.tsx` (redirect to `/` if token); `_layout.tsx` (auth guard → `/login?redirect=`; Header + footer); `_layout/index.tsx` (My trips dashboard), `_layout/trips.$tripId.tsx` (trip workspace), `_layout/places.tsx` (explore & saved places with map), `_layout/community.tsx` (blog feed), `_layout/admin.tsx` (ADMIN guard; tabs) | VERIFIED |
| API layer | `client/client.ts` axios instance (baseURL `VITE_API_URL` or same origin; request interceptor adds Bearer; response interceptor: 401-with-token → `logout()`, network error → `ApiError`, unwraps `ApiResponse`), `request<T>()` returns `data`, `downloadFile()` for .ics; `client/sse.ts` fetch-based SSE reader (supports POST + headers); `client/errors.ts` (`ApiError`, code mapping); per-module API objects in `client/api/*-api.ts`; types in `client/types/index.ts` | VERIFIED |
| Server state | TanStack Query hooks (`hooks/use-trips.ts`: useTrips/useTrip/useItinerary/useTripWeather; `hooks/query-keys.ts`), mutations in components with cache invalidation; `useInfiniteQuery` for community feed; `components/pages/community/blog-cache.ts` helpers | VERIFIED |
| Auth state | `contexts/auth-context.tsx` (React Context over `lib/auth-storage.ts` with a subscribe/notify mechanism so logouts from the interceptor propagate) | VERIFIED |
| Long-running AI | `hooks/use-itinerary-generation.ts` (start job → SSE stream → poll fallback every 2 s, stale-run guards); `components/pages/ai/chat-state.ts` (pure reducer-style helpers, unit-tested) | VERIFIED |
| Components | `components/ui/*` (shadcn-style: button, card, badge, input, dialog incl. SheetContent, confirm-dialog, tabs, sonner); `components/common/*` (Header, Logo, NotFound, **TripMap** Leaflet wrapper); feature folders `pages/{trips, itinerary, ai, budget, checklist, community, admin, user}` | VERIFIED |
| Styling | Tailwind CSS v4 via Vite plugin, `index.css`, `theme.postcss`, dark mode via `next-themes` + `theme-context` | VERIFIED |
| Validation | hand-written helpers in `lib/validation.ts` (react-hook-form/zod declared but unused) | VERIFIED |
| Build/quality | Vite 6 + SWC, `tsc --noEmit` typecheck (passes), Biome lint, Vitest | VERIFIED |

Frontend → backend mapping per screen is given in `key-flows.md`.
