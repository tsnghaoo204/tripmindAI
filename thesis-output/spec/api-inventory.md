# REST API Inventory — TripMind backend (internal evidence)

All entries VERIFIED by reading `backend/src/main/java/com/tripmind/controllers/*.java` and `configurations/security/SecurityConfig.java` (current working tree, including uncommitted `POST /api/blog/trips/{tripId}/recap`).

Conventions
- Base URL: backend port 8088 (`SERVER_PORT`); frontend dev server proxies `/api` (`frontend/vite.config.ts`).
- Envelope: every JSON response is `ApiResponse<T>{success, code?, message, data?, details?, timestamp}` (`domains/responses/ApiResponse.java`). "Response" column describes `data`.
- Auth column: **Public** = `permitAll`; **User** = any authenticated JWT (`Authorization: Bearer <jwt>`); **Admin** = `ROLE_ADMIN` (`/api/admin/**`). "Owner" = service additionally checks the resource belongs to the caller and answers **404** otherwise.
- All `/api/**` requests pass `ApiRateLimitFilter` (120/min per user or IP) and `RequestSizeLimitFilter` (1 MB → 413).
- AI-quota (20/hour/user, `RateLimiter.check("ai", …)`) applies where marked **[AI-quota]**.
- Page = `PageResponse{content[], page, size, totalElements}` (record in `domains/responses/PageResponse.java`).
- Total: **92 endpoints in 19 controllers** + actuator/Swagger.

## 1. AuthController — `/api/auth`
| Method | Path | Auth | Request | Response | Controller method |
|---|---|---|---|---|---|
| POST | /api/auth/register | Public | `RegisterRequest{email, password(8–72), name}` | 201 `AuthResponse{accessToken, tokenType, expiresInMs, user}` | `AuthController.register` |
| POST | /api/auth/login | Public (login lockout) | `LoginRequest{email, password}` | `AuthResponse` | `AuthController.login` |
| GET | /api/auth/me | User | — | `UserResponse` | `AuthController.getCurrentUser` |
| GET | /api/auth/health | Public | — | string | `AuthController.health` |

## 2. UserController — `/api/users`
| Method | Path | Auth | Request | Response | Method |
|---|---|---|---|---|---|
| GET | /api/users/profile | User | — | `UserResponse{id,email,name,avatarUrl,role,isActive,createdAt…}` | `getProfile` |
| PUT | /api/users/profile | User | `UpdateProfileRequest{name?, avatarUrl? (https or "")}` | `UserResponse` | `updateProfile` |
| PUT | /api/users/change-password | User | `ChangePasswordRequest{currentPassword, newPassword(8–72)}` | new `AuthResponse` (old tokens revoked) | `changePassword` |

## 3. PreferenceController — `/api/me/preferences`
| GET | /api/me/preferences | User | — | `UserPreferencesResponse{travelStyle, budgetPreference, preferences[]}` | `PreferenceController.get` |
|---|---|---|---|---|---|
| PUT | /api/me/preferences | User | `UserPreferencesRequest` | `UserPreferencesResponse` | `update` |

## 4. TripController — `/api/trips`
| Method | Path | Auth | Request | Response | Method |
|---|---|---|---|---|---|
| POST | /api/trips/parse-prompt | User [AI-quota] | `ParsePromptRequest{prompt ≤1000}` | `Map` of parsed fields (title, destinationName, destinationId, startDate, endDate, travelers, budget, travelStyle, group & travel-plan hints…) | `parsePrompt` |
| POST | /api/trips | User | `CreateTripRequest{destinationId \| destinationPlaceId \| destinationData, name, startDate, endDate, travelers, budget, currency, travelStyle, budgetPreference, preferences[], groupProfile, travelPlan}` | 201 `TripResponse` (detail with day summaries) | `createTrip` |
| GET | /api/trips?status=upcoming\|ongoing\|past | User | — | `TripResponse[]` (summaries) | `getMyTrips` |
| GET | /api/trips/{tripId} | User, Owner | — | `TripResponse` | `getTripDetails` |
| PUT | /api/trips/{tripId} | User, Owner | `UpdateTripRequest` (all optional + `confirmDropDays`) | `TripResponse` | `updateTrip` |
| PUT | /api/trips/{tripId}/phase | User, Owner | `UpdateTripPhaseRequest{phase: BEFORE\|DURING\|AFTER\|null}` | `TripResponse` | `updatePhase` |
| POST | /api/trips/{tripId}/duplicate | User, Owner **or** trip linked to a blog post (uncommitted) | `DuplicateTripRequest{startDate, name?}` | 201 `DuplicateTripResponse{trip, activitiesCopied, checklistItemsCopied, previouslySkipped[]}` | `duplicate` |
| DELETE | /api/trips/{tripId} | User, Owner | — | null | `deleteTrip` |

