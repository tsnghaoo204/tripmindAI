# Feature Catalog — TripMind (internal evidence)

Tags: **VERIFIED / PARTIAL / INFERRED / UNVERIFIED**. Status: **IMPLEMENTED** (backend + UI), **BACKEND-ONLY** (API exists, no UI uses it), **PARTIAL** (incomplete or buggy path), **PROPOSED-ONLY** (only in `TripMind_AI_Travel_Planner.md` or `docs/`, not in code).
Paths: backend classes under `backend/src/main/java/com/tripmind/`, frontend under `frontend/src/`.
Common error envelope: `ApiResponse{success:false, code:<ErrorCode>, message, details, timestamp}` produced by `exceptions/GlobalExceptionHandler` (validation → 400 `VALIDATION_ERROR`; ownership miss → 404 `RESOURCE_NOT_FOUND`; no/invalid token → 401 `UNAUTHORIZED` from `SecurityConfig.authenticationEntryPoint`; wrong role → 403 `FORBIDDEN`; rate limit → 429 `RATE_LIMITED`; body > 1 MB → 413 `PAYLOAD_TOO_LARGE`).

Summary table (details below):

| # | Module / feature | Status | Tag |
|---|---|---|---|
| A1 | Register | IMPLEMENTED | VERIFIED |
| A2 | Login (+ brute-force lockout) | IMPLEMENTED | VERIFIED |
| A3 | Logout | PARTIAL (client-side only) | VERIFIED |
| A4 | Refresh token | PROPOSED-ONLY | VERIFIED (absent) |
| A5 | Current user / profile view | IMPLEMENTED (UI reads stored user; `/me` & GET profile backend-only) | VERIFIED |
| A6 | Update profile (name, avatar URL) | IMPLEMENTED | VERIFIED |
| A7 | Change password (+ token revocation) | IMPLEMENTED | VERIFIED |
| A8 | Default travel preferences | BACKEND-ONLY | VERIFIED |
| T1 | Create trip (multi-step wizard, AI prompt prefill) | IMPLEMENTED | VERIFIED |
| T2 | AI parse natural-language prompt → trip fields | IMPLEMENTED | VERIFIED |
| T3 | List my trips (upcoming / past) | IMPLEMENTED | VERIFIED |
| T4 | Trip detail | IMPLEMENTED | VERIFIED |
| T5 | Edit trip (dates resize, budget, currency…) | BACKEND-ONLY | VERIFIED |
| T6 | Set trip phase manually | IMPLEMENTED | VERIFIED |
| T7 | Delete trip | IMPLEMENTED | VERIFIED |
| T8 | Duplicate own trip | IMPLEMENTED | VERIFIED |
| T9 | Copy a trip from a community post | IMPLEMENTED (uncommitted) | VERIFIED |
| T10 | Export itinerary to .ics | IMPLEMENTED | VERIFIED |
| T11 | Destination search / catalog | IMPLEMENTED | VERIFIED |
| I1 | View itinerary (days, distances, travel time, tips) | IMPLEMENTED | VERIFIED |
| I2 | Add activity (manual / from saved place) | IMPLEMENTED | VERIFIED |
| I3 | Update activity / mark status (DOING/DONE/SKIPPED) | BACKEND-ONLY | VERIFIED |
| I4 | Delete activity | IMPLEMENTED | VERIFIED |
| I5 | Reorder activities (drag & drop) | BACKEND-ONLY | VERIFIED |
| I6 | Cost estimate hint (price level × travellers) | IMPLEMENTED | VERIFIED |
| I7 | Budget-tier alternatives for food/lodging | IMPLEMENTED | VERIFIED |
| I8 | Day map (Leaflet route) | IMPLEMENTED (uncommitted) | VERIFIED |
| AI1 | AI itinerary generation (background job) | IMPLEMENTED | VERIFIED |
| AI2 | AI assistant chat (SSE, tool calling) | IMPLEMENTED | VERIFIED |
| AI3 | Proposals: view / apply / reject | IMPLEMENTED | VERIFIED |
| AI4 | Undo applied proposal (+ preview) | IMPLEMENTED (preview backend-only) | VERIFIED |
| AI5 | "Why this activity?" explanation | IMPLEMENTED | VERIFIED |
| AI6 | AI place candidates / checklist suggestions inside chat | PARTIAL (events not rendered by UI) | VERIFIED |
| AI7 | Conversation history | IMPLEMENTED | VERIFIED |
| AI8 | AI audit log per trip | BACKEND-ONLY (admin UI shows global log) | VERIFIED |
| C1 | Checklist CRUD (PACK / TODO) | IMPLEMENTED | VERIFIED |
| C2 | Rule-based checklist suggestions | IMPLEMENTED | VERIFIED |
| B1 | Budget summary & warnings & daily allowance | IMPLEMENTED | VERIFIED |
| B2 | Expenses CRUD | IMPLEMENTED (no edit UI) | VERIFIED |
| W1 | Trip weather per day | IMPLEMENTED | VERIFIED |
| P1 | Place search / explore page with map | IMPLEMENTED | VERIFIED |
| P2 | Save / unsave favourite places | PARTIAL (saving a Google result from Explore likely fails) | INFERRED |
| P3 | Nearby recommendations, place details by provider id | BACKEND-ONLY | VERIFIED |
| P4 | Rate places after trip (LIKE/DISLIKE) | IMPLEMENTED | VERIFIED |
| CM1 | Community feed (filter by destination, search, paging) | IMPLEMENTED | VERIFIED |
| CM2 | Create post (optionally linked to own trip) | IMPLEMENTED | VERIFIED |
| CM3 | Delete post (author or admin) | IMPLEMENTED | VERIFIED |
| CM4 | Upvote post (toggle) | PARTIAL (UI cache update mismatch) | INFERRED |
| CM5 | Comments + comment upvote | IMPLEMENTED | VERIFIED |
| CM6 | AI trip recap → draft review post | IMPLEMENTED (uncommitted) | VERIFIED |
| CM7 | Edit post / delete comment | PROPOSED-ONLY / absent | VERIFIED (absent) |
| AD1 | Admin: AI usage metrics | IMPLEMENTED | VERIFIED |
| AD2 | Admin: tool execution log | IMPLEMENTED | VERIFIED |
| AD3 | Admin: user management (list/create/update role,active,password/delete) | IMPLEMENTED | VERIFIED |
| AD4 | Admin: trip management (list/edit name,phase,budget/delete) | IMPLEMENTED | VERIFIED |
| AD5 | Admin: knowledge dataset CRUD + toggle | IMPLEMENTED | VERIFIED |
| AD6 | Admin: extract knowledge from community post via AI | IMPLEMENTED | VERIFIED |
| X1 | Dark/light theme, responsive mobile nav | IMPLEMENTED (mobile nav uncommitted) | VERIFIED |
| Z  | Design-doc features not in code | PROPOSED-ONLY | see §Z |

