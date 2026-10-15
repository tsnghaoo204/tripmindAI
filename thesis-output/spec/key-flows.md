# Key Flows — call sequences for sequence/activity diagrams (internal evidence)

Each flow lists participants (left→right lifelines) and numbered calls. Names are exact identifiers from the code. Paths: frontend `frontend/src/…`, backend `backend/src/main/java/com/tripmind/…`. All flows VERIFIED by reading code unless a step is tagged INFERRED. Cross-cutting steps omitted from each flow for brevity (apply to every authenticated call): `axios request interceptor` adds `Authorization: Bearer` → `RequestSizeLimitFilter` → Spring Security chain → `JwtAuthenticationFilter.doFilterInternal` (`JwtTokenProvider.getClaims` → `CustomUserDetailsService.loadUserById` → token-version check) → `ApiRateLimitFilter` (`RateLimiter.check("api",…)`) → controller → `SecurityUtils.getCurrentUserId()`; responses wrapped in `ApiResponse`; errors via `GlobalExceptionHandler`; frontend `client.ts` response interceptor (401 → `logout()`), components show `toast` via `getErrorMessage`.

Generic participants abbreviations: **UI** = React component, **API** = `client/api/*-api.ts` function (axios `request<T>`), **Ctl** = controller, **Svc** = service, **Repo** = Spring Data repository, **DB** = PostgreSQL, **Redis**, **LLM** = OpenAI-compatible endpoint, **Google** = Google Places API, **OM** = Open-Meteo.

---

## F1. Register

Participants: User → `RegisterPage` (`routes/register.tsx`) → `authApi.register` → `AuthController.register` → `AuthService.register` → `UserRepository` → DB; `JwtTokenProvider`.

1. User submits name/email/password → `RegisterPage.handleSubmit`.
2. `authApi.register({name,email,password})` → `POST /api/auth/register` (public; no JWT).
3. Spring validates `@Valid RegisterRequest` → on failure 400 `VALIDATION_ERROR` (field map in `details`) → alt **[invalid]**: toast error, stay on page.
4. `AuthController.register` → `AuthService.register(request)`:
   4.1 normalize email (`toLowerCase().trim()`);
   4.2 `UserRepository.existsByEmail(email)` → alt **[exists]**: throw `AppException(CONFLICT)` → 409 → toast.
   4.3 `PasswordEncoder.encode(password)` (BCrypt);
   4.4 `UserRepository.save(UserEntity{role=USER, isActive=true})` → INSERT users;
   4.5 `toAuthResponse` → `JwtTokenProvider.generateToken(UserPrincipal.fromEntity(user))`.
5. 201 `AuthResponse` → UI ignores token, `toast.success`, `navigate({to:"/login"})`.

## F2. Login (and session handling)

Participants: User → `LoginPage` (`routes/login.tsx`) → `authApi.login` → `AuthController.login` → `LoginAttemptLimiter` (Redis) → `AuthService.login` → `UserRepository` → DB → `JwtTokenProvider`; then `AuthProvider`/`auth-storage` (localStorage).

1. `LoginPage.handleSubmit` → `authApi.login({email,password})` → `POST /api/auth/login`.
2. `AuthController.login`: `ip = request.getRemoteAddr()`.
3. `LoginAttemptLimiter.ensureAllowed(email, ip)` → Redis GET `login-fail:email:<email>` and `login-fail:ip:<ip>` → alt **[≥5 / ≥20 failures]**: throw `RATE_LIMITED` → 429 `{retryAfterSeconds}` → toast. (Redis error → allow.)
4. `AuthService.login(request)` → `UserRepository.findByEmail(normalized)`:
   - alt **[not found]** → `AppException(UNAUTHORIZED)`;
   - `PasswordEncoder.matches` alt **[mismatch]** → `UNAUTHORIZED`;
   - alt **[!isActive]** → `UNAUTHORIZED` "deactivated".
   - On `UNAUTHORIZED`: controller `LoginAttemptLimiter.recordFailure(email, ip)` (Redis Lua INCR+EXPIRE 5 min) then rethrow → 401 → toast "Invalid email or password".
5. Success: `JwtTokenProvider.generateToken` (sub, email, role, name, tv; exp 24 h) → `LoginAttemptLimiter.reset(email)` (Redis DEL) → 200 `AuthResponse{accessToken, user}`.
6. UI: `useAuth().login(token, user)` → `saveSession` → localStorage `access_token`, `user_profile` → `notify()` listeners → `router.history.push(safeRedirect(redirect))`.
7. Subsequent navigation: `_layout.beforeLoad` checks `getToken()`; `admin.beforeLoad` checks `getStoredUser().role === "ADMIN"`.
8. Logout (client only): `Header` logout → `lib/auth-storage.logout()` → `clearSession()` → `unauthorizedHandler` (`main.tsx`): `queryClient.clear()`, `router.navigate("/login")`. No backend call.

