# Data Model — TripMind (internal evidence)

Sources compared:
- JPA entities: `backend/src/main/java/com/tripmind/entities/*.java` (21 entities).
- DDL: `scripts/schema.sql` ≡ `backend/src/main/resources/db/schema.sql` (byte-identical, 729 lines, PostgreSQL). It is the old Flyway V1…V12 migrations concatenated ("-- Source: V2__…" sections) + seed of 15 destinations. Applied **manually** (Flyway removed in commit `3a6180c`); integration tests load it via `spring.sql.init.schema-locations`. (VERIFIED)
- Hibernate runs with `ddl-auto: validate` → startup fails if a mapped column/table is missing; extra unmapped DB columns/tables are tolerated. (VERIFIED `application.yml`)
- Test `schema/EnumCheckConstraintTest` asserts every Java enum mapped to a column equals that column's CHECK list. (VERIFIED, passing)

Conventions in DDL (VERIFIED): all PKs `BIGINT GENERATED ALWAYS AS IDENTITY` (entity `@GeneratedValue(IDENTITY)`), timestamps `TIMESTAMPTZ` (entity `Instant`, `@CreationTimestamp/@UpdateTimestamp`), money as `BIGINT` in minor-less units (VND), JSON as `JSONB` (entity `@JdbcTypeCode(SqlTypes.JSON)`), enums as `VARCHAR` + `CHECK` (entity `@Enumerated(STRING)`).

---

## 1. Entity / table list

| # | Table | Entity | Purpose |
|---|---|---|---|
| 1 | users | UserEntity | accounts, role, token_version |
| 2 | user_preferences | UserPreferencesEntity | default travel style/budget/preferences (1:1 user) |
| 3 | destinations | DestinationEntity | cities/regions (seed + Google/manual) |
| 4 | places | PlaceEntity | adopted POIs (Google), unique by provider+external_id |
| 5 | saved_places | SavedPlaceEntity | user favourites (join User–Place) |
| 6 | trips | TripEntity | trip header, preferences, group profile, travel plan, phase override |
| 7 | itinerary_days | ItineraryDayEntity | one row per trip date |
| 8 | activities | ActivityEntity | ordered activities per day, status, costs, link to proposal |
| 9 | activity_alternatives | ActivityAlternativeEntity | ≤3 tier alternatives for food/lodging activity |
| 10 | expenses | ExpenseEntity | actual spending |
| 11 | trip_checklist_items | ChecklistItemEntity | PACK / TODO items |
| 12 | place_ratings | PlaceRatingEntity | LIKE/DISLIKE per user+place after trip |
| 13 | conversations | ConversationEntity | AI chat thread per trip |
| 14 | messages | MessageEntity | chat messages (+ token usage, attachments) |
| 15 | ai_tool_executions | AiToolExecutionEntity | audit log of every tool call |
| 16 | ai_proposals | AiProposalEntity | AI change sets awaiting approval + undo log |
| 17 | destination_knowledge | DestinationKnowledgeEntity | curated tips dataset injected into AI |
| 18 | blog_posts | BlogPostEntity | community posts |
| 19 | blog_comments | BlogCommentEntity | comments |
| 20 | blog_post_upvotes | BlogPostUpvoteEntity | join User–BlogPost |
| 21 | blog_comment_upvotes | BlogCommentUpvoteEntity | join User–BlogComment |
| — | trip_participants | **none** | schema-only (expense split) |
| — | journal_entries | **none** | schema-only (travel journal) |
| — | photos | **none** | schema-only (photo metadata) |

---

## 2. Tables in detail

Notation: PK, FK→table (ON DELETE rule), UQ, CK = CHECK, NN = NOT NULL. "JPA" = how the entity maps it.

### 2.1 users (VERIFIED)
| Column | Type | Constraints | JPA |
|---|---|---|---|
| id | BIGINT identity | PK | Long |
| email | VARCHAR(255) | NN, UQ `uq_users_email` | `unique=true` |
| password_hash | VARCHAR(72) | NN | BCrypt hash; setter bumps token_version on change |
| name | VARCHAR(120) | NN | |
| avatar_url | TEXT | | https URL only (DTO validation) |
| role | VARCHAR(16) | NN default 'USER', CK IN (USER, ADMIN) | enum UserRole |
| is_active | BOOLEAN | NN default true | `setActive(false)` bumps token_version |
| token_version | INT | NN default 0 (added by V11 section via `ALTER TABLE … ADD COLUMN IF NOT EXISTS`) | int; JWT claim `tv` |
| created_at, updated_at | TIMESTAMPTZ | NN default now() | |