---

## A. Authentication & account

### A1 Register — IMPLEMENTED (VERIFIED)
- Actors: Guest. Preconditions: none.
- Main flow: `routes/register.tsx` form (name, email, password) → `authApi.register` → `POST /api/auth/register` → `AuthController.register` (`@Valid RegisterRequest`: email ≤254 valid format, password 8–72, name ≤120) → `AuthService.register`: lower-case+trim email; `UserRepository.existsByEmail` → build `UserEntity(role=USER, isActive=true, passwordHash=BCrypt)` → `save` → returns `AuthResponse{accessToken, tokenType:"Bearer", expiresInMs, user}` with 201.
- UI then shows toast and navigates to `/login` (the returned token is **not** used — user must log in). (VERIFIED `register.tsx`.)
- Alternatives: email taken → 409 `CONFLICT`; validation → 400; already logged-in user visiting `/register` is redirected to `/`.
- No email verification, no captcha. (VERIFIED; `.env.example` comment.)

### A2 Login — IMPLEMENTED (VERIFIED)
- Main flow: `routes/login.tsx` → `authApi.login` → `POST /api/auth/login` → `AuthController.login`: `LoginAttemptLimiter.ensureAllowed(email, ip)` (Redis counters) → `AuthService.login` (find by normalized email; BCrypt `matches`; `isActive` check) → `JwtTokenProvider.generateToken` (claims `sub`=userId, `email`, `role`, `name`, `tv`=tokenVersion; HS256; exp = 24 h default) → `loginAttemptLimiter.reset(email)` → 200 `AuthResponse`. UI: `useAuth().login` → `saveSession` (localStorage `access_token`, `user_profile`) → navigate to safe `redirect` search param or `/`.
- Alternatives: wrong email/password → 401 `UNAUTHORIZED` "Invalid email or password" and failure recorded; deactivated → 401 "User account is deactivated" (also recorded as failure since code is UNAUTHORIZED); ≥5 failures/email or ≥20/IP within 5 min → 429 `RATE_LIMITED` with `retryAfterSeconds` (even with correct password); Redis down → limiter fails open.
- Evidence: `LoginAttemptLimiter`, `AuthServiceTest`, `SecurityHardeningApiTest`.

### A3 Logout — PARTIAL (VERIFIED)
- Only client-side: Header logout button → `lib/auth-storage.logout()` → clears localStorage, `queryClient.clear()`, navigate `/login` (`main.tsx` `setUnauthorizedHandler`). **No backend logout endpoint**; the JWT stays valid until expiry. Server-side revocation happens only on password change or deactivation (token_version).
- Also triggered automatically when any authenticated request returns 401 (`client/client.ts` response interceptor).

### A4 Refresh token — PROPOSED-ONLY
- Design doc §4.1/§23 lists `POST /api/auth/refresh`, `POST /api/auth/logout`. Not in code; `docs/BE-TODO.md` BE-1.7/BE-1.8 marks them "Chưa làm" and replaced by `token_version`.

### A5 Current user / profile — IMPLEMENTED
- Backend: `GET /api/auth/me` and `GET /api/users/profile` → `UserService.getProfile`. Frontend uses the user object stored at login (`getStoredUser`); `authApi.getProfile` is defined but not called by any component (VERIFIED grep). Header displays name/role badge.

### A6 Update profile — IMPLEMENTED (VERIFIED)
- `components/pages/user/UserProfileModal.tsx` → `authApi.updateProfile` (client-side avatar URL validation) → `PUT /api/users/profile` → `UserService.updateProfile` (name 1–120; avatarUrl empty or `https://…`, ≤2048; empty string clears avatar).

### A7 Change password — IMPLEMENTED (VERIFIED)
- `UserProfileModal` → `authApi.changePassword` → `PUT /api/users/change-password` → `UserService.changePassword`: verify current password (else 400 `VALIDATION_ERROR` "Current password does not match") → `user.setPasswordHash(...)` which **increments `tokenVersion`** → controller returns a **new token** via `AuthService.issueToken` → UI `saveSession` replaces token. All older tokens now fail in `JwtAuthenticationFilter` (tv mismatch → request unauthenticated → 401).
- Tests: `UserServiceTest`, `SecurityHardeningApiTest`.

### A8 Default travel preferences — BACKEND-ONLY (VERIFIED)
- `GET/PUT /api/me/preferences` → `UserPreferencesService` (travelStyle, budgetPreference, preferences list ≤20). Auto-created from the first trip's choices in `TripServiceImpl.createTrip` when none exist; used as defaults for new trips. `authApi.getPreferences/updatePreferences` exist but no component calls them.

---

## T. Trips

