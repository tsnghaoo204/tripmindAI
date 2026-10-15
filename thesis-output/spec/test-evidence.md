# Test Evidence — TripMind (internal evidence)

All numbers below come from commands actually executed on 2026-10-15 in this working tree (including uncommitted changes) or from counting files. Nothing is estimated. Tags: VERIFIED / PARTIAL / INFERRED / UNVERIFIED.

## 1. What was executed

| Command (cwd) | Result | Tag |
|---|---|---|
| `mvn -o -B test` (`backend/`, offline, local Docker available for Testcontainers) | **BUILD SUCCESS, exit 0, 42.8 s. Tests run: 116, Failures: 0, Errors: 0, Skipped: 0** across 32 test classes. Log kept at `/tmp/claude-1000/-mnt-hdd2tb-haots-repos-tripmindAI/4aee73cd-b86d-47bd-b9c3-6becd837673f/scratchpad/mvn-test.log` (session scratch, not in repo). | VERIFIED |
| `npx vitest run` (`frontend/`) | **4 test files, 18 tests, all passed** (Vitest 3.2.7, 1.04 s). | VERIFIED |
| `npx tsc --noEmit -p .` (`frontend/`) | exit 0 (no type errors). | VERIFIED |
| Biome lint (`npm run lint:ci`), `npm run build`, `./mvnw verify` | **Not run** in this session. | — |
| GitHub Actions run history | **Not checked** (no `gh` CLI available; remote `github.com:tsnghaoo204/tripmindAI`). Whether CI is green on GitHub is UNVERIFIED. | UNVERIFIED |

Note: 114 `@Test` annotations + 1 `@ParameterizedTest` (`BudgetServiceImplTest`, 3 cases) → 116 executed tests (VERIFIED by grep + surefire summary).

## 2. Test infrastructure

| Item | Detail | Evidence |
|---|---|---|
| Frameworks | JUnit 5, Mockito, AssertJ (spring-boot-starter-test), spring-security-test, Testcontainers 1.21.4 | `backend/pom.xml` |
| Integration base | `support/IntegrationTest`: `@SpringBootTest` + `@AutoConfigureMockMvc` + profile `test`; real **PostgreSQL 16** and **Redis 7.4-alpine** containers started once (`@ServiceConnection`); schema loaded from `classpath:db/schema.sql` (`spring.sql.init.mode=always`); LLM replaced by scripted `support/FakeLlmGateway` (`@Primary`); Google Places in mock mode; Open-Meteo pointed at an unreachable address (tests the failure path); AI rate limit lowered to 5/h. Helpers: `registerUser`, `registerAdmin` (promote via SQL), `createTrip`, `call(...)`. | `IntegrationTest.java`, `application-test.yml` |
| Unit tests | Mockito-based service tests and pure-logic tests (no Spring context). | `services/*Test.java` |
| Frontend | Vitest, node environment, `src/**/*.test.ts` (pure TS logic; no React component/DOM tests, no E2E). | `vite.config.ts` |
| CI | `.github/workflows/ci.yml`: backend `./mvnw -B verify` (Java 21), frontend Node 20 & 22: `npm ci`, typecheck, `lint:ci`, `npm test`, `build`. | VERIFIED file |
| Manual/ad-hoc | `scratch/test_all_endpoints.py`, `scratch/test_full_dom_flows.js` — not wired into CI; not executed here. | VERIFIED presence only |

## 3. Backend test classes and coverage (tests executed per class, from surefire)