### 2.2 user_preferences (VERIFIED)
id PK; user_id NN **UQ** FK→users (CASCADE) — JPA `@OneToOne(LAZY)`; travel_style VARCHAR(16) CK (RELAXED, BALANCED, FAST_PACED); budget_preference VARCHAR(16) CK (BUDGET, MODERATE, LUXURY); preferences_json JSONB NN default '[]' (List<String>); created_at, updated_at.

### 2.3 destinations (VERIFIED)
id PK; provider VARCHAR(24) NN default 'MANUAL' CK (MANUAL, GOOGLE, MAPBOX); external_id VARCHAR(255) (CK: provider='MANUAL' OR external_id NOT NULL); name VARCHAR(160) NN; country VARCHAR(80) NN; latitude/longitude NUMERIC(9,6) NN; timezone VARCHAR(64) NN (IANA); metadata JSONB; created_at.
Indexes: partial UQ `(provider, external_id) WHERE external_id IS NOT NULL`; partial UQ `(country, name) WHERE external_id IS NULL`; idx name; idx external_id. Seed: 15 popular destinations (`INSERT … ON CONFLICT (country,name) WHERE external_id IS NULL DO NOTHING`). JPA: provider is a plain String (no enum), partial unique indexes not declared in entity.

### 2.4 places (VERIFIED)
id PK; provider VARCHAR(24) NN; external_id VARCHAR(255) NN; **UQ (provider, external_id)** (also in entity `@UniqueConstraint`); name VARCHAR(255) NN; category VARCHAR(64); latitude/longitude NUMERIC(9,6) NN; rating NUMERIC(2,1) CK 0..5; user_ratings_total INT default 0; price_level SMALLINT CK 0..4; address TEXT; phone_number VARCHAR(32); website_url TEXT; opening_hours/reviews/photo_urls/metadata JSONB; adopted_via VARCHAR(16) NN default 'SAVED' CK (SAVED, ITINERARY, PROPOSAL, AI_GENERATE) — enum PlaceAdoption; adopted_at, fetched_at, created_at TIMESTAMPTZ NN. Indexes (lat,lng), name.
Rule (VERIFIED `PlaceAdoptionService`): search never inserts; a row is inserted only when a user/AI action adopts a place.

### 2.5 saved_places (VERIFIED) — N–N User↔Place
id PK; user_id NN FK→users (CASCADE) `@ManyToOne(LAZY)`; place_id NN FK→places (**RESTRICT**) `@ManyToOne(EAGER)`; **UQ (user_id, place_id)**; created_at.

### 2.6 trips (VERIFIED)
| Column | Type | Constraints | JPA |
|---|---|---|---|
| id | BIGINT | PK | |
| user_id | BIGINT | NN FK→users CASCADE | `@ManyToOne(LAZY) user` |
| destination_id | BIGINT | NN FK→destinations RESTRICT | `@ManyToOne(EAGER) destination` |
| name | VARCHAR(160) | NN | |
| start_date, end_date | DATE | NN, CK end ≥ start | max 30 days enforced in service (`TripProperties.maxDays`) |
| travelers | SMALLINT | NN default 1, CK ≥1 | short |
| budget | BIGINT | CK NULL or ≥0 | Long |
| currency | CHAR(3) | NN default 'VND' | String (`SqlTypes.CHAR`) |
| travel_style | VARCHAR(16) | CK | enum TravelStyle |
| budget_preference | VARCHAR(16) | CK | enum BudgetPreference |
| preferences_json | JSONB | NN default '[]' | List<String> |
| group_profile | JSONB | NN default '{}' | record `GroupProfile{children, seniors, dietary:Set<DietaryRestriction>, limitedMobility, note, childAges, motionSickness, allergies}` |
| travel_plan | JSONB | NN default '{}' | record `TravelPlan{origin, arrivalTime, departureTime, transportMode, localTransport, lodgingType, lodgingName, budgetIncludesTransport, avoid}` |
| phase_override | VARCHAR(8) | CK (BEFORE, DURING, AFTER) | enum TripPhase |
| constraints_json | JSONB | NN default '[]' | **not mapped** (see §4) |
| created_at, updated_at | TIMESTAMPTZ | NN | |
Index `(user_id, start_date DESC)`. JPA: `@OneToMany(mappedBy="trip", cascade=ALL, orphanRemoval=true) @OrderBy("dayNumber") days`.