## 5. ItineraryController
| Method | Path | Auth | Request | Response | Method |
|---|---|---|---|---|---|
| GET | /api/trips/{tripId}/itinerary | User, Owner | — | `ItineraryResponse{tripId, days[ItineraryDayResponse{activities[ActivityResponse…], distances}], totals}` | `getTripItinerary` |
| POST | /api/trips/{tripId}/itinerary/activities | User, Owner | `CreateActivityRequest` | 201 `ActivityResponse` | `addActivity` |
| PUT | /api/activities/{activityId} | User, Owner | `UpdateActivityRequest` (incl. status, skipReason, actualStart/End) | `ActivityResponse` | `updateActivity` |
| DELETE | /api/activities/{activityId} | User, Owner | — | null | `deleteActivity` |
| GET | /api/trips/{tripId}/cost-estimate?placeId&priceLevel&activityType | User, Owner | — | `CostEstimateResponse{suggested, min, max, basis, reason, currency}` | `estimateCost` |
| PUT | /api/itinerary-days/{dayId}/reorder | User, Owner | `ReorderActivitiesRequest{activityIds[1..200]}` | `ItineraryDayResponse` | `reorderActivities` |
| GET | /api/activities/{activityId}/alternatives | User, Owner | — | `ActivityAlternativeResponse[]` (≤3 tiers) | `getAlternatives` |
| PUT | /api/activities/{activityId}/alternatives/{altId}/select | User, Owner | — | `ActivityResponse` | `selectAlternative` |

## 6. ChecklistController
| GET | /api/trips/{tripId}/checklist?kind=PACK\|TODO | User, Owner | — | `ChecklistItemResponse[]` | `ChecklistController.list` |
|---|---|---|---|---|---|
| POST | /api/trips/{tripId}/checklist | User, Owner | `ChecklistItemRequest[]` (1–50) | 201 created items (duplicates skipped) | `add` |
| GET | /api/trips/{tripId}/checklist/suggestions | User, Owner | — | `ChecklistSuggestionsResponse{items[], skippedRules[]…}` | `suggestions` |
| PATCH | /api/checklist-items/{itemId} | User, Owner | `UpdateChecklistItemRequest{title?, done?, category?, dueDate?}` | `ChecklistItemResponse` | `update` |
| DELETE | /api/checklist-items/{itemId} | User, Owner | — | null | `delete` |

## 7. BudgetController
| GET | /api/trips/{tripId}/budget | User, Owner | — | `BudgetSummaryResponse{budget, estimatedTotal, actualTotal, remaining, usedRatio, warningLevel, byCategory, daily…}` | `BudgetController.getBudget` |
|---|---|---|---|---|---|
| GET | /api/trips/{tripId}/expenses?category&from&to | User, Owner | — | `ExpenseResponse[]` newest first | `listExpenses` |
| POST | /api/trips/{tripId}/expenses | User, Owner | `ExpenseRequest{category, amount, currency?, description?, expenseDate, activityId?}` | 201 `ExpenseResponse` | `createExpense` |
| PUT | /api/expenses/{expenseId} | User, Owner | `ExpenseRequest` | `ExpenseResponse` | `updateExpense` |
| DELETE | /api/expenses/{expenseId} | User, Owner | — | null | `deleteExpense` |