### 3.1 API integration tests (`backend/src/test/java/com/tripmind/api/`, Testcontainers)
| Class | Tests | What it verifies |
|---|---|---|
| `TripApiTest` | 6 | create trip stores preferences + group profile; shifting dates doesn't violate UNIQUE(trip_id,date); >30 days → 422 TRIP_TOO_LONG; shrinking with activities needs `confirmDropDays`; other user's trip → 404 (BR-104); manual phase + status filter; currency change blocked by recorded amounts |
| `ItineraryApiTest` | 4 | ownership 404, reorder with duplicate ids → 422; adding activity with Google id adopts place with `adopted_via=ITINERARY`; end<start rejected, null cost stays null; alternatives get/select (3 real places for FOOD) |
| `TripExtrasApiTest` | 3 | checklist suggestions (weather failure reported as skipped), add, dedupe, tick, delete; ratings only after trip end and disliked place filtered from `search_places`; .ics export + duplicate (keeps activities/checklist, drops expenses, reports previously skipped) |
| `BudgetApiTest` | 1 | expense CRUD, SKIPPED excluded from estimate, wrong currency rejected |
| `CrossUserAccessApiTest` | 5 | user B cannot read/modify user A's activities, days, alternatives, expenses, checklist, ratings, saved places, trips (export, duplicate, update, phase, delete, budget, weather), proposals and conversations — all 404; assistant in B's trip cannot target A's activity |
| `AiAssistantApiTest` | 5 | SSE turn with 2 tool calls emits tool_start/tool_end/token/done and logs 2 OK executions; tools ignore tripId supplied by model (BR-103); loop stops at 8 rounds (BR-502); duplicate identical tool calls executed once (BR-503); prechecks return JSON 404/503/429 before stream |
| `ToolAuditApiTest` | 2 | failing tool still audited with ERROR and turn completes; LLM failure mid-turn → SSE `error` event, executed tools still logged |
| `ProposalApiTest` | 11 | propose → view → apply → second apply 409 (no duplication, BR-508); invented place id rejected (BR-509); apply checks wrong trip/user/rejected/expired (BR-506/507); undo keeps manual edits & restores deleted, preview accurate; undo after 10 min → UNDO_WINDOW_CLOSED; reorder via optimize_day_order and day changed after proposal → PROPOSAL_STALE; relaxed-LIFO undo blocked by later proposal, expired proposals cleaned; unavailable place op skipped (PLACE_UNAVAILABLE) rest applied; invalid UPDATE changes nothing; two ops on same activity → DUPLICATE_TARGET; activity with linked expense not deleted even on undo |
| `GenerationApiTest` | 2 | generation keeps real places, drops invented ones and counts them; non-empty trip → 409; lenient partial plan parsing, duplicate day merge, repeated start returns same job, food alternatives generated |
| `AdminApiTest` | 2 | admin reads tool log/AI usage/lists, USER gets 403; global rate limit 120/min → 429 with retryAfterSeconds |
| `AdminKnowledgeApiTest` | 1 | admin knowledge CRUD, filter, toggle and injection into AI prompt |
| `KnowledgePromptApiTest` | 1 | inactive knowledge excluded from prompt, re-enabled included |
| `SecurityHardeningApiTest` | 13 | public blog GET hides email/role, anonymous POST 401; delete post: other user 403, admin OK; USER calling extract-knowledge 403; post cannot link another user's trip, photos must be https, ≤10; upvote toggle count consistent; parse-prompt anonymous 401, >1000 chars/empty 400; places & destinations require login; 5 wrong logins → 429 even with correct password; registration password ≥8; password change revokes old token and returns new one, admin deactivation revokes tokens; `?token=` query auth rejected; client `destinationData` cannot overwrite seeded rows, bad coords/timezone 400; body >1 MB → 413 and JSON errors leak no internals; CORS only for allowed origins |
| `support/MigrationSmokeTest` | 1 | schema loads on empty DB under `ddl-auto: validate`; ≥15 seeded destinations |