### T1 Create trip — IMPLEMENTED (VERIFIED)
- Actors: USER. Pre: logged in.
- UI: Dashboard `routes/_layout/index.tsx` → `components/pages/trips/CreateTripModal.tsx` (4-step wizard `STEPS`, optional AI prompt box). Destination either chosen from `tripApi.getDestinations` (→ `GET /api/destinations?query=`) or typed manually (custom mode → `destinationData{provider:"MANUAL", name, country, lat?, lng?, timezone}`).
- API: `POST /api/trips` → `TripController.createTrip` (`CreateTripRequest`: name ≤160, startDate `@FutureOrPresent`, endDate, travelers ≥1, budget ≥0, currency `^[A-Z]{3}$` default VND, travelStyle, budgetPreference, preferences ≤20, groupProfile, travelPlan) → `TripServiceImpl.createTrip`:
  1. load user; `validateDateRange` (end ≥ start → else 400 `INVALID_DATE_RANGE`; length ≤ 30 days → else 422 `TRIP_TOO_LONG`);
  2. `validateGroup` (children+seniors ≤ travelers → else 400);
  3. `DestinationService.getOrCreateDestination(destinationId | destinationPlaceId | destinationData)` — existing row, or Google Place Details, or manual input (geocoded by Open-Meteo if no coords; timezone via Google Time Zone API);
  4. fill missing style/budget preference/preferences from `user_preferences`;
  5. build `TripEntity` + one `ItineraryDayEntity` per date (cascade);
  6. if user had no defaults, create `user_preferences` from this trip;
  7. `tripRepository.save` → `TripMapper.toDetail` → 201.
- Alternatives: unknown destinationId → 404; neither destination input → 400 `VALIDATION_ERROR`; Google failure while resolving placeId → 502 `EXTERNAL_SERVICE_ERROR`.
- Evidence: `TripApiTest.createAndShiftTrip`, `tripTooLong`.

### T2 AI prompt parsing — IMPLEMENTED (VERIFIED)
- UI: prompt box in `CreateTripModal` → `lib/prompt-parser.parseTripPromptWithAi` → `tripApi.parsePrompt` → `POST /api/trips/parse-prompt` (`ParsePromptRequest.prompt` 1–1000 chars, auth required) → `TripController.parsePrompt`: `RateLimiter.check("ai", userId, 20/h)` → `TripPromptParserService.parsePrompt`:
  - If LLM configured: system prompt asks for strict JSON (destination, country, lat/lng, timezone, days, nights, travelers, budget, travelStyle, children/seniors, vegetarian, mobility, origin, transportMode, localTransport, lodgingType, interests, childAges, motionSickness, avoid); temperature 0.1; parse first `{…}`; clamp days to [1, 30], travelers, budget; startDate = tomorrow in that timezone; `matchDestination` (accent-insensitive) against DB → `destinationId` / `isCustomDestination`.
  - On any LLM error / not configured → `dynamicFallbackParse` (Vietnamese regex: "đi/đến/tới/du lịch X", "N ngày M đêm", "N người", "X triệu"…).
  - Frontend also has its own local fallback `parseTripPromptDynamic` (`lib/prompt-parser.ts`).
- Output only pre-fills the wizard; nothing is saved.
- Alternatives: anonymous 401, empty/too long 400 (test `SecurityHardeningApiTest.parsePromptRequiresAuthAndLimits`), 429 over AI quota.

### T3 List trips — IMPLEMENTED (VERIFIED)
- Dashboard tabs "upcoming"/"past" → `hooks/use-trips.useTrips(status)` → `GET /api/trips?status=upcoming|ongoing|past` → `TripServiceImpl.getTrips` filters `TripClock.phase(trip)` (BEFORE/DURING/AFTER) → `TripMapper.toSummaries` (includes planned-days % via `TripRepository.countPlannedDays`, threshold 2 activities/day). Invalid status → 400.

### T4 Trip detail — IMPLEMENTED (VERIFIED)
- `routes/_layout/trips.$tripId.tsx` loads `useTrip` (`GET /api/trips/{id}`), `useItinerary` (`GET /api/trips/{id}/itinerary`), `useTripWeather` (`GET /api/trips/{id}/weather`). Tabs: itinerary / budget / checklist; floating AI assistant button; `TripHero` actions (generate, phase, review, recap, duplicate, delete).

### T5 Edit trip — BACKEND-ONLY (VERIFIED)
- `PUT /api/trips/{id}` → `TripServiceImpl.updateTrip`: partial update; currency change blocked if expenses exist (422 `CURRENCY_MISMATCH`) or budget/priced activities in old currency (422 `CURRENCY_CHANGE_BLOCKED`); date change → `resizeDays` (keeps day N = start+N−1, adds/drops tail days; dropping days that have activities requires `confirmDropDays=true` else 422 `DAYS_HAVE_ACTIVITIES`; UNIQUE(trip_id,date) is DEFERRABLE so shifting works).
- `tripApi.updateTrip` defined in `client/api/trip-api.ts` but **no component calls it** (only the admin `EditTripDialog` uses the separate admin endpoint). Tests: `TripApiTest.createAndShiftTrip`, `shrinkTripNeedsConfirmation`, `currencyChangeBlockedByRecordedAmounts`.

### T6 Trip phase override — IMPLEMENTED (VERIFIED)
- `TripHero.onChangePhase` → `tripApi.updateTripPhase` → `PUT /api/trips/{id}/phase {phase: BEFORE|DURING|AFTER|null}` → `TripServiceImpl.updatePhase` sets `phase_override` (null = derive from dates). Used to simulate "during trip" (`TripClock.effectiveToday`).

### T7 Delete trip — IMPLEMENTED (VERIFIED)
- `ConfirmDialog` → `tripApi.deleteTrip` → `DELETE /api/trips/{id}` → `TripServiceImpl.deleteTrip` → `tripRepository.delete` (JPA cascade days→activities; DB `ON DELETE CASCADE` for expenses, checklist, conversations, messages, proposals; blog_posts.trip_id SET NULL; place_ratings.trip_id SET NULL).