### 2.7 itinerary_days (VERIFIED)
id PK; trip_id NN FK→trips CASCADE (`@ManyToOne(LAZY)`); day_number SMALLINT NN CK ≥1; date DATE NN; note TEXT; created_at.
UQ `(trip_id, day_number)`; UQ `(trip_id, date)` **DEFERRABLE INITIALLY DEFERRED** (so shifting dates works inside one transaction). JPA declares both unique constraints (without deferrable). `@OneToMany(mappedBy="itineraryDay", cascade=ALL, orphanRemoval=true) @OrderBy("orderIndex") activities`.

### 2.8 activities (VERIFIED)
| Column | Type | Constraints | JPA |
|---|---|---|---|
| id | BIGINT | PK | |
| itinerary_day_id | BIGINT | NN FK→itinerary_days CASCADE | `@ManyToOne(LAZY) itineraryDay` |
| title | VARCHAR(200) | NN | |
| place_id | BIGINT | FK→places RESTRICT | `@ManyToOne(LAZY) place` (nullable: activity without place allowed) |
| activity_type | VARCHAR(16) | NN CK (SIGHTSEEING, FOOD, TRANSPORT, ACCOMMODATION, REST, OTHER) | enum ActivityType |
| created_by | VARCHAR(8) | NN default 'USER' CK (USER, AI) | enum ActivityCreator |
| start_time, end_time | TIME | | LocalTime; end ≥ start enforced in service |
| estimated_cost | BIGINT | CK NULL or ≥0 | Long |
| estimated_cost_source | VARCHAR(12) | CK (USER, PRICE_LEVEL, AI) | enum CostSource |
| transportation_mode | VARCHAR(16) | | String (default MOTORBIKE in service) |
| notes | TEXT | | |
| order_index | SMALLINT | NN CK ≥0; **UQ (itinerary_day_id, order_index) DEFERRABLE INITIALLY DEFERRED** | short |
| status | VARCHAR(10) | NN default 'PLANNED' CK (PLANNED, DOING, DONE, SKIPPED) | enum ActivityStatus |
| actual_start, actual_end | TIME | | |
| skip_reason | VARCHAR(12) | CK (RAIN, TIRED, CLOSED, NO_TIME, OTHER) | enum SkipReason |
| from_proposal_id | BIGINT | FK→ai_proposals SET NULL | **plain Long** (no JPA relation) |
| created_at, updated_at | TIMESTAMPTZ | NN | |
Indexes: (itinerary_day_id, order_index), place_id, partial from_proposal_id.

### 2.9 activity_alternatives (VERIFIED)
id PK; activity_id NN FK→activities CASCADE (`@ManyToOne(LAZY)`); place_id FK→places SET NULL (`@ManyToOne(LAZY)`); place_name VARCHAR(200); title VARCHAR(200) NN; tier VARCHAR(20) NN CK (BUDGET, BALANCED, PREMIUM) — **String in JPA, no enum**; tier_label VARCHAR(60); estimated_cost BIGINT; rating NUMERIC(2,1); user_ratings_total INT; address TEXT; photo_url TEXT; is_selected BOOLEAN NN default false; created_at. Index (activity_id, is_selected), partial place_id.

### 2.10 expenses (VERIFIED)
id PK; trip_id NN FK→trips CASCADE (`@ManyToOne(LAZY)`); activity_id FK→activities SET NULL (`@ManyToOne(LAZY)`); category VARCHAR(16) NN CK (ACCOMMODATION, FOOD, TRANSPORTATION, ACTIVITIES, SHOPPING, OTHER); amount BIGINT NN CK ≥0; currency CHAR(3) NN (must equal trip currency — service rule); description VARCHAR(255); expense_date DATE NN; paid_by BIGINT FK→trip_participants SET NULL (**plain Long, never set by API**); share_with JSONB NN default '[]' (List<Long>, unused); source VARCHAR(8) NN default 'FORM' CK (FORM, CHAT) — CHAT never produced; created_at. Indexes (trip_id, expense_date), activity_id, paid_by.