## F3. Create trip manually (with optional AI prompt prefill)

Participants: User → `HomePage` (`routes/_layout/index.tsx`) → `CreateTripModal` → [`lib/prompt-parser.parseTripPromptWithAi` → `tripApi.parsePrompt` → `TripController.parsePrompt` → `RateLimiter` → `TripPromptParserService` → LLM / `DestinationRepository`] → `tripApi.getDestinations` → `DestinationController` → `DestinationService` → DB/Google → `tripApi.createTrip` → `TripController.createTrip` → `TripServiceImpl.createTrip` → `UserRepository`, `DestinationService`, `UserPreferencesRepository`, `TripRepository` → DB.

Optional prefill (alt **[user typed a description]**):
1. `CreateTripModal` → `parseTripPromptWithAi(text, destinations)` → `tripApi.parsePrompt(prompt)` → `POST /api/trips/parse-prompt`.
2. `TripController.parsePrompt` → `RateLimiter.check("ai", userId, 20, 1h)` (alt 429) → `TripPromptParserService.parsePrompt(prompt)`:
   - alt **[LLM configured]**: `LlmGateway.call(Prompt(system JSON-extraction prompt, user text), deadline)` → LLM → parse JSON → clamp values → `matchDestination(name)` → `DestinationRepository.findByNameContainingIgnoreCase` (accent-insensitive compare) → Map.
   - alt **[LLM missing or error/invalid JSON]**: `dynamicFallbackParse` (regex) → Map.
3. Response pre-fills wizard fields (frontend may also apply local `parseTripPromptDynamic` fallback if the API fails — INFERRED from `lib/prompt-parser.ts`).

Main creation:
4. Step 1 destination: `tripApi.getDestinations(query)` → `GET /api/destinations?query=` → `DestinationService.searchDestinations` → DB rows + `GooglePlacesClient.searchText` (alt Google failure → DB only).
5. Steps 2–4 (dates, travellers & group, budget/currency/style/preferences/travel plan) — client-side state only.
6. Submit → `tripApi.createTrip(payload)` → `POST /api/trips` (`CreateTripRequest`; alt **[validation]** 400, e.g. startDate in past).
7. `TripServiceImpl.createTrip(userId, req)`:
   7.1 `UserRepository.findById` (404 if missing);
   7.2 `validateDateRange` → alt 400 `INVALID_DATE_RANGE` / 422 `TRIP_TOO_LONG` (>30 days);
   7.3 `validateGroup` → alt 400;
   7.4 `DestinationService.getOrCreateDestination(destinationId, destinationPlaceId, destinationData)`:
       - by id → `DestinationRepository.findById` (alt 404);
       - by Google placeId → `resolveByPlaceId` → existing row or `GooglePlacesClient.getPlaceDetails` + `resolveTimezone` → INSERT destinations (alt 502 `EXTERNAL_SERVICE_ERROR`);
       - by manual input → `resolveByInput` → if no coordinates `GeocodingClient.search(name,country)` (Open-Meteo) → INSERT/merge destinations;
   7.5 `UserPreferencesRepository.findByUserId` → fill defaults;
   7.6 build `TripEntity` + N × `ItineraryDayEntity` (one per date);
   7.7 alt **[no defaults yet]** `UserPreferencesRepository.save(...)`;
   7.8 `TripRepository.save(trip)` → INSERT trips + itinerary_days (cascade);
   7.9 `TripMapper.toDetail(trip)`.
8. 201 `TripResponse` → toast → `onSuccess(created)` → dashboard (`index.tsx`) invalidates `["trips"]` so the new card appears; modal closes (no automatic navigation). The dashboard also accepts an `initialPrompt` from a home prompt box (VERIFIED `index.tsx`).

## F4. AI generate itinerary (background job + SSE)

Participants: User → `TripDetailPage`/`TripHero` → `useItineraryGeneration` hook → `aiApi.startGenerateItinerary` / `aiApi.streamGenerateStatus` (`client/sse.ts`) / `aiApi.getGenerateStatus` → `ProposalController` → `ItineraryGenerationService` → Redis (Redisson queue/bucket/topic/lock) → worker thread → `TripService`, `ConversationService`, `PlaceService`/`GooglePlacesClient`, `PlaceRatingLookup`, `WeatherService`/OM, `DestinationKnowledgeService`, `LlmGateway`/LLM, `AiProposalRepository`, `ProposalApplyService`, `ActivityAlternativeService` → DB.