## 8. WeatherController / ExportController
| GET | /api/trips/{tripId}/weather | User, Owner | — | `TripWeatherResponse{reason?, days[{dayNumber, date, source, summary, tempMin, tempMax, precipitationProbability…}]}` | `WeatherController.getWeather` |
|---|---|---|---|---|---|
| GET | /api/trips/{tripId}/export.ics | User, Owner | — | `text/calendar` file (not JSON envelope) | `ExportController.exportIcs` |

## 9. AiController (SSE chat & history)
| Method | Path | Auth | Request | Response | Method |
|---|---|---|---|---|---|
| POST | /api/trips/{tripId}/ai/chat | User, Owner [AI-quota] | `AiChatRequest{message ≤4000, conversationId?}` | `text/event-stream`: `tool_start{tool,label}`, `tool_end{tool,ms,status}`, `proposal{proposalId, summary,…}`, `places{candidates}`, `checklist{items}`, `token{text}`, `done{messageId, conversationId}`, `error{code,message}` | `AiController.chat` |
| GET | /api/trips/{tripId}/ai/conversations | User, Owner | — | `ConversationResponse[]` | `conversations` |
| GET | /api/conversations/{conversationId}/messages | User, Owner | — | `MessageResponse[]` (with attachments) | `messages` |
| GET | /api/trips/{tripId}/ai/activity?limit(1–200) | User, Owner | — | `ToolExecutionResponse[]` | `activity` |

## 10. ProposalController (proposals, undo, explanation, generation)
| Method | Path | Auth | Request | Response | Method |
|---|---|---|---|---|---|
| GET | /api/proposals/{proposalId} | User, Owner | — | `ProposalResponse{id, status, summary, changes[], costDelta, travelTimeDelta, expiresAt…}` | `get` |
| POST | /api/trips/{tripId}/ai/apply | User, Owner | `ApplyProposalRequest{proposalId}` | `ApplyProposalResponse{applied, skipped[], undoAvailableUntil…}` | `apply` |
| POST | /api/proposals/{proposalId}/reject | User, Owner | — | `ProposalResponse` | `reject` |
| GET | /api/trips/{tripId}/ai/undo/{proposalId}/preview | User, Owner | — | `UndoProposalResponse` (dry run) | `undoPreview` |
| POST | /api/trips/{tripId}/ai/undo | User, Owner | `ApplyProposalRequest{proposalId}` | `UndoProposalResponse` (reverted, undoSkipped…) | `undo` |
| GET | /api/activities/{activityId}/explanation | User, Owner | — | `ExplanationResponse` | `explain` |
| POST | /api/trips/{tripId}/ai/generate | User, Owner [AI-quota] | — | 202 job view `{jobId, state, step, progress, result?, error?}` | `generate` |
| GET | /api/trips/{tripId}/ai/generate/{jobId} | User, Owner | — | job view | `generationStatus` |
| GET | /api/trips/{tripId}/ai/generate/{jobId}/stream | User, Owner | — | `text/event-stream` events `progress` / `done` / `error` with job view | `streamGenerationStatus` |

## 11. DestinationController — `/api/destinations`
| GET | /api/destinations?query= | User | — | `DestinationResponse[]` (DB + Google) | `getDestinations` |
|---|---|---|---|---|---|
| GET | /api/destinations/popular | User | — | `DestinationResponse[]` | `getPopularDestinations` |
| GET | /api/destinations/{destinationId} | User | — | `DestinationResponse` | `getDestinationById` |