### 2.11 trip_checklist_items (VERIFIED)
id PK; trip_id NN FK→trips CASCADE; kind VARCHAR(8) NN CK (PACK, TODO); title VARCHAR(120) NN; category VARCHAR(16) CK (CLOTHING, TOILETRIES, HEALTH, DOCUMENTS, ELECTRONICS, MONEY, BOOKING, KIDS, OTHER); due_date DATE (TODO only); is_done BOOLEAN NN default false; source VARCHAR(10) NN default 'USER' CK (USER, SUGGESTED, AI); reason VARCHAR(200); order_index SMALLINT NN default 0; created_at, updated_at. **Unique index (trip_id, kind, lower(title))** (DB only); index (trip_id, kind, order_index).

### 2.12 place_ratings (VERIFIED) — N–N User↔Place with attribute
id PK; user_id NN FK→users CASCADE (**plain Long**); place_id NN FK→places CASCADE (`@ManyToOne(EAGER)`); trip_id FK→trips SET NULL (plain Long; rating survives trip deletion); verdict VARCHAR(8) NN CK (LIKE, DISLIKE); note VARCHAR(200); created_at, updated_at; **UQ (user_id, place_id)** (DB only; not declared in entity). Index (user_id, verdict, updated_at DESC).

### 2.13 conversations (VERIFIED)
id PK; user_id NN FK→users CASCADE; trip_id NN FK→trips CASCADE (both **plain Long** in JPA); title VARCHAR(200); created_at, updated_at. Indexes (trip_id, updated_at DESC), user_id.

### 2.14 messages (VERIFIED)
id PK; conversation_id NN FK→conversations CASCADE (plain Long); role VARCHAR(12) NN CK (SYSTEM, USER, ASSISTANT, TOOL); content TEXT; tool_calls_json JSONB; tool_call_id VARCHAR(64); token_usage JSONB; attachments_json JSONB (proposalIds, places, checklist); created_at. Index (conversation_id, created_at). In practice only USER and ASSISTANT rows are written (`ConversationService`), tool pairs are kept in ai_tool_executions (INFERRED from `ConversationService` Javadoc).

### 2.15 ai_tool_executions (VERIFIED)
id PK; conversation_id FK→conversations CASCADE; message_id FK→messages SET NULL; trip_id FK→trips SET NULL; user_id FK→users SET NULL (all plain Long, nullable); tool_name VARCHAR(64) NN; arguments JSONB; result JSONB; status VARCHAR(12) NN CK (OK, ERROR, TIMEOUT); error_message TEXT; execution_time_ms INT NN; created_at. Indexes (user_id, created_at DESC), (tool_name, created_at DESC), (conversation_id, created_at), (trip_id, created_at DESC).

### 2.16 ai_proposals (VERIFIED)
id PK; trip_id NN FK→trips CASCADE; conversation_id NN FK→conversations CASCADE; message_id FK→messages SET NULL (all plain Long); summary TEXT NN; changes_json JSONB NN (list of ADD/REMOVE/UPDATE/REORDER — `domains/proposals/ProposalChange`); estimated_cost_delta BIGINT; travel_time_delta INT; status VARCHAR(16) NN default 'PENDING' CK (PENDING, APPLIED, REJECTED, EXPIRED, REVERTED); kind VARCHAR(12) NN default 'ITINERARY' CK (ITINERARY, EXPENSE); evidence_json JSONB (reason, toolExecutionIds, rejected); applied_seq BIGINT (ordering for LIFO undo); undo_json JSONB (undo log + fingerprints); reverted_at, expires_at (NN), applied_at, created_at TIMESTAMPTZ. Unmapped columns: expense_json, option_group, option_title, metrics_json, constraints_json. Indexes (trip_id, status, created_at DESC), conversation_id, option_group.