Request phase:
1. `TripHero` "generate" (shown only if itinerary empty) → `useItineraryGeneration.start()` → `aiApi.startGenerateItinerary(tripId)` → `POST /api/trips/{tripId}/ai/generate`.
2. `ProposalController.generate` → `ItineraryGenerationService.start(userId, tripId)`:
   2.1 `TripService.getOwnedTrip` (alt 404);
   2.2 alt **[LLM not configured]** 503 `AI_NOT_CONFIGURED`;
   2.3 `ActivityRepository.countByItineraryDayTripId > 0` → alt 409 `TRIP_NOT_EMPTY`;
   2.4 Redis `RBucket active:{tripId}` → alt **[job running]** return existing job view (same jobId);
   2.5 `RateLimiter.check("ai", …)` → alt 429;
   2.6 create `ItineraryJobPayload{state:QUEUED}` → `saveJob` (Redis bucket, TTL 2 h) → `active.setIfAbsent` → `RBlockingDeque(queue).addFirst(jobId)`.
3. 202 job view `{jobId, state, step, progress}` → UI shows progress card.
4. UI `aiApi.streamGenerateStatus(tripId, jobId)` → `GET …/ai/generate/{jobId}/stream` → `ItineraryGenerationService.subscribe`: subscribe `RTopic topic:{jobId}` first, then emit current snapshot; events `progress` / `done` / `error`. alt **[stream error/interrupted]** → UI polls `GET …/ai/generate/{jobId}` (`status`) every 2 s.

Worker phase (async, `workerLoop` on `aiExecutor`):
5. `queue.pollLastAndOfferFirstTo(processing)` → `processJob(jobId)` → `getJob` → `RLock lock:{tripId}.tryLock` (alt not acquired → requeue).
6. `run(job)`:
   6.1 `updateJob(RUNNING, 0.1)` (save + `publishJobEvent` → RTopic → SSE `progress`); `TripService.getOwnedTrip`; `ConversationService.openOrCreate(…, "Sinh lịch trình tự động")`; compute `maxPerDay` from travel style & group.
   6.2 `updateJob(0.2)` → `buildPool(trip)`: for each `PoolQuery` (attractions, restaurants + dietary keywords, cafés, lodging, preferences) → `PlaceService`/`GooglePlacesClient.searchText` (each search logged in `ai_tool_executions`) → drop `PlaceRatingLookup.dislikedExternalIds(user)` → rank (Bayesian popularity) → codes P1…Pn.
   6.3 `updateJob(0.4)` → `askModel`: `WeatherService.forTrip` (OM, Redis cache), `PlaceRatingLookup.dislikedNames`, `DestinationKnowledgeService.getFormattedKnowledgeForPrompt` (DB), `SystemPromptBuilder.tripSummary/groupSummary` → fill `prompts/generate-itinerary.md` → `LlmGateway.call(prompt, now+2 min)` → LLM → `parsePlan` (lenient JSON). alt **[empty/unreadable]** → `AppException(AI_PROVIDER_ERROR)`.
   6.4 `updateJob(0.6)` → `verify(...)`: each item → pool code match or `findReal` (Google search + `nameMatch ≥ 0.5`) → build `ProposalChange.Add`; collect `Rejected` reasons. alt **[no valid items]** → `AI_PROVIDER_ERROR`.
   6.5 `updateJob(0.9)` → `AiProposalRepository.save(AiProposalEntity{kind ITINERARY, PENDING, changes_json, evidence_json})`.
   6.6 `ProposalApplyService.apply(userId, tripId, proposalId, AI_GENERATE, requireEmptyTrip=true)` (see F5 step 8, writes activities with `createdBy=AI`, `fromProposalId`).
   6.7 `generateAlternatives(tripId)` → `ActivityAlternativeService.generateForActivity` for FOOD/ACCOMMODATION activities (Google search; failures logged and ignored).
   6.8 `job.setResult(...)`; `updateJob(DONE, 1.0)` → RTopic → SSE `done` → UI `finish()` → toast + `refreshTrip()` (re-fetch trip/itinerary).
   - alt **[AppException/Exception]** → `discardProposal` (PENDING → EXPIRED) → `updateJob(FAILED)` → SSE `error` → UI toast error.
7. `processing.remove(jobId)`; unlock. Crash recovery: `@Scheduled recoverStaleJobs` re-queues stuck jobs.

## F5. AI assistant chat → proposal → apply / reject / undo

Participants: User → `AiChatDrawer` → `aiApi.streamChat` (`openSseStream` fetch) → `AiController.chat` → `AgentService` (+ `ConversationService`, `SystemPromptBuilder`, `ToolRegistry`, `ToolExecutor`, tool beans e.g. `TripReadTools`/`PlaceTools`/`ProposalTools`, `ProposalService`) → `LlmGateway`/LLM → Repos/DB; then `aiApi.applyProposal` → `ProposalController.apply` → `ProposalApplyService` → `PlaceAdoptionService`/Google → DB.