## 12. PlaceController / SavedPlaceController / PlaceRatingController
| Method | Path | Auth | Request | Response | Method |
|---|---|---|---|---|---|
| GET | /api/places/search?query&destination | User | — | `PlaceResponse[]` (Google results have no DB id) | `PlaceController.searchPlaces` |
| GET | /api/places/recommendations/nearby?lat&lng&radius&category | User | — | `PlaceResponse[]` | `getNearbyRecommendations` |
| GET | /api/places/{provider}/{externalId} | User | — | `PlaceResponse` | `getPlaceDetails` |
| POST | /api/places/{placeId}/save | User | — | 201 `SavedPlaceResponse` | `SavedPlaceController.savePlace` |
| POST | /api/me/saved-places | User | `SaveExternalPlaceRequest{provider="GOOGLE", externalId}` | 201 `SavedPlaceResponse` | `saveExternalPlace` |
| DELETE | /api/places/{placeId}/save | User | — | null | `unsavePlace` |
| GET | /api/me/saved-places | User | — | `SavedPlaceResponse[]` | `getMySavedPlaces` |
| GET | /api/trips/{tripId}/review/places | User, Owner | — | `PlaceRatingResponse[]` | `PlaceRatingController.reviewPlaces` |
| PUT | /api/trips/{tripId}/places/{placeId}/rating | User, Owner, trip ended | `PlaceRatingRequest{verdict LIKE\|DISLIKE, note?}` | `PlaceRatingResponse` | `rate` |
| DELETE | /api/trips/{tripId}/places/{placeId}/rating | User, Owner | — | null | `delete` |
| GET | /api/me/place-ratings | User | — | `PlaceRatingResponse[]` | `myRatings` |

## 13. BlogController — `/api/blog`
| Method | Path | Auth | Request | Response | Method |
|---|---|---|---|---|---|
| GET | /api/blog/posts?destination&search&page&size(1–100) | Public (user optional for `isUpvoted`) | — | Page<`BlogPostResponse{id, user(PublicUser), tripId, destinationName, title, content, photoUrls, upvoteCount, commentCount, isUpvoted, isConvertedToKnowledge…}`> | `getPosts` |
| GET | /api/blog/posts/destinations | Public | — | `String[]` | `getDestinations` |
| GET | /api/blog/posts/{id} | Public | — | `BlogPostResponse` | `getPostDetail` |
| POST | /api/blog/posts | User | `BlogPostRequest{destinationName, title ≤200, content ≤20000, tripId? (own), photoUrls ≤10 https}` | 201 `BlogPostResponse` | `createPost` |
| POST | /api/blog/trips/{tripId}/recap | User, Owner (uncommitted; no AI-quota) | — | `TripRecapResponse{title, destinationName, content(Markdown), tripId, totalSpent, currency, placesVisited, photoUrls}` | `generateTripRecap` |
| DELETE | /api/blog/posts/{id} | User (author or ADMIN, else 403) | — | null | `deletePost` |
| POST | /api/blog/posts/{id}/upvote | User | — | `{upvoted, upvoteCount}` | `togglePostUpvote` |
| GET | /api/blog/posts/{id}/comments | Public | — | `BlogCommentResponse[]` | `getComments` |
| POST | /api/blog/posts/{id}/comments | User | `BlogCommentRequest{content ≤2000}` | 201 `BlogCommentResponse` | `addComment` |
| POST | /api/blog/comments/{commentId}/upvote | User | — | `{upvoted, upvoteCount}` | `toggleCommentUpvote` |

## 14. AdminController — `/api/admin` (Admin)
| GET | /api/admin/tool-executions?userId&tool&status&from&to&page&size | Admin | — | Page<`ToolExecutionResponse`> | `toolExecutions` |
|---|---|---|---|---|---|
| GET | /api/admin/ai-usage?from&to | Admin | — | `Map` (turns, tokens, toolCalls, errorRate, topTools, perDay) | `aiUsage` |
| GET | /api/admin/users?page&size | Admin | — | Page<Map{id,email,name,role,active,createdAt,tripCount}> (JdbcTemplate SQL) | `users` |
| GET | /api/admin/trips?page&size | Admin | — | Page<Map{id,name,userId,destinationName,country,startDate,endDate,travelers,currency,phase,activityCount,conversationCount,createdAt}> (JdbcTemplate SQL) | `trips` |
| POST | /api/admin/users | Admin | `AdminCreateUserRequest{email, password, name, role}` | Map (status 200, message "created") | `createUser` |
| PUT | /api/admin/users/{userId} | Admin | `AdminUpdateUserRequest{name?, role?, active?, password?}` | Map | `updateUser` |
| DELETE | /api/admin/users/{userId} | Admin (not self) | — | null | `deleteUser` |
| PUT | /api/admin/trips/{tripId} | Admin | raw `Map{name?, phase?, budget?}` | Map | `updateTrip` |
| DELETE | /api/admin/trips/{tripId} | Admin | — | null | `deleteTrip` |