### T8 Duplicate own trip — IMPLEMENTED (VERIFIED)
- `TripHero` → `DuplicateTripModal` (choose start date, default today+7) → `tripApi.duplicateTrip(tripId,{name:"<name> (Bản sao)", startDate})` → `POST /api/trips/{id}/duplicate` (`DuplicateTripRequest`: startDate `@FutureOrPresent`, name ≤160) → `TripDuplicationService.duplicate`: shift = start − source.start; copy trip fields, days (date+shift, note), activities (status reset to PLANNED, `createdBy=USER`, same place/cost/time), checklist items (done=false, dueDate+shift); not copied: expenses, conversations, proposals, ratings; returns `DuplicateTripResponse{trip, activitiesCopied, checklistItemsCopied, previouslySkipped[]}` (201). UI navigates to the new trip.
- Test: `TripExtrasApiTest.exportAndDuplicate`; cross-user 404 in `CrossUserAccessApiTest.tripLevel`.

### T9 Copy a trip from a community post — IMPLEMENTED, uncommitted (VERIFIED)
- `routes/_layout/community.tsx` `handleCloneTrip(post)` (only for posts with `tripId`) → `DuplicateTripModal` → `tripApi.duplicateTrip(post.tripId, {startDate, name})` → same endpoint. In `TripDuplicationService.duplicate` (uncommitted diff): if the trip is not owned by caller **but `BlogPostRepository.existsByTripId(tripId)`** → source = any trip; the copy is assigned to the **current user**; default name suffix "(sao chép)".
- Note (INFERRED): any authenticated user can copy any trip that has at least one blog post linked, including private fields (budget, group profile, travel plan, notes). There is no test for this branch.

### T10 Export .ics — IMPLEMENTED (VERIFIED)
- `DayTimeline` export button → `tripApi.exportIcs` → `downloadFile` → `GET /api/trips/{id}/export.ics` → `ExportController.exportIcs` → `TripService.getOwnedTrip` + `ItineraryService.getItinerary` → `IcsExporter.export(itinerary, TripClock.zoneOf(trip))` → `text/calendar` attachment named by ASCII slug. Tests: `IcsExporterTest`, `IcsEscapeTest`.

### T11 Destination search/catalog — IMPLEMENTED (VERIFIED)
- `GET /api/destinations?query=` → `DestinationService.searchDestinations` (DB rows first, then Google Text Search; Google failure → DB only). `GET /api/destinations/popular`, `GET /api/destinations/{id}` exist (BACKEND-ONLY). 15 seed destinations in `schema.sql`. Authenticated only (Google billed).

---

## I. Itinerary & activities

### I1 View itinerary — IMPLEMENTED (VERIFIED)
- `GET /api/trips/{id}/itinerary` → `ItineraryServiceImpl.getItinerary`: days (`findByTripIdOrderByDayNumberAsc`), activities with places in one query (`findWithPlacesByTripId`), alternatives in one query; for consecutive activities with places → `DistanceService.estimateTravel` (Haversine distance, speed by transport mode, default MOTORBIKE); `getIdealTimingTip`; totals (distance, activities, estimated cost excluding SKIPPED). UI `components/pages/itinerary/DayTimeline.tsx`, `ActivityCard.tsx`.

### I2 Add activity — IMPLEMENTED (VERIFIED; modal rewritten in uncommitted change)
- `AddActivityModal` tabs: **manual** form (title, type, start/end time default 09:00–11:00, cost, notes) or **saved places** list (`placeApi.getSavedPlaces`) with "quick add" (type inferred from category).
- → `itineraryApi.addActivity` → `POST /api/trips/{tripId}/itinerary/activities` (`CreateActivityRequest`: dayId or dayNumber, title ≤200, activityType required, placeId | placeProvider+placeExternalId, times, estimatedCost ≥0, transportationMode, notes ≤2000) → `ItineraryServiceImpl.addActivity`: owned trip; resolve target day (404 if not in trip; 400 if neither dayId nor dayNumber); `validateTimeRange` (end < start → 400 `INVALID_TIME_RANGE`); `PlaceAdoptionService.resolve(..., PlaceAdoption.ITINERARY)` (DB place, or fetch Google details/candidate cache and insert into `places`); `orderIndex = max+1`; `createdBy=USER`, `status=PLANNED`; cost source USER/PRICE_LEVEL → 201.
- Tests: `ItineraryApiTest.addActivityAdoptsSearchResult`, `timeRangeAndNullCost`; `ItineraryServiceTest`.

### I3 Update activity / execution status — BACKEND-ONLY (VERIFIED)
- `PUT /api/activities/{id}` → `ItineraryServiceImpl.updateActivity` + `applyStatus`: SKIPPED requires `skipReason` (RAIN/TIRED/CLOSED/NO_TIME/OTHER) else 400; only one DOING activity per trip (else 409 `CONFLICT`); actual start/end times recorded beside planned. `itineraryApi.updateActivity` defined but **not called** by any component.

### I4 Delete activity — IMPLEMENTED (VERIFIED)
- `DayTimeline` → `itineraryApi.deleteActivity` → `DELETE /api/activities/{id}` → `ItineraryServiceImpl.deleteActivity` (ownership via `findByIdAndUserId`; delete; re-index remaining order 0..n).

### I5 Reorder activities — BACKEND-ONLY (VERIFIED)
- `PUT /api/itinerary-days/{dayId}/reorder {activityIds}` → `reorderActivities`: list must equal exact set of the day (else 422 `REORDER_SET_MISMATCH`); two-pass index update (temp +1000) to avoid unique collisions. `itineraryApi.reorderActivities` not called by UI (no drag & drop in current UI). Tests `ItineraryApiTest.ownershipAndReorderValidation`, `ItineraryServiceTest`.