### 3.2 Unit / component tests
| Class | Tests | What it verifies |
|---|---|---|
| `services/AuthServiceTest` | 4 | register success, duplicate email → CONFLICT, login success, wrong password → UNAUTHORIZED |
| `services/UserServiceTest` | 4 | get/update profile, change password success, wrong current password → VALIDATION_ERROR |
| `configurations/security/JwtTokenProviderTest` | 2 | generate+validate token; invalid token returns false |
| `services/ItineraryServiceTest` | 8 | itinerary with distances, add activity orderIndex, other-user 404, reorder duplicate ids 422, end<start, SKIPPED requires reason, reorder, delete + reindex |
| `services/BudgetServiceImplTest` | 9 | warning thresholds 89/91/101 % (parameterized), remaining uses max(estimate, actual), daily allowance during trip, over-budget, plan mode before trip, no allowance after trip/no budget, "today" in destination timezone |
| `services/WeatherServiceTest` | 6 | forecast/climate boundary (day 16 vs 17), forecast down → PARTIAL, all down → empty + WEATHER_UNAVAILABLE, past days OBSERVED, Feb 29 climate normal, one bad archive year skipped |
| `services/ChecklistSuggesterTest` | 2 | rain + beach → raincoat & swimwear with reasons; abroad + kids + flight → passport, kid items, online check-in TODO |
| `services/CostEstimatorTest` | 4 | $$ food for 2 → 200k–600k suggested 400k; rounding; no price level → no guess; unsupported currency/type |
| `services/DistanceServiceTest` | 3 | Haversine distance (Dragon Bridge ↔ My Khe), travel time by mode, ideal timing tip |
| `services/ItineraryOptimizerTest` | 2 | zig-zag route shortened with start fixed; timed/no-coordinate anchors stay |
| `services/IcsExporterTest` / `IcsEscapeTest` | 2 / 1 | local→UTC conversion, all-day grouping, stable UID, 75-byte folding without splitting UTF-8; CR injection prevented |
| `services/SavedPlaceServiceTest` | 3 | save, unsave, list |
| `services/TripClockTest` | 3 | today in destination timezone (Tokyo), manual DURING clamps, invalid timezone → UTC |
| `services/ai/TripPromptParserServiceTest` | 2 | accent-insensitive exact destination match, injected clock dates; days clamped to [1, maxDays] |
| `services/ai/GroupAwareToolsTest` | 2 | vegetarian keyword added to food search; group summary in system prompt |
| `services/ai/NameMatchTest` | 1 | accent/case-insensitive name matching, invented names fail |
| `schema/EnumCheckConstraintTest` | 1 | every Java enum mapped to a column matches the DB CHECK list in schema.sql |

### 3.3 Frontend tests (`frontend/src`)
| File | Tests | Covers |
|---|---|---|
| `client/errors.test.ts` | 4 | `apiErrorFromBody` keeps message/code/status, rate-limit message with retry delay, status fallback; `skipReasonLabel` |
| `client/sse.test.ts` | 5 | SSE parser: named JSON events, multi-line data, chunk splits + CRLF, comments/default event name, trailing event flush |
| `components/pages/ai/chat-state.test.ts` | 4 | proposal event mapping, early proposal & dedupe, status update prevents re-apply, empty assistant bubble handling |
| `client/api/ai-api.test.ts` | 5 | `streamChat`: token payload, single onDone, finish without done event, single error, backend message on non-2xx |

## 4. Coverage gaps (VERIFIED by absence of tests referencing these paths)

| Area | Gap |
|---|---|
| Uncommitted features | `BlogService.generateTripRecap` / `POST /api/blog/trips/{id}/recap`, community trip copy branch in `TripDuplicationService` (non-owner + `existsByTripId`), `TripMap.tsx`, new `AddActivityModal`, mobile header — **no tests** |
| Blog | comments listing/adding only indirectly (`SecurityHardeningApiTest`); no test for feed filtering/search paging |
| Places | no test of `PlaceServiceImpl.searchPlaces` with Google enabled (tests use mock mode); Explore-page save path for Google results untested (suspected bug, see feature-catalog P2) |
| Destinations | `DestinationService` Google/manual resolution only indirectly |
| Admin user/trip write endpoints | create/delete user, update/delete trip — no dedicated test (`AdminApiTest` covers read endpoints, 403 and rate limit; only deactivation via admin update is exercised in `SecurityHardeningApiTest`) — PARTIAL |
| LLM real provider | all AI tests use `FakeLlmGateway`; no test against a real model (by design) |
| Frontend | no component, routing, or E2E tests; coverage tool not configured (no coverage numbers exist — do not cite any) |
| Coverage metrics | JaCoCo/Istanbul not configured → **no line/branch coverage figures available** |
| Non-functional | no load/performance/security scanning tests in repo |

## 5. Suggested wording for the thesis (factual)
"The backend has 116 automated tests (32 test classes) combining Mockito unit tests and Testcontainers-based integration tests against real PostgreSQL 16 and Redis 7.4 with a scripted fake LLM; all pass (`mvn test`, 2026-10-15). The frontend has 18 Vitest unit tests for the API error mapper, SSE parser, chat streaming and proposal state; all pass, and the TypeScript type check is clean. A GitHub Actions workflow runs both suites on every push and pull request." (Do not claim coverage percentages or CI pass status — not measured.)