## 15. AdminKnowledgeController — `/api/admin/knowledge` (Admin)
| GET | /api/admin/knowledge?destination&category&search&page&size | Admin | — | Page<`KnowledgeResponse`> | `listKnowledge` |
|---|---|---|---|---|---|
| GET | /api/admin/knowledge/{id} | Admin | — | `KnowledgeResponse` | `getKnowledge` |
| GET | /api/admin/knowledge/destinations | Admin | — | `String[]` | `getDestinations` |
| POST | /api/admin/knowledge | Admin | `KnowledgeRequest{destinationId?, destinationName, category, title, content, placeName?, sourceName?, sourceUrl?, tags[], priority 1–3, active}` | 201 `KnowledgeResponse` | `createKnowledge` |
| PUT | /api/admin/knowledge/{id} | Admin | `KnowledgeRequest` | `KnowledgeResponse` | `updateKnowledge` |
| DELETE | /api/admin/knowledge/{id} | Admin | — | null | `deleteKnowledge` |
| PATCH | /api/admin/knowledge/{id}/toggle | Admin | — | `KnowledgeResponse` | `toggleActive` |

## 16. AdminBlogController — `/api/admin/blog` (Admin)
| POST | /api/admin/blog/posts/{id}/extract-knowledge | Admin | — | `ExtractedKnowledgeResponse{destinationName, category, categoryLabel, title, content, placeName, tags, priority, reasoning, sourceName, sourceSnippet}` | `extractKnowledge` |
|---|---|---|---|---|---|
| POST | /api/admin/blog/posts/{id}/convert-to-knowledge | Admin | `KnowledgeRequest` | 201 `KnowledgeResponse` | `convertToKnowledge` |

## 17. Infrastructure endpoints
| GET | /actuator/health | Public | — | Spring health (details never) | Actuator |
|---|---|---|---|---|---|
| GET | /swagger-ui.html, /api-docs | Public (toggle `SWAGGER_ENABLED`) | — | OpenAPI UI/JSON | springdoc |

## 18. Frontend usage cross-check
API functions defined in `frontend/src/client/api/*.ts` but **never called by a component** (VERIFIED by grep): `authApi.getProfile`, `authApi.getPreferences/updatePreferences`, `tripApi.updateTrip`, `itineraryApi.updateActivity`, `itineraryApi.reorderActivities`, `budgetApi.updateExpense`, `aiApi.getProposal`, `aiApi.previewUndo`, `placeApi.saveByExternalId`, `blogApi.getPostDetail`. Backend endpoints with no frontend client at all: `GET /api/auth/me`, `GET /api/destinations/popular`, `GET /api/destinations/{id}`, `GET /api/places/recommendations/nearby`, `GET /api/places/{provider}/{externalId}`, `DELETE …/rating`, `GET /api/me/place-ratings`, `GET /api/trips/{id}/ai/activity`, `GET /api/admin/knowledge/{id}`.

Contract mismatch (INFERRED): `blogApi.togglePostUpvote` / `toggleCommentUpvote` are typed as returning `BlogPost` / `BlogComment`, backend returns `{upvoted, upvoteCount}`.

Design-doc endpoints missing (PROPOSED-ONLY): `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/places`, `GET /api/places/{id}`.