### 2.17 destination_knowledge (VERIFIED)
id PK; destination_id FK→destinations SET NULL (`@ManyToOne(LAZY)`); destination_name VARCHAR(160) NN; category VARCHAR(32) NN CK (FOOD, ACCOMMODATION, TRANSPORT, SIGHTSEEING, TIP, WARNING); title VARCHAR(255) NN; content TEXT NN; place_name VARCHAR(200); source_name VARCHAR(100); source_url TEXT; tags JSONB NN default '[]'; priority INT NN default 1 CK 1..3; is_active BOOLEAN NN default true; created_at, updated_at. Indexes (destination_name, is_active), category, (priority DESC, created_at DESC), (destination_id, is_active), partial LOWER(destination_name) WHERE destination_id IS NULL.

### 2.18 blog_posts (VERIFIED)
id PK; user_id NN FK→users CASCADE (`@ManyToOne(EAGER)`); trip_id FK→trips SET NULL (`@ManyToOne(LAZY)`); destination_name VARCHAR(160) NN (free text, **not FK** to destinations); title VARCHAR(255) NN (API limits 200); content TEXT NN (API ≤20000); photo_urls JSONB NN default '[]'; upvote_count INT NN default 0 (denormalised, re-synced from upvote table); comment_count INT NN default 0 (denormalised); converted_to_knowledge_id FK→destination_knowledge SET NULL (`@ManyToOne(LAZY) convertedKnowledge`); created_at, updated_at. Indexes (destination_name, created_at DESC), (user_id, created_at DESC), (upvote_count DESC, created_at DESC). No index on trip_id (used by `existsByTripId` in uncommitted copy flow).

### 2.19 blog_comments (VERIFIED)
id PK; post_id NN FK→blog_posts CASCADE (`@ManyToOne(LAZY)`); user_id NN FK→users CASCADE (`@ManyToOne(EAGER)`); content TEXT NN (API ≤2000); upvote_count INT NN default 0; created_at, updated_at. Index (post_id, created_at).

### 2.20 blog_post_upvotes / 2.21 blog_comment_upvotes (VERIFIED)
id PK; user_id NN FK→users CASCADE; post_id (resp. comment_id) NN FK→blog_posts (resp. blog_comments) CASCADE; created_at; **UQ (user_id, post_id)** / **UQ (user_id, comment_id)** (declared in entity too). JPA maps both ids as plain Long. Toggle uses native `INSERT … ON CONFLICT DO NOTHING`.

### 2.22 Schema-only tables (no entity, no API) (VERIFIED)
- trip_participants: id, trip_id FK→trips CASCADE, name VARCHAR(80), is_me BOOLEAN.
- journal_entries: id, trip_id, itinerary_day_id, activity_id (all FK CASCADE), note VARCHAR(280), mood CK (GOOD, OK, BAD), UQ (trip_id, itinerary_day_id, activity_id).
- photos: id VARCHAR(16) PK, trip_id, itinerary_day_id, activity_id, journal_entry_id (FK CASCADE), width, height, bytes CK ≥0, mime, storage_key, thumb.

---

## 3. Relationships & cardinality