1. User sends message → `AiChatDrawer` appends user bubble + empty assistant bubble → `aiApi.streamChat(tripId, {message, conversationId})` → `POST /api/trips/{tripId}/ai/chat` (fetch with Bearer).
2. `AiController.chat` → `AgentService.precheck`: `TripService.getOwnedTrip` (404), `ConversationService.requireConversation` (404), `LlmGateway.isConfigured` (503), `RateLimiter.check("ai")` (429) — errors returned as normal JSON responses (UI `onError`).
3. Create `SseEmitter(90 s)` → `aiExecutor.submit(runTurn)` → return emitter (HTTP 200 text/event-stream).
4. `AgentService.runTurn`: `ConversationService.openOrCreate` → `saveUserMessage` (INSERT messages) → build messages [`SystemPromptBuilder.build(trip)`, `ConversationService.history(…)`, user] → options with `ToolRegistry.all()` and `internalToolExecutionEnabled(false)`.
5. Loop (≤ `maxRounds`=8 and before deadline 60 s):
   5.1 `LlmGateway.call(prompt, deadline)` → `OpenAiLlmGateway` tries models in order → LLM.
   5.2 alt **[no tool calls]** → answer, exit loop.
   5.3 alt **[tool calls]** → for each: `ToolExecutor.execute(call, turn)` → emit `tool_start` → duplicate check → run tool method with `ToolContext(turn)` under timeout → INSERT `ai_tool_executions` (OK/ERROR/TIMEOUT) → emit `tool_end` → result JSON (truncated) appended as `ToolResponseMessage`.
       - Example tool `propose_itinerary_changes` (`ProposalTools`) → `ProposalService.create(turn, summary, reason, inputs)`: check duplicate targets → `PlaceVerifier.verify` each place (candidate cache / DB) → in short tx build Add/Remove/Update/Reorder against real days/activities, compute cost & travel-time delta → INSERT `ai_proposals` (PENDING, expires +30 min) → emit `proposal{proposalId, summary,…}` (after commit).
       - `search_places`, `propose_places` (emit `places`), `suggest_checklist` (emit `checklist`), read tools, `optimize_day_order` (`DayOrderService` → `ItineraryOptimizer`).
   5.4 alt **[AI_TIMEOUT]** → break.
6. alt **[no answer]** → `wrapUp` (one call without tools; fallback text if <3 s left or failure).
7. `streamText(answer)` → `token` events → `ConversationService.saveAssistantMessage(content, usage, attachments, executionIds)` → emit `done{messageId, conversationId}` → `emitter.complete()`. alt **[exception]** → emit `error{code,message}` → complete.
   UI: `onToken` appends text; `onProposal` → `addProposal` → renders `ProposalCard`; `onDone` stores conversationId; `places`/`checklist` events are ignored by the current UI.
8. Apply: `ProposalCard` "apply" → `AiChatDrawer.handleApplyProposal` → `aiApi.applyProposal(tripId, proposalId)` → `POST /api/trips/{tripId}/ai/apply {proposalId}` → `ProposalApplyService.apply(userId, tripId, proposalId)`:
   8.1 outside tx: `AiProposalRepository.findById` → `PlaceAdoptionService.prefetch(places of ADD ops)` (Google details for places not yet in DB);
   8.2 `TransactionTemplate.execute(applyLocked)`: `TripRepository.findByIdAndUserIdForUpdate` (row lock; alt not owner → 404 `PROPOSAL_NOT_FOUND`) → `AiProposalRepository.findByIdForUpdate` → alt status≠PENDING 409 `PROPOSAL_NOT_PENDING` → alt expired 409 `PROPOSAL_EXPIRED` → `checkReordersStillValid` (alt 409 `PROPOSAL_STALE`) → for each change `applyAdd/applyRemove/applyUpdate/applyReorder` (skips with reason when target vanished) → flush → `reindex` touched days → set APPLIED, `appliedAt`, `appliedSeq`, `undo_json` (fingerprints) → save → `ItineraryService.getItinerary`.
   8.3 200 `ApplyProposalResponse{applied, skipped, undoAvailableUntil, itinerary}` → UI marks APPLIED, shows undo banner, `onProposalApplied` → `refreshTrip()`.
9. Reject: `aiApi.rejectProposal` → `POST /api/proposals/{id}/reject` → `ProposalService.reject` → status REJECTED.
10. Undo (within 10 min): `aiApi.undoProposal` → `POST /api/trips/{tripId}/ai/undo` → `ProposalApplyService.undo(…, preview=false)` → checks APPLIED (409), window (409 `UNDO_WINDOW_CLOSED`), `checkNotBlocked` (409 `UNDO_BLOCKED_BY_LATER`) → for each undo op compare `fingerprint(activity)`; unchanged → delete/restore/revert; changed by user → keep and report → status REVERTED → UI marks REVERTED.
11. Background: `ProposalService.expirePending` (every minute) marks overdue PENDING proposals EXPIRED.