### I6 Cost estimate hint — IMPLEMENTED (VERIFIED)
- `AddActivityModal.handleFetchCostHint` → `GET /api/trips/{id}/cost-estimate?placeId&priceLevel&activityType` → `ItineraryServiceImpl.estimateCost` → `CostEstimator.estimate(priceLevel 0–4, type, travelers, currency)` using `tripmind.cost-estimate` tables (VND, USD); returns `suggested`, range, `basis` text, or a `reason` when no data (no guessing; lodging/transport not estimated). Test `CostEstimatorTest`.
- Note: UI sends `priceLevel: 2` as a default when no place price level (INFERRED from `AddActivityModal`).

### I7 Alternatives (BUDGET / BALANCED / PREMIUM) — IMPLEMENTED (VERIFIED)
- `ActivityAlternatives.tsx` → `GET /api/activities/{id}/alternatives` → `ActivityAlternativeService.getOrGenerateAlternatives` (only FOOD/ACCOMMODATION; real places from search, no invented ratings/prices) ; `PUT /api/activities/{id}/alternatives/{altId}/select` → `selectAlternative` (updates the activity's place, title, cost; marks `is_selected`). Also auto-generated after AI itinerary generation. Test `ItineraryApiTest.getAndSelectActivityAlternatives`.

### I8 Day map — IMPLEMENTED, uncommitted (VERIFIED)
- `DayTimeline` renders `<TripMap points=…>` from activities with coordinates; Leaflet markers numbered by order and a polyline route (`showRoute`), CartoDB tiles. No routing API (straight lines). (VERIFIED `TripMap.tsx`.)

---

## AI. AI features

### AI1 AI itinerary generation — IMPLEMENTED (VERIFIED)
- Pre: trip owned, **itinerary empty** (no activities), LLM configured, AI quota not exceeded. UI button "generate" visible only when itinerary empty (`TripHero canGenerate`).
- Flow: `hooks/use-itinerary-generation.start` → `POST /api/trips/{id}/ai/generate` (202, job view) → `ItineraryGenerationService.start` (job stored in Redis `RBucket`, one active job per trip — repeat call returns the same job; push job id to Redisson deque) → UI subscribes `GET …/ai/generate/{jobId}/stream` (SSE events `progress`/`done`/`error`; falls back to polling `GET …/ai/generate/{jobId}` every 2 s, `POLL_INTERVAL_MS = 2000`).
- Worker (2 virtual-thread workers, `pollLastAndOfferFirstTo` processing list, per-trip `RLock`) → `run(job)`: 0.1 read trip, create conversation "Sinh lịch trình tự động" → 0.2 `buildPool`: Google Places queries per category (attractions, restaurants with dietary keywords, cafés, lodging, + top 3 user preferences), excluding places the user DISLIKED, ranked with Bayesian rating; codes P1, P2… → 0.4 `askModel`: fill `prompts/generate-itinerary.md` with trip summary, group summary, weather per day, place pool, disliked places, destination knowledge tips (≤8 active), maxPerDay (pace RELAXED 4 / BALANCED 5 / FAST_PACED 7, −1 if gentle group) → lenient JSON parse → 0.6 `verify`: each item must map to a real pool place or a real search hit (name match ≥ 50 %), no duplicates, not disliked; rejected items listed with reasons → 0.9 save `AiProposalEntity(kind=ITINERARY, PENDING)` → `ProposalApplyService.apply(..., PlaceAdoption.AI_GENERATE, requireEmptyTrip=true)` → `generateAlternatives` → job DONE with `{proposalId, activitiesCreated, rejectedCount, rejected, undoAvailableUntil}`.
- Errors: not configured 503; trip not empty 409 `TRIP_NOT_EMPTY`; quota 429; model returns empty/unreadable plan or no verifiable activity → job FAILED (`AI_PROVIDER_ERROR`), pending proposal marked EXPIRED; crash recovery `@Scheduled recoverStaleJobs` (every 1 min) re-queues jobs stuck in processing.
- Tests: `GenerationApiTest` (2).

### AI2 AI assistant chat — IMPLEMENTED (VERIFIED)
- UI `components/pages/ai/AiChatDrawer.tsx` → `aiApi.streamChat` (fetch SSE, Bearer header) → `POST /api/trips/{id}/ai/chat {message ≤4000, conversationId?}` → `AiController.chat`: `AgentService.precheck` (owned trip 404, conversation belongs to trip 404, LLM configured 503, AI quota 429 — all as JSON before the stream opens) → `SseEmitter` (timeout 90 s) → task on `aiExecutor` (virtual threads) → `AgentService.runTurn`:
  1. open/create conversation, save USER message;
  2. messages = system prompt (`SystemPromptBuilder.build(trip)` from `assistant-system.md`) + last 20 history messages + user message;
  3. loop ≤8 rounds and ≤60 s: `LlmGateway.call(prompt with 13 tool callbacks, internal execution disabled)`; if tool calls → `ToolExecutor.execute` for each (dedupe identical calls BR-503, timeout, truncate result 4000 chars, **always** insert `ai_tool_executions` row, emit `tool_start`/`tool_end`); else final answer;
  4. if no answer → `wrapUp` (one more call without tools) or fallback text;
  5. stream answer as `token` events (24-char chunks — pseudo-streaming, the model call itself is not streamed);
  6. save ASSISTANT message with token usage + attachments (proposalIds, places, checklist) → event `done{messageId, conversationId}`.
  - Errors inside stream → `error{code,message}` event then close.
- Tests: `AiAssistantApiTest` (5), `ToolAuditApiTest` (2).

### AI3 Proposals (view / apply / reject) — IMPLEMENTED (VERIFIED)
- Created by tool `propose_itinerary_changes` → `ProposalService.create` (validates each ADD/REMOVE/UPDATE/REORDER against real data; rejects invalid ops with reasons; duplicate target → reject whole proposal 422 `DUPLICATE_TARGET`; computes `estimatedCostDelta`, `travelTimeDelta`; status PENDING, expires in 30 min; emits `proposal` SSE event). UI renders `ProposalCard` (before/after diff).
- Apply: `aiApi.applyProposal` → `POST /api/trips/{tripId}/ai/apply {proposalId}` (id only — BR-506) → `ProposalApplyService.apply`: prefetch Google places outside tx → tx with trip row lock (`findByIdAndUserIdForUpdate`) → checks: same trip/user (404), PENDING (409 `PROPOSAL_NOT_PENDING`), not expired (409 `PROPOSAL_EXPIRED`), reorders still valid (409 `PROPOSAL_STALE`) → apply ops (activities created with `createdBy=AI`, `fromProposalId`), write `undo_json` with fingerprints, `applied_seq`, status APPLIED → response with `applied`, `skipped`, `undoAvailableUntil`.
- Reject: `POST /api/proposals/{id}/reject` → `ProposalService.reject` (status REJECTED, kept). `GET /api/proposals/{id}` → details.
- Expiry: `ProposalService.expirePending` `@Scheduled` every 1 min.
- Tests: `ProposalApiTest` (11).

### AI4 Undo — IMPLEMENTED (VERIFIED)
- `AiChatDrawer` undo banner (until `undoAvailableUntil`) → `POST /api/trips/{tripId}/ai/undo {proposalId}` → `ProposalApplyService.undo(…, preview=false)`: must be APPLIED (409 `PROPOSAL_NOT_APPLIED`), within 10 min (409 `UNDO_WINDOW_CLOSED`), not blocked by a later applied proposal touching same activities (409 `UNDO_BLOCKED_BY_LATER`); deletes added activities, restores removed ones, reverts updates — but **keeps activities the user edited manually** (fingerprint mismatch → `undoSkipped`); status REVERTED.
- Preview `GET /api/trips/{tripId}/ai/undo/{proposalId}/preview` is BACKEND-ONLY (`aiApi.previewUndo` exists, not used).

### AI5 Explanation — IMPLEMENTED (VERIFIED)
- `ExplanationModal` → `GET /api/activities/{id}/explanation` → `ExplanationService.explain`: proposal summary, reason of the ADD op, tool executions run, rejected candidates.

### AI6 Place candidates / checklist suggestions in chat — PARTIAL (VERIFIED)
- Tools `propose_places` and `suggest_checklist` emit SSE events `places` and `checklist` and store them as message attachments, but `aiApi.streamChat` only handles `token`, `tool_start`, `tool_end`, `proposal`, `done`, `error` → these suggestions are **not displayed** in the current UI. (VERIFIED: `tools/PlaceTools.java:120`, `tools/ChecklistTools.java:72`, `client/api/ai-api.ts` streamChat.)

### AI7 Conversation history — IMPLEMENTED (VERIFIED)
- `GET /api/trips/{id}/ai/conversations`, `GET /api/conversations/{id}/messages` (`ConversationService.list/messages`) used by `AiChatDrawer`.

### AI8 Per-trip audit log — BACKEND-ONLY
- `GET /api/trips/{id}/ai/activity?limit` → `ConversationService.activity`. Not used by UI (admin uses `/api/admin/tool-executions`).

---

## C. Checklist

### C1 Checklist CRUD — IMPLEMENTED (VERIFIED)
- `components/pages/checklist/ChecklistView.tsx` (columns PACK and TODO via `ChecklistColumn`) → `GET /api/trips/{id}/checklist?kind`, `POST /api/trips/{id}/checklist` (array 1–50 of `{kind, title ≤120, category, dueDate (TODO only), source, reason}`), `PATCH /api/checklist-items/{id}` (title, done, category, dueDate), `DELETE /api/checklist-items/{id}` → `ChecklistService` (`add` skips duplicates by kind+lower(title); also DB unique index `uq_checklist_trip_kind_title`).

### C2 Rule-based suggestions — IMPLEMENTED (VERIFIED)
- `ChecklistSuggestionsDialog` → `GET /api/trips/{id}/checklist/suggestions` → `ChecklistService.suggestions` → `ChecklistSuggester.suggest(trip, activities, WeatherService.forTrip(trip))`: rules on rain probability ≥50 % / rain ≥5 mm, hot ≥32 °C, cold ≤15 °C, long trip ≥4 days, abroad (not in home countries → passport, currency), kids, flights (online check-in TODO), beach activities, etc.; each with `reason`; weather failure → rule group skipped and listed in `skippedRules`. Nothing saved until user selects → `POST …/checklist` with `source=SUGGESTED`. Tests: `ChecklistSuggesterTest`, `TripExtrasApiTest.checklist`.

---

## B. Budget & expenses

### B1 Budget summary — IMPLEMENTED (VERIFIED)
- `components/pages/budget/BudgetView.tsx` → `GET /api/trips/{id}/budget` → `BudgetServiceImpl.summarize`: estimated total (sum of activity costs by type, excluding SKIPPED) vs actual (expenses by category); remaining = budget − max(estimated, actual); warning NONE (no budget, or within 90 %) / NEAR_LIMIT (> 90 %) / OVER (> 100 %); daily allowance: BEFORE → average per day after pre-paid expenses; DURING → (budget − spent before today) ÷ remaining days incl. today; AFTER/no budget → reason. Tests: `BudgetServiceImplTest` (9), `BudgetApiTest`.

### B2 Expenses — IMPLEMENTED (create/list/delete in UI; update BACKEND-ONLY) (VERIFIED)
- `ExpenseForm` → `POST /api/trips/{id}/expenses` (`ExpenseRequest`: category, amount ≥0, currency must equal trip currency → else 422 `CURRENCY_MISMATCH`, description, expenseDate, activityId optional must be in trip); `GET …/expenses?category&from&to` (JPA Specification, newest first); `PUT /api/expenses/{id}` (no UI); `DELETE /api/expenses/{id}`. No multi-currency conversion; `paid_by` / `share_with` columns unused by API (split bill not implemented).

---

## W. Weather

### W1 Trip weather — IMPLEMENTED (VERIFIED)
- `useTripWeather` → `GET /api/trips/{id}/weather` → `WeatherService.getTripWeather` (no transaction): per day source FORECAST (today−90 … today+15 via forecast API), OBSERVED (past), CLIMATE_NORMAL (average of same day over 3 past years from archive; Feb 29 handled); Redis cache 3 h; partial failures → `reason=PARTIAL`; total failure → empty days + `WEATHER_UNAVAILABLE` (still 200). Used by AI tools, generation prompt and checklist rules. Tests: `WeatherServiceTest` (6).

---

## P. Places

### P1 Explore places — IMPLEMENTED (VERIFIED; extended in uncommitted change)
- `routes/_layout/places.tsx` → `placeApi.search(query, destination)` → `GET /api/places/search?query&destination` → `PlaceServiceImpl.searchPlaces`: Google Text Search (`query + destination`) → cache candidates in Redis (`PlaceCandidateCache`) → map to `PlaceResponse` (provider GOOGLE, externalId, **no DB id**); if Google empty/failed → local `places` table LIKE search. Search **does not insert** places (adoption only on user action). Results shown as cards + `TripMap` markers; "open in Google Maps" link. Default query "Đà Nẵng" when empty.

### P2 Saved places — PARTIAL (INFERRED bug)
- List: `GET /api/me/saved-places` (used by Explore page and AddActivityModal). Unsave: `DELETE /api/places/{placeId}/save`.
- Save: Explore page calls `placeApi.savePlace(place.id)` → `POST /api/places/{placeId}/save` (requires a DB id). Google search results have no `id` (see P1), so saving a Google result from the Explore page would call `/api/places/undefined/save` → 400 type-mismatch. The correct endpoint `POST /api/me/saved-places {provider, externalId}` (`SavedPlaceService.saveExternalPlace`, adopts with `PlaceAdoption.SAVED`) exists and `placeApi.saveByExternalId` is defined, but is **not used**. Works only for local-catalog results (Google disabled/mocked) — INFERRED, not executed.

### P3 Nearby / details — BACKEND-ONLY (VERIFIED)
- `GET /api/places/recommendations/nearby?lat&lng&radius&category` — note: implementation ignores lat/lng/radius and just does a text search for the category (default "attractions"), else last 20 DB places (VERIFIED `PlaceServiceImpl.getNearbyRecommendations`).
- `GET /api/places/{provider}/{externalId}` → DB or Google details.

### P4 Rate places after trip — IMPLEMENTED (VERIFIED)
- Only when `trip.phase === "AFTER"` UI shows "review" → `PlaceReviewModal` → `GET /api/trips/{id}/review/places` (places in itinerary, excluding skipped, with current verdict) and `PUT /api/trips/{id}/places/{placeId}/rating {verdict: LIKE|DISLIKE, note?}` → `PlaceRatingService.rate` (trip must have ended else 409 `TRIP_NOT_ENDED`; one rating per user+place, overwrite). DISLIKE places are filtered out of `search_places` and AI generation pools. `DELETE …/rating`, `GET /api/me/place-ratings` BACKEND-ONLY. Test: `TripExtrasApiTest.ratingsFeedSearch`.

---

## CM. Community (blog)

### CM1 Feed — IMPLEMENTED (VERIFIED)
- `routes/_layout/community.tsx`: destination chips (`GET /api/blog/posts/destinations`), search box, infinite scroll (`useInfiniteQuery`) → `GET /api/blog/posts?destination&search&page&size(1–100)` → `BlogService.getPosts` (`BlogPostRepository.findFiltered`: exact case-insensitive destination, LIKE on title/content, newest first) + `isUpvoted` for current user. Public on backend; UI requires login (route under `_layout`). Author shown as `PublicUserResponse` (no email/role — test `SecurityHardeningApiTest.blogPublicResponsesHidePrivateFields`). `PostCard.tsx` renders content (markdown-ish), photos, trip-copy button when `tripId`.

### CM2 Create post — IMPLEMENTED (VERIFIED)
- `CreatePostDialog` → `blogApi.createPost` → `POST /api/blog/posts` (`BlogPostRequest`: destinationName ≤160, title ≤200, content ≤20000, tripId optional, photoUrls ≤10 https URLs) → `BlogService.createPost`: trip must be the user's own (else 404). Status 201.

### CM3 Delete post — IMPLEMENTED (VERIFIED)
- `DELETE /api/blog/posts/{id}` → author or ADMIN, else 403 `FORBIDDEN`. Cascade deletes comments/upvotes.

### CM4 Upvote post — PARTIAL (VERIFIED backend; INFERRED UI bug)
- `POST /api/blog/posts/{id}/upvote` → `BlogService.togglePostUpvote`: atomic `insertIfAbsent` (native ON CONFLICT) else delete; recount from upvote table (`syncUpvoteCount`); returns `{upvoted, upvoteCount}`.
- UI (`community.tsx` `upvoteMutation.onSuccess`) treats the result as a `BlogPost` and updates the cached post where `p.id === updated.id` — the response has no `id`, so the card count/state is not updated until refetch (INFERRED from code; TypeScript typing `request<BlogPost>` hides the mismatch). Test of backend consistency: `SecurityHardeningApiTest.upvoteToggleIsConsistent`.

### CM5 Comments — IMPLEMENTED (VERIFIED)
- `PostComments.tsx` → `GET /api/blog/posts/{id}/comments` (public, oldest first, with `isUpvoted`), `POST …/comments {content ≤2000}` (auth; increments `comment_count`), `POST /api/blog/comments/{id}/upvote` (toggle).

### CM6 AI trip recap — IMPLEMENTED, uncommitted (VERIFIED)
- UI: `TripHero` "AI Viết Review Tổng Kết" button (only when phase AFTER) → `blogApi.generateTripRecap` → `POST /api/blog/trips/{tripId}/recap` → `BlogController.generateTripRecap` → `BlogService.generateTripRecap` (`@Transactional(readOnly=true)`): owned trip (404) → non-skipped activities with places → distinct place names; expenses total; user's place ratings for those places → Vietnamese prompt asking for JSON `{title, content(Markdown)}` → `LlmGateway.call` → parse first `{…}`; on missing LLM or any error → template-built Markdown (places, spend, ratings, tips) → `TripRecapResponse{title, destinationName, content, tripId, totalSpent, currency, placesVisited, photoUrls=[]}`. UI opens `CreatePostDialog` pre-filled; user edits and publishes via CM2 (post linked to the trip).
- Notes: no AI rate-limit check on this endpoint (unlike chat/generate/parse-prompt) (VERIFIED absence); LLM call happens inside a read-only transaction (holds a DB connection up to the 2-min default budget) (INFERRED); not covered by tests.

### CM7 Not implemented
- Edit post, edit/delete comment, report/moderation queue, image upload. (VERIFIED absence in `BlogController`.)

---

## AD. Administration (role ADMIN; UI `routes/_layout/admin.tsx` tabs: Metrics, Users, Trips, Tools, Knowledge)

| Feature | API → service | UI | Tag |
|---|---|---|---|
| AD1 AI usage metrics (turns, tokens, tool calls, error rate, top tools, per day; default last 30 days) | `GET /api/admin/ai-usage?from&to` → `AdminService.aiUsage` | `MetricsTab.tsx` | VERIFIED |
| AD2 Tool execution log, filters user/tool/status/date, paged | `GET /api/admin/tool-executions` → `AdminService.toolExecutions` | `ToolsTab.tsx` | VERIFIED |
| AD3 Users: list with trip count; create (email, password, name, role); update (name, role, active, password — deactivation or password reset bumps token_version); delete (not self → 400) | `GET/POST /api/admin/users`, `PUT/DELETE /api/admin/users/{id}` | `UsersTab.tsx`, `UserDialogs.tsx` | VERIFIED |
| AD4 Trips: summary list (no itinerary content); edit name/phase/budget; delete any trip | `GET /api/admin/trips`, `PUT/DELETE /api/admin/trips/{id}` (`updateTrip` takes raw `Map`) | `TripsTab.tsx`, `EditTripDialog.tsx` | VERIFIED |
| AD5 Knowledge dataset CRUD, filter (destination/category/search), toggle active | `/api/admin/knowledge/**` → `DestinationKnowledgeService` | `KnowledgeManagementTab.tsx`, `KnowledgeTable.tsx`, `KnowledgeFormDialog.tsx` | VERIFIED; tests `AdminKnowledgeApiTest`, `KnowledgePromptApiTest` |
| AD6 Extract knowledge from post (AI, prompt-injection-guarded with `<untrusted_…>` tags, fallback to raw post) then admin confirms convert → `destination_knowledge` row linked from `blog_posts.converted_to_knowledge_id` | `POST /api/admin/blog/posts/{id}/extract-knowledge`, `POST /api/admin/blog/posts/{id}/convert-to-knowledge` → `BlogService.extractKnowledgeFromPost/convertPostToKnowledge` | `ExtractKnowledgeModal.tsx` (from community page, admin only) | VERIFIED; test `SecurityHardeningApiTest.blogDeleteAndAdminExtractPermissions` (USER → 403) |

Note: `AdminController` Javadoc still says "read-only", but it now contains write endpoints (VERIFIED inconsistency). Admin "manage destinations / places / API usage / system logs" from the design doc are not implemented.

---

## X. Cross-cutting UI
- Theme toggle (`contexts/theme-context.tsx`, `next-themes`), toasts (`sonner`), accessible Radix dialogs (`components/ui/dialog.tsx`, `confirm-dialog.tsx`), responsive mobile drawer navigation in `Header.tsx` (uncommitted). (VERIFIED.)
- Routes: `/login`, `/register`, `/` (My trips dashboard), `/trips/$tripId`, `/places`, `/community`, `/admin`; `NotFound` component. (VERIFIED `routeTree.gen.ts`.)

---

## Z. In design documents but NOT in code (PROPOSED-ONLY)

| Proposed item | Source | Evidence of absence |
|---|---|---|
| Refresh token rotation, server logout | design doc §4.1, §23; ADS | no endpoints; `BE-TODO.md` marks "Chưa làm" |
| Dashboard widgets: budget alerts, weather alerts, AI suggestions, saved places on dashboard | design doc §5 | `routes/_layout/index.tsx` shows trip cards + filters only |
| Public landing / Explore / Destination detail pages | design doc §24 | all app routes behind login |
| Trip workspace tabs Overview / Map / Places as separate tabs | design doc §24 | tabs: itinerary, budget, checklist (map embedded in timeline) |
| Currency conversion (Frankfurter), exchange rates | design doc §28 | no client; currency change blocked |
| Nominatim / OSM / Mapbox providers | design doc §28, §40 | only Google + Open-Meteo geocoding; `destinations.provider` allows `MAPBOX` but unused |
| `weather_cache` table | design doc §20.13 | replaced by Redis cache |
| Admin: manage destinations, places, API usage, system logs | design doc §3.2, §24 | not implemented |
| Zustand state store | design doc §25, §40 | not used |
| Nginx reverse proxy + frontend container in compose | design doc §39 | compose has postgres/redis/backend only |
| Drag & drop reorder in UI, mark activity DOING/DONE/SKIPPED, edit trip form | design doc §8; `demo/` | backend exists, UI not wired |
| Expense split (paid_by / share_with, trip_participants), travel journal, photos | `schema.sql` tables `trip_participants`, `journal_entries`, `photos`; `demo/tripmind-demo.html` | no entities/API (schema-only) |
| Expense proposals from chat (`ProposalKind.EXPENSE`, `ExpenseSource.CHAT`, `ai_proposals.expense_json`) | enums/schema | no code path creates them (INFERRED from grep: only ITINERARY proposals are built) |
| Email verification / password reset | — | no mail dependency |
| Collaboration / sharing a trip with other users | design doc mentions travelers only | no share endpoints; only community post + copy |