| Relationship | Cardinality | Implemented by | JPA navigation? | Cascade (JPA / DB) |
|---|---|---|---|---|
| User – UserPreferences | 1 : 0..1 | user_preferences.user_id UQ | yes (`@OneToOne`) | — / CASCADE |
| User – Trip | 1 : N | trips.user_id | Trip→User | — / CASCADE |
| Destination – Trip | 1 : N | trips.destination_id | Trip→Destination | — / RESTRICT |
| Trip – ItineraryDay | 1 : N (one per date) | itinerary_days.trip_id | bidirectional | ALL + orphanRemoval / CASCADE |
| ItineraryDay – Activity | 1 : N (ordered) | activities.itinerary_day_id | bidirectional | ALL + orphanRemoval / CASCADE |
| Place – Activity | 0..1 : N | activities.place_id | Activity→Place | — / RESTRICT |
| Activity – ActivityAlternative | 1 : 0..3 | activity_alternatives.activity_id | Alt→Activity | — / CASCADE |
| Place – ActivityAlternative | 0..1 : N | activity_alternatives.place_id | Alt→Place | — / SET NULL |
| User – Place (favourites) | N : N via saved_places | UQ(user, place) | SavedPlace→User, →Place | — / CASCADE user, RESTRICT place |
| User – Place (ratings) | N : N via place_ratings (+verdict) | UQ(user, place) | Rating→Place only | — / CASCADE |
| Trip – Expense | 1 : N | expenses.trip_id | Expense→Trip | — / CASCADE |
| Activity – Expense | 0..1 : N | expenses.activity_id | Expense→Activity | — / SET NULL |
| Trip – ChecklistItem | 1 : N | trip_checklist_items.trip_id | Item→Trip | — / CASCADE |
| User/Trip – Conversation | 1 : N each | conversations.user_id, trip_id | ids only | — / CASCADE |
| Conversation – Message | 1 : N | messages.conversation_id | id only | — / CASCADE |
| Conversation – AiProposal | 1 : N | ai_proposals.conversation_id | id only | — / CASCADE |
| Trip – AiProposal | 1 : N | ai_proposals.trip_id | id only | — / CASCADE |
| AiProposal – Activity | 0..1 : N | activities.from_proposal_id | id only | — / SET NULL |
| Conversation/Message/Trip/User – AiToolExecution | 0..1 : N each | nullable FKs | ids only | CASCADE (conversation) / SET NULL (others) |
| Destination – DestinationKnowledge | 0..1 : N | destination_knowledge.destination_id | Knowledge→Destination | — / SET NULL |
| User – BlogPost | 1 : N | blog_posts.user_id | Post→User | — / CASCADE |
| Trip – BlogPost | 0..1 : N | blog_posts.trip_id | Post→Trip | — / SET NULL |
| BlogPost – DestinationKnowledge | N : 0..1 | blog_posts.converted_to_knowledge_id | Post→Knowledge | — / SET NULL |
| BlogPost – BlogComment | 1 : N | blog_comments.post_id | Comment→Post | — / CASCADE |
| User – BlogComment | 1 : N | blog_comments.user_id | Comment→User | — / CASCADE |
| User – BlogPost (upvotes) | N : N via blog_post_upvotes | UQ | ids only | — / CASCADE |
| User – BlogComment (upvotes) | N : N via blog_comment_upvotes | UQ | ids only | — / CASCADE |

Deleting a user (admin) cascades at DB level to preferences, saved places, trips (→ days → activities → alternatives; expenses; checklist; conversations → messages, proposals), ratings, blog posts/comments/upvotes; nullifies tool-execution user_id. Deleting a place referenced by saved_places/activities is blocked (RESTRICT). Deleting a destination used by a trip is blocked. (VERIFIED from FK definitions; JPA delete of `UserEntity` relies on DB cascades — INFERRED: works because entities hold no inverse collections from User.)

---

## 4. Entity ↔ schema.sql cross-check (mismatches flagged)

| # | Finding | Severity | Tag |
|---|---|---|---|
| M1 | Tables `trip_participants`, `journal_entries`, `photos` exist in schema with no entity/API (leftover of a larger design shown in `demo/`). | Dead schema | VERIFIED |
| M2 | `trips.constraints_json` (NN default '[]') not mapped. | Unmapped column | VERIFIED |
| M3 | `ai_proposals.expense_json, option_group, option_title, metrics_json, constraints_json` not mapped; index on `option_group` unused. | Unmapped columns | VERIFIED |
| M4 | `expenses.paid_by` (FK→trip_participants) and `share_with` mapped but never written by any service; `expenses.source='CHAT'` and `ai_proposals.kind='EXPENSE'` never produced. | Unused features | VERIFIED (grep) |
| M5 | Many FKs mapped as plain `Long` (no `@ManyToOne`): activities.from_proposal_id, conversations.*, messages.conversation_id, ai_* ids, place_ratings.user_id/trip_id, blog upvote ids. Referential integrity is enforced only by DB FKs. | Design choice | VERIFIED |
| M6 | DB-only constraints not declared in entities: deferrable UQ (itinerary_day_id, order_index); deferrable on UQ (trip_id, date); functional UQ (trip_id, kind, lower(title)); partial UQs on destinations; UQ (user_id, place_id) on place_ratings; all CHECKs. Since `ddl-auto=validate`, these are authoritative only in schema.sql. | Documentation note | VERIFIED |
| M7 | `blog_posts.title` VARCHAR(255) vs `BlogPostRequest.title` max 200; `trips.name` 160 both sides; ok. | Minor | VERIFIED |
| M8 | `activity_alternatives.tier` mapped as `String` (CK BUDGET/BALANCED/PREMIUM) — not covered by `EnumCheckConstraintTest`. `destinations.provider` also String. | Minor | VERIFIED |
| M9 | Base `CREATE TABLE users` lacks `token_version`; it is added later in the same file by `ALTER TABLE … ADD COLUMN IF NOT EXISTS` (V11 section). Running the whole file works; running only the CREATE section would fail Hibernate validation. Same pattern for trips columns (V2, V7) and activities.estimated_cost_source (V3), messages.attachments_json (V4). | Note | VERIFIED |
| M10 | Old Flyway files `db/migration/V1…V12__*.sql` remain on disk but are no longer executed; `schema.sql` drops `flyway_schema_history`. Docs/tests referencing "migrations" (`MigrationSmokeTest` name/DisplayName) are historical. | Note | VERIFIED |
| M11 | Design doc ERD (§20) differs: has `weather_cache` table (replaced by Redis), lacks proposals/checklist/blog/knowledge/ratings/alternatives tables. | Design vs implementation | VERIFIED |
| M12 | `blog_posts.destination_name` and `destination_knowledge.destination_name` are free text, not FK; knowledge lookup matches by destination_id or LOWER(name). | Note | VERIFIED |