## F6. Add activity to itinerary

Participants: User → `DayTimeline` → `AddActivityModal` → (`placeApi.getSavedPlaces` → `SavedPlaceController` → `SavedPlaceServiceImpl` → `SavedPlaceRepository`) → (`itineraryApi.getCostEstimate` → `ItineraryController.estimateCost` → `ItineraryServiceImpl.estimateCost` → `PlaceRepository`, `CostEstimator`) → `itineraryApi.addActivity` → `ItineraryController.addActivity` → `ItineraryServiceImpl.addActivity` → `TripRepository`, `ItineraryDayRepository`, `PlaceAdoptionService` (→ `PlaceRepository` / `PlaceCandidateCache` / `GooglePlacesClient`), `ActivityRepository` → DB.

1. User clicks "add" on a day in `DayTimeline` → opens `AddActivityModal(tripId, dayNumber)`.
2. Modal loads saved places: `placeApi.getSavedPlaces()` → `GET /api/me/saved-places` → `SavedPlaceServiceImpl.getSavedPlaces(userId)`.
3. alt **[pick a saved place]** → `handleSelectSavedPlace` fills title/type (inferred from category) or `handleQuickAdd(place, start, end)` submits immediately.
4. alt **[cost hint]** → `handleFetchCostHint` → `GET /api/trips/{id}/cost-estimate?placeId&priceLevel=2&activityType` → `ItineraryServiceImpl.estimateCost` → owned trip → `PlaceRepository.findById` (place price level overrides param) → `CostEstimator.estimate(level, type, travelers, currency)` → `{suggested, basis}` or `{reason}`.
5. Submit → `itineraryApi.addActivity(tripId, {dayNumber, title, activityType, placeId?, placeExternalId?, startTime, endTime, estimatedCost?, notes})` → `POST /api/trips/{tripId}/itinerary/activities`.
6. `ItineraryServiceImpl.addActivity`:
   6.1 `TripRepository.findByIdAndUserId` (alt 404);
   6.2 target day: `ItineraryDayRepository.findById(dayId)` filtered by trip, or `findByTripIdAndDayNumber` (alt 404; alt neither → 400);
   6.3 `validateTimeRange` (alt 400 `INVALID_TIME_RANGE`);
   6.4 `PlaceAdoptionService.resolve(placeId, provider, externalId, ITINERARY)`: placeId → `PlaceRepository.findById` (alt 404); externalId → existing row by (provider, externalId) or candidate cache / `GooglePlacesClient.getPlaceDetails` → INSERT places (`adopted_via=ITINERARY`) (alt 404 not resolvable; concurrent insert → 409); none → null place;
   6.5 `ActivityRepository.findMaxOrderIndexByItineraryDayId` → next index;
   6.6 `ActivityRepository.save(ActivityEntity{createdBy USER, status PLANNED})` → INSERT activities;
   6.7 `toActivityResponse` (alternatives lookup, timing tip).
7. 201 → toast, `onSuccess()` → invalidate itinerary query → `DayTimeline` re-renders (map points updated in `TripMap`).

## F7. Manage checklist

Participants: User → `ChecklistView` / `ChecklistColumn` / `ChecklistSuggestionsDialog` → `checklistApi.*` → `ChecklistController` → `ChecklistService` → `TripService.getOwnedTrip`, `ChecklistItemRepository`, (`ActivityRepository`, `WeatherService` → Redis/OM, `ChecklistSuggester`) → DB.

1. Load: `checklistApi.getChecklist(tripId)` → `GET /api/trips/{id}/checklist` → `ChecklistService.list` → `getOwnedTrip` (404) → `ChecklistItemRepository.findByTripIdOrderByKindAscOrderIndexAscIdAsc` → items rendered in PACK and TODO columns.
2. Add manual item: `checklistApi.createItems(tripId, [{kind, title, category?, dueDate?}])` → `POST /api/trips/{id}/checklist` (1–50 items, `@Valid`) → `ChecklistService.add`: existing keys `kind:lower(title)` → skip duplicates → `ChecklistItemRepository.maxOrderIndex(tripId, kind)` → `save` per new item (`source` default USER; dueDate kept only for TODO) → 201 created list (may be empty if all duplicates).
3. Suggestions: `ChecklistSuggestionsDialog` → `checklistApi.getSuggestions` → `GET …/checklist/suggestions` → `ChecklistService.suggestions` (no transaction): `getOwnedTrip` → existing keys → `ActivityRepository.findWithPlacesByTripId` → `WeatherService.forTrip(trip)` (Redis cache / Open-Meteo; failure tolerated) → `ChecklistSuggester.suggest(trip, activities, weather)` (rules with reasons; `skippedRules` when weather missing) → filter out existing → response. User ticks suggestions → `checklistApi.createItems(... source:"SUGGESTED", reason)` → step 2.
4. Toggle / rename: `checklistApi.updateItem(itemId, {done | title | category | dueDate})` → `PATCH /api/checklist-items/{id}` → `ChecklistService.update` → `ChecklistItemRepository.findByIdAndUserId` (404) → set fields → save.
5. Delete: `checklistApi.deleteItem` → `DELETE /api/checklist-items/{id}` → `ChecklistService.delete` → `findByIdAndUserId` → `delete`.
6. UI invalidates the checklist query after each mutation.

## F8. Community: browse, create post, upvote, comment

Participants: User → `CommunityPage` (`routes/_layout/community.tsx`), `PostCard`, `PostComments`, `CreatePostDialog` → `blogApi.*` → `BlogController` → `BlogService` → `BlogPostRepository`, `BlogCommentRepository`, `BlogPostUpvoteRepository`, `BlogCommentUpvoteRepository`, `UserRepository`, `TripRepository` → DB.

Browse:
1. `blogApi.getDestinations()` → `GET /api/blog/posts/destinations` (public) → `BlogPostRepository.findDistinctDestinations` → filter chips.
2. `useInfiniteQuery` → `blogApi.getPosts({destination, search, page, size})` → `GET /api/blog/posts` → `BlogController.getPosts` (`SecurityUtils.getOptionalUserId()`) → `BlogService.getPosts` → `BlogPostRepository.findFiltered(destination, search, PageRequest)` → if user: `BlogPostUpvoteRepository.findUpvotedPostIds(userId, ids)` → `BlogPostResponse.fromEntity(p, isUpvoted)` (author as `PublicUserResponse`) → `PageResponse`. Next page on scroll.
Create post:
3. "Write post" (alt **[no user]** toast) → `CreatePostDialog` (destination, title, content, optional own trip, photo URLs validated client-side) → `blogApi.createPost` → `POST /api/blog/posts` → `@Valid BlogPostRequest` (alt 400) → `BlogService.createPost`: `UserRepository.findById` → alt **[tripId]** `TripRepository.findByIdAndUserId` (alt 404 for another user's trip) → `BlogPostRepository.save` → 201 → UI invalidates `["blog"]` queries.
Upvote:
4. `PostCard` upvote → `blogApi.togglePostUpvote(id)` → `POST /api/blog/posts/{id}/upvote` → `BlogService.togglePostUpvote`: `existsById` (404) → `BlogPostUpvoteRepository.insertIfAbsent` (native `INSERT … ON CONFLICT DO NOTHING`) → alt **[0 rows: already upvoted]** `deleteVote` → `BlogPostRepository.syncUpvoteCount` (UPDATE from COUNT) → `countByPostId` → `{upvoted, upvoteCount}`. UI `updateCachedPosts` expects a `BlogPost` (INFERRED mismatch: response lacks `id`, so cache not updated until refetch).
Comment:
5. `PostComments` (expand) → `blogApi.getComments(postId)` → `GET /api/blog/posts/{id}/comments` → `BlogService.getComments` (404 if post missing; oldest first; `findUpvotedCommentIds`).
6. Add → `blogApi.addComment(postId, content)` → `POST …/comments` → `BlogService.addComment`: `BlogPostRepository.findById` (404) → `UserRepository.findById` → `BlogCommentRepository.save` → `BlogPostRepository.updateCommentCount(postId, +1)` → 201.
7. Comment upvote → `POST /api/blog/comments/{id}/upvote` → `BlogService.toggleCommentUpvote` (same toggle pattern).
Delete:
8. Author/admin → `ConfirmDialog` → `blogApi.deletePost` → `DELETE /api/blog/posts/{id}` → `BlogService.deletePost(userId, id, SecurityUtils.isAdmin())` → alt **[not author & not admin]** 403 → `BlogPostRepository.delete` (DB cascade comments/upvotes).
Admin extraction (alt, ADMIN only): `ExtractKnowledgeModal` → `blogApi.extractKnowledge` → `POST /api/admin/blog/posts/{id}/extract-knowledge` → `BlogService.extractKnowledgeFromPost` (snapshot in `TransactionTemplate` → LLM with `<untrusted_…>` wrapping → parse JSON or fallback) → admin edits → `blogApi.convertToKnowledge` → `POST …/convert-to-knowledge` → `DestinationKnowledgeService.create` + link `blog_posts.converted_to_knowledge_id`.

## F9. Duplicate trip (own trip, and copy from community post)

Participants: User → `TripDetailPage`/`TripHero` **or** `CommunityPage`/`PostCard` → `DuplicateTripModal` → `tripApi.duplicateTrip` → `TripController.duplicate` → `TripDuplicationService.duplicate` → `TripRepository`, `BlogPostRepository`, `UserRepository`, `ActivityRepository`, `ChecklistItemRepository`, `TripMapper` → DB.

1. Entry A (own trip): `TripHero.onDuplicate` → modal (default start = today+7) → `duplicateMutation` → `tripApi.duplicateTrip(tripId, {name:"<name> (Bản sao)", startDate})`.
   Entry B (community, uncommitted): `PostCard` "copy itinerary" (only posts with `tripId`; alt **[no user]** toast) → modal → `cloneMutation` → `tripApi.duplicateTrip(post.tripId, {startDate, name?})`.
2. `POST /api/trips/{tripId}/duplicate` → `@Valid DuplicateTripRequest` (alt 400 if start date in past) → `TripDuplicationService.duplicate(userId, tripId, req)` (`@Transactional`):
   2.1 `TripRepository.findByIdAndUserId(tripId, userId)` → alt **[owned]** source;
       alt **[not owned]** `BlogPostRepository.existsByTripId(tripId)` → true → `TripRepository.findById` → source; false → 404 `RESOURCE_NOT_FOUND`;
   2.2 `UserRepository.findById(userId)` → new owner;
   2.3 `shift = days(source.start → req.start)`; default name suffix "(bản sao)" or "(sao chép)" for community copies;
   2.4 build new `TripEntity` (copy destination, budget, currency, style, preferences, group profile, travel plan);
   2.5 for each source day → new day (date+shift, note) → `ActivityRepository.findByItineraryDayIdOrderByOrderIndexAsc(day.id)` → copy each activity (status PLANNED, createdBy USER) and record previously SKIPPED ones;
   2.6 `TripRepository.save(copy)` (cascade days/activities);
   2.7 `ChecklistItemRepository.findByTripIdOrderByKindAscOrderIndexAscIdAsc(source)` → save copies (done=false, dueDate+shift);
   2.8 map skipped activities to new ids → `TripMapper.toDetail(copy)`.
3. 201 `DuplicateTripResponse` → toast "copied N activities" → invalidate `["trips"]` → navigate to `/trips/{newId}`.
Not copied: expenses, conversations, proposals, ratings, alternatives.

## F10. Trip recap → review post (uncommitted feature)

Participants: User → `TripHero` (phase AFTER) → `TripDetailPage.recapMutation` → `blogApi.generateTripRecap` → `BlogController.generateTripRecap` → `BlogService.generateTripRecap` → `TripService.getOwnedTrip`, `ActivityRepository`, `ExpenseRepository`, `PlaceRatingRepository`, `LlmGateway`/LLM → then `CreatePostDialog` → F8 create post.

1. Button "AI Viết Review Tổng Kết" (visible when `trip.phase === "AFTER"`) → `recapMutation.mutate()` → `blogApi.generateTripRecap(tripId)` → `POST /api/blog/trips/{tripId}/recap`.
2. `BlogService.generateTripRecap(userId, tripId)` (`@Transactional(readOnly=true)`):
   2.1 `TripService.getOwnedTrip` (alt 404);
   2.2 `ActivityRepository.findWithPlacesByTripId` → drop SKIPPED → distinct place names;
   2.3 `ExpenseRepository.findByTripIdOrderByExpenseDateDescCreatedAtDesc` → total spent;
   2.4 for each activity with place: `PlaceRatingRepository.findByUserIdAndPlaceId` → ratings summary;
   2.5 build Vietnamese prompt (trip name, destination, dates, travellers, places, spend, ratings) asking for JSON `{title, content}`;
   2.6 alt **[LLM configured]** `LlmGateway.call(Prompt(system "travel blogger", user))` → LLM → extract `{…}` → `ObjectMapper.readValue` → title/content; exceptions logged and ignored;
   2.7 alt **[no title/content]** → template title "Kinh nghiệm du lịch <dest>: …" and template Markdown (places, cost summary, ratings, tips);
   2.8 return `TripRecapResponse{title, destinationName, content, tripId, totalSpent, currency, placesVisited, photoUrls:[]}`.
3. 200 → `setRecapData`, open `CreatePostDialog(initialData)` (pre-filled destination/title/content/tripId) → user edits → publish = F8 step 3 (post linked to trip, which then makes the trip copyable by others via F9 entry B).
4. alt **[HTTP error]** → toast "Không thể tạo bài review tổng kết". No AI quota check on this endpoint.

## F11. Places search & save

Participants: User → `PlacesPage` (`routes/_layout/places.tsx`) → `placeApi.search` / `placeApi.getSavedPlaces` / `placeApi.savePlace` / `placeApi.unsavePlace` → `PlaceController.searchPlaces` / `SavedPlaceController` → `PlaceServiceImpl` / `SavedPlaceServiceImpl` → `GooglePlacesClient` (Google), `PlaceCandidateCache` (Redis), `PlaceRepository`, `SavedPlaceRepository`, `UserRepository` → DB; `TripMap` (Leaflet).

1. Page load / search submit → `useQuery(["places","search",q,dest])` → `placeApi.search(query, destination)` → `GET /api/places/search?query&destination` (auth required).
2. `PlaceServiceImpl.searchPlaces(query, destination)`:
   2.1 `GooglePlacesClient.searchText(query + " " + destination, null)` → Google Places Text Search (field mask, `vi`, ≤10);
   2.2 `PlaceCandidateCache.putAll("GOOGLE", results)` (Redis, ~1 h) → map each to `PlaceResponse` (provider, externalId, coords, rating, priceLevel…; **no DB id**);
   2.3 alt **[Google error or empty]** → `PlaceRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrAddressContainingIgnoreCase(q,q,q)` → `PlaceResponse.fromEntity` (with id).
3. UI renders cards + `TripMap` markers (`exploreMapPoints`), "open in Google Maps" link.
4. Saved list: `placeApi.getSavedPlaces()` → `GET /api/me/saved-places` → `SavedPlaceServiceImpl.getSavedPlaces` → saved tab + map.
5. Toggle save: `toggleSave(place)`:
   - alt **[already saved]** → `placeApi.unsavePlace(place.id)` → `DELETE /api/places/{id}/save` → `SavedPlaceServiceImpl.unsavePlace`.
   - else → `placeApi.savePlace(place.id)` → `POST /api/places/{id}/save` → `SavedPlaceServiceImpl.savePlace`: `UserRepository.findById`, `PlaceRepository.findById` (404) → `SavedPlaceRepository.save` (UQ user+place).
   - INFERRED defect: for Google results `place.id` is undefined → request `/api/places/undefined/save` → 400 `VALIDATION_ERROR` (type mismatch). Intended endpoint `POST /api/me/saved-places {provider, externalId}` → `SavedPlaceServiceImpl.saveExternalPlace` → `PlaceAdoptionService.resolve(null, provider, externalId, SAVED)` → `savePlace` exists but is not wired in the UI.
6. On success: invalidate `["saved-places"]`, toast.

## F12. Budget & expense (supporting flow)

Participants: `BudgetView`/`ExpenseForm` → `budgetApi.*` → `BudgetController` → `BudgetServiceImpl` / `ExpenseServiceImpl` → `ActivityRepository.sumEstimatedByType`, `ExpenseRepository.sumByCategory`, `TripClock` → DB.

1. `budgetApi.getBudgetSummary` → `GET /api/trips/{id}/budget` → `BudgetServiceImpl.getTripBudgetSummary` → `summarize(trip)` (estimated vs actual per category, remaining, warningLevel, daily allowance by phase).
2. `ExpenseForm` → `budgetApi.createExpense` → `POST /api/trips/{id}/expenses` → `ExpenseServiceImpl.create` → owned trip → `apply`: currency ≠ trip currency → 422 `CURRENCY_MISMATCH`; activityId must be in trip (404) → `ExpenseRepository.save` → UI invalidates budget & expenses.
3. Delete → `DELETE /api/expenses/{id}` → `ExpenseServiceImpl.delete` (owner check 404).

## F13. Weather (supporting flow)
`useTripWeather` → `weatherApi.getTripWeather` → `GET /api/trips/{id}/weather` → `WeatherController.getWeather` → `WeatherService.getTripWeather` → `getOwnedTrip` → `forTrip`: Redis `readCache(weather:…)` → miss → split days into archive / forecast / climate groups → `OpenMeteoClient.archive` / `forecast` (each group `safely`, failure → missing days + reason) → `writeCache` (3 h) → `TripWeatherResponse`.

---

## Activity-diagram hints (decision points)
- Login: limiter check → credentials valid? → account active? → issue token.
- Create trip: dates valid? → length ≤30? → destination resolvable? → defaults exist? → save.
- AI generation: owned? → AI configured? → itinerary empty? → job already running? → quota? → enqueue → [worker] pool → model plan → verify → any valid? → save proposal → apply (trip still empty?) → alternatives → DONE / FAILED.
- Chat turn: prechecks → loop {call model → tool calls? → execute tools} until answer / 8 rounds / 60 s → wrap-up → stream → save.
- Apply proposal: owned? → PENDING? → not expired? → reorders still valid? → apply ops (skip vanished targets) → APPLIED.
- Undo: APPLIED? → within 10 min? → not blocked by later proposal? → per op fingerprint unchanged? → revert / keep.
- Duplicate: owned? else post-linked? → copy days/activities/checklist.
- Recap: owned? → LLM configured & valid JSON? → AI text : template.