---

## 5. Enumerations (Java enum = DB CHECK, verified by `EnumCheckConstraintTest`)

| Enum | Values | Column(s) |
|---|---|---|
| UserRole | USER, ADMIN | users.role |
| TravelStyle | RELAXED, BALANCED, FAST_PACED | trips.travel_style, user_preferences.travel_style |
| BudgetPreference | BUDGET, MODERATE, LUXURY | trips.budget_preference, user_preferences.budget_preference |
| TripPhase | BEFORE, DURING, AFTER | trips.phase_override |
| ActivityType | SIGHTSEEING, FOOD, TRANSPORT, ACCOMMODATION, REST, OTHER | activities.activity_type |
| ActivityCreator | USER, AI | activities.created_by |
| ActivityStatus | PLANNED, DOING, DONE, SKIPPED | activities.status |
| SkipReason | RAIN, TIRED, CLOSED, NO_TIME, OTHER | activities.skip_reason |
| CostSource | USER, PRICE_LEVEL, AI | activities.estimated_cost_source |
| ExpenseCategory | ACCOMMODATION, FOOD, TRANSPORTATION, ACTIVITIES, SHOPPING, OTHER | expenses.category |
| ExpenseSource | FORM, CHAT | expenses.source |
| ChecklistKind | PACK, TODO | trip_checklist_items.kind |
| ChecklistCategory | CLOTHING, TOILETRIES, HEALTH, DOCUMENTS, ELECTRONICS, MONEY, BOOKING, KIDS, OTHER | trip_checklist_items.category |
| ChecklistSource | USER, SUGGESTED, AI | trip_checklist_items.source |
| PlaceAdoption | SAVED, ITINERARY, PROPOSAL, AI_GENERATE | places.adopted_via |
| PlaceVerdict | LIKE, DISLIKE | place_ratings.verdict |
| MessageRole | SYSTEM, USER, ASSISTANT, TOOL | messages.role |
| ToolExecutionStatus | OK, ERROR, TIMEOUT | ai_tool_executions.status |
| ProposalStatus | PENDING, APPLIED, REJECTED, EXPIRED, REVERTED | ai_proposals.status |
| ProposalKind | ITINERARY, EXPENSE | ai_proposals.kind |
| KnowledgeCategory | FOOD, ACCOMMODATION, TRANSPORT, SIGHTSEEING, TIP, WARNING | destination_knowledge.category |
| DietaryRestriction | VEGETARIAN, VEGAN, HALAL, NO_SEAFOOD, NO_PORK | inside trips.group_profile JSON |
| WeatherSource | FORECAST, CLIMATE_NORMAL, OBSERVED | API only (not stored) |

## 6. Non-relational state (Redis) — VERIFIED
See `system-overview.md` §3.4: rate-limit counters, login-failure counters, weather cache (3 h), place candidate cache (1 h), itinerary-generation jobs (job JSON retained 2 h, queue, processing list, active-job pointer, pub/sub topic, per-trip lock). Browser `localStorage`: `access_token`, `user_profile`.
