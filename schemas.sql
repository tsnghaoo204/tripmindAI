-- =============================================================================
-- TripMind AI Travel Planner — Database Schema (Full Snapshot v2.8)
-- File: schemas.sql
-- Engine: PostgreSQL 16+
-- Notes:
--   - All entity/table identifiers, enums, constraints use standard English.
--   - Monetary values stored as BIGINT (smallest currency unit, e.g., VND dong, USD cents).
--   - Primary keys use BIGINT GENERATED ALWAYS AS IDENTITY.
--   - Managed by Flyway in backend/src/main/resources/db/migration/
-- =============================================================================

-- =============================================================================
-- 1. USERS & PREFERENCES
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(72) NOT NULL,
    name VARCHAR(120) NOT NULL,
    avatar_url TEXT,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    travel_style VARCHAR(16),
    budget_preference VARCHAR(16),
    preferences_json JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_preferences_user UNIQUE (user_id),
    CONSTRAINT chk_user_preferences_travel_style CHECK (travel_style IS NULL OR travel_style IN ('RELAXED', 'BALANCED', 'FAST_PACED')),
    CONSTRAINT chk_user_preferences_budget_pref CHECK (budget_preference IS NULL OR budget_preference IN ('BUDGET', 'MODERATE', 'LUXURY'))
);

-- =============================================================================
-- 2. DESTINATIONS & PLACES
-- =============================================================================

CREATE TABLE IF NOT EXISTS destinations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider VARCHAR(24) NOT NULL DEFAULT 'MANUAL',
    external_id VARCHAR(255),
    name VARCHAR(160) NOT NULL,
    country VARCHAR(80) NOT NULL,
    latitude NUMERIC(9,6) NOT NULL,
    longitude NUMERIC(9,6) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_destinations_provider CHECK (provider IN ('MANUAL', 'GOOGLE', 'MAPBOX')),
    CONSTRAINT chk_destinations_external_id CHECK (provider = 'MANUAL' OR external_id IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_destinations_provider_external
    ON destinations (provider, external_id) WHERE external_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_destinations_country_name
    ON destinations (country, name) WHERE external_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_destinations_name ON destinations (name);
CREATE INDEX IF NOT EXISTS idx_destinations_external ON destinations (external_id) WHERE external_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS places (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider VARCHAR(24) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(64),
    latitude NUMERIC(9,6) NOT NULL,
    longitude NUMERIC(9,6) NOT NULL,
    rating NUMERIC(2,1),
    user_ratings_total INTEGER DEFAULT 0,
    price_level SMALLINT,
    address TEXT,
    phone_number VARCHAR(32),
    website_url TEXT,
    opening_hours JSONB,
    reviews JSONB,
    photo_urls JSONB,
    metadata JSONB,
    adopted_via VARCHAR(16) NOT NULL DEFAULT 'SAVED',
    adopted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_places_provider_external UNIQUE (provider, external_id),
    CONSTRAINT chk_places_rating CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5)),
    CONSTRAINT chk_places_price_level CHECK (price_level IS NULL OR (price_level >= 0 AND price_level <= 4)),
    CONSTRAINT chk_places_adopted_via CHECK (adopted_via IN ('SAVED', 'ITINERARY', 'PROPOSAL', 'AI_GENERATE'))
);

CREATE INDEX IF NOT EXISTS idx_places_coords ON places (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_places_name ON places (name);

CREATE TABLE IF NOT EXISTS saved_places (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_saved_places_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_places_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE RESTRICT,
    CONSTRAINT uq_saved_places_user_place UNIQUE (user_id, place_id)
);

CREATE TABLE IF NOT EXISTS place_ratings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    trip_id BIGINT,
    verdict VARCHAR(8) NOT NULL,
    note VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_place_ratings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_place_ratings_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    CONSTRAINT chk_place_ratings_verdict CHECK (verdict IN ('LIKE', 'DISLIKE')),
    CONSTRAINT uq_place_ratings_user_place UNIQUE (user_id, place_id)
);

CREATE INDEX IF NOT EXISTS idx_place_ratings_user_verdict ON place_ratings (user_id, verdict, updated_at DESC);

-- =============================================================================
-- 3. TRIPS, ITINERARY & ACTIVITIES
-- =============================================================================

CREATE TABLE IF NOT EXISTS trips (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    destination_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    travelers SMALLINT NOT NULL DEFAULT 1,
    budget BIGINT,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    travel_style VARCHAR(16),
    budget_preference VARCHAR(16),
    preferences_json JSONB NOT NULL DEFAULT '[]',
    group_profile JSONB NOT NULL DEFAULT '{}',
    constraints_json JSONB NOT NULL DEFAULT '[]',
    phase_override VARCHAR(8),
    travel_plan JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_trips_destination FOREIGN KEY (destination_id) REFERENCES destinations(id) ON DELETE RESTRICT,
    CONSTRAINT chk_trips_date_range CHECK (end_date >= start_date),
    CONSTRAINT chk_trips_travelers CHECK (travelers >= 1),
    CONSTRAINT chk_trips_budget CHECK (budget IS NULL OR budget >= 0),
    CONSTRAINT chk_trips_travel_style CHECK (travel_style IS NULL OR travel_style IN ('RELAXED', 'BALANCED', 'FAST_PACED')),
    CONSTRAINT chk_trips_budget_pref CHECK (budget_preference IS NULL OR budget_preference IN ('BUDGET', 'MODERATE', 'LUXURY')),
    CONSTRAINT chk_trips_phase_override CHECK (phase_override IS NULL OR phase_override IN ('BEFORE', 'DURING', 'AFTER'))
);

CREATE INDEX IF NOT EXISTS idx_trips_user_start ON trips (user_id, start_date DESC);

CREATE TABLE IF NOT EXISTS itinerary_days (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    day_number SMALLINT NOT NULL,
    date DATE NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_itinerary_days_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT uq_itinerary_days_trip_day UNIQUE (trip_id, day_number),
    CONSTRAINT uq_itinerary_days_trip_date UNIQUE (trip_id, date) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_itinerary_days_number CHECK (day_number >= 1)
);

CREATE TABLE IF NOT EXISTS activities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    itinerary_day_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    place_id BIGINT,
    activity_type VARCHAR(16) NOT NULL,
    created_by VARCHAR(8) NOT NULL DEFAULT 'USER',
    start_time TIME,
    end_time TIME,
    estimated_cost BIGINT,
    estimated_cost_source VARCHAR(12),
    transportation_mode VARCHAR(16),
    notes TEXT,
    order_index SMALLINT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PLANNED',
    actual_start TIME,
    actual_end TIME,
    skip_reason VARCHAR(12),
    from_proposal_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_activities_itinerary_day FOREIGN KEY (itinerary_day_id) REFERENCES itinerary_days(id) ON DELETE CASCADE,
    CONSTRAINT fk_activities_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE RESTRICT,
    CONSTRAINT chk_activities_type CHECK (activity_type IN ('SIGHTSEEING', 'FOOD', 'TRANSPORT', 'ACCOMMODATION', 'REST', 'OTHER')),
    CONSTRAINT chk_activities_created_by CHECK (created_by IN ('USER', 'AI')),
    CONSTRAINT chk_activities_cost CHECK (estimated_cost IS NULL OR estimated_cost >= 0),
    CONSTRAINT chk_activities_cost_source CHECK (estimated_cost_source IS NULL OR estimated_cost_source IN ('USER', 'PRICE_LEVEL', 'AI')),
    CONSTRAINT chk_activities_order CHECK (order_index >= 0),
    CONSTRAINT chk_activities_status CHECK (status IN ('PLANNED', 'DOING', 'DONE', 'SKIPPED')),
    CONSTRAINT chk_activities_skip_reason CHECK (skip_reason IS NULL OR skip_reason IN ('RAIN', 'TIRED', 'CLOSED', 'NO_TIME', 'OTHER')),
    CONSTRAINT uq_activity_day_order UNIQUE (itinerary_day_id, order_index) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX IF NOT EXISTS idx_activities_day_order ON activities (itinerary_day_id, order_index);
CREATE INDEX IF NOT EXISTS idx_activities_place ON activities (place_id);

-- 3b. ACTIVITY ALTERNATIVES (Top 3 options cho Ăn uống & Lưu trú theo ngân sách)
CREATE TABLE IF NOT EXISTS activity_alternatives (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    place_id BIGINT,
    place_name VARCHAR(200),
    title VARCHAR(200) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    tier_label VARCHAR(60),
    estimated_cost BIGINT,
    rating NUMERIC(2, 1),
    user_ratings_total INTEGER,
    address TEXT,
    photo_url TEXT,
    is_selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_activity_alternatives_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_alternatives_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE SET NULL,
    CONSTRAINT chk_activity_alternatives_tier CHECK (tier IN ('BUDGET', 'BALANCED', 'PREMIUM'))
);

CREATE INDEX IF NOT EXISTS idx_activity_alternatives_activity ON activity_alternatives(activity_id, is_selected);

-- 3c. TRIP CHECKLIST (Đồ cần mang & việc cần làm trước chuyến)
CREATE TABLE IF NOT EXISTS trip_checklist_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    kind VARCHAR(8) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(16),
    due_date DATE,
    is_done BOOLEAN NOT NULL DEFAULT false,
    source VARCHAR(10) NOT NULL DEFAULT 'USER',
    reason VARCHAR(200),
    order_index SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_checklist_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT chk_checklist_kind CHECK (kind IN ('PACK', 'TODO')),
    CONSTRAINT chk_checklist_source CHECK (source IN ('USER', 'SUGGESTED', 'AI')),
    CONSTRAINT chk_checklist_category CHECK (category IS NULL OR category IN
        ('CLOTHING', 'TOILETRIES', 'HEALTH', 'DOCUMENTS', 'ELECTRONICS', 'MONEY', 'BOOKING', 'KIDS', 'OTHER'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_checklist_trip_kind_title ON trip_checklist_items (trip_id, kind, lower(title));
CREATE INDEX IF NOT EXISTS idx_checklist_trip_order ON trip_checklist_items (trip_id, kind, order_index);

-- 3d. DESTINATION KNOWLEDGE / COMMUNITY DATASET (Kinh nghiệm & Mẹo du lịch từ diễn đàn/mạng xã hội cho AI)
CREATE TABLE IF NOT EXISTS destination_knowledge (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    destination_id BIGINT,
    destination_name VARCHAR(160) NOT NULL,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    place_name VARCHAR(200),
    source_name VARCHAR(100),
    source_url TEXT,
    tags JSONB NOT NULL DEFAULT '[]',
    priority INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_knowledge_destination FOREIGN KEY (destination_id) REFERENCES destinations(id) ON DELETE SET NULL,
    CONSTRAINT chk_knowledge_category CHECK (category IN ('FOOD', 'ACCOMMODATION', 'TRANSPORT', 'SIGHTSEEING', 'TIP', 'WARNING')),
    CONSTRAINT chk_knowledge_priority CHECK (priority BETWEEN 1 AND 3)
);

CREATE INDEX IF NOT EXISTS idx_destination_knowledge_dest ON destination_knowledge(destination_name, is_active);
CREATE INDEX IF NOT EXISTS idx_destination_knowledge_cat ON destination_knowledge(category);
CREATE INDEX IF NOT EXISTS idx_destination_knowledge_prio ON destination_knowledge(priority DESC, created_at DESC);

-- =============================================================================
-- 4. BUDGET, EXPENSES, PARTICIPANTS, JOURNAL & PHOTOS
-- =============================================================================

CREATE TABLE IF NOT EXISTS trip_participants (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    is_me BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_trip_participants_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_trip_participants_trip ON trip_participants (trip_id);

CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    activity_id BIGINT,
    category VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    description VARCHAR(255),
    expense_date DATE NOT NULL,
    paid_by BIGINT,
    share_with JSONB NOT NULL DEFAULT '[]',
    source VARCHAR(8) NOT NULL DEFAULT 'FORM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_expenses_source CHECK (source IN ('FORM', 'CHAT')),
    CONSTRAINT fk_expenses_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE SET NULL,
    CONSTRAINT fk_expenses_paid_by FOREIGN KEY (paid_by) REFERENCES trip_participants(id) ON DELETE SET NULL,
    CONSTRAINT chk_expenses_category CHECK (category IN ('ACCOMMODATION', 'FOOD', 'TRANSPORTATION', 'ACTIVITIES', 'SHOPPING', 'OTHER')),
    CONSTRAINT chk_expenses_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_expenses_trip_date ON expenses (trip_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_activity ON expenses (activity_id);
CREATE INDEX IF NOT EXISTS idx_expenses_paid_by ON expenses (paid_by);

CREATE TABLE IF NOT EXISTS journal_entries (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    itinerary_day_id BIGINT NOT NULL,
    activity_id BIGINT,
    note VARCHAR(280),
    mood VARCHAR(4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_journal_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_day FOREIGN KEY (itinerary_day_id) REFERENCES itinerary_days(id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT chk_journal_mood CHECK (mood IS NULL OR mood IN ('GOOD', 'OK', 'BAD')),
    CONSTRAINT uq_journal_target UNIQUE (trip_id, itinerary_day_id, activity_id)
);

CREATE TABLE IF NOT EXISTS photos (
    id VARCHAR(16) PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    itinerary_day_id BIGINT NOT NULL,
    activity_id BIGINT,
    journal_entry_id BIGINT,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    bytes INTEGER NOT NULL,
    mime VARCHAR(32) NOT NULL,
    storage_key TEXT NOT NULL,
    thumb TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_photos_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_photos_day FOREIGN KEY (itinerary_day_id) REFERENCES itinerary_days(id) ON DELETE CASCADE,
    CONSTRAINT fk_photos_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_photos_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE,
    CONSTRAINT chk_photos_bytes CHECK (bytes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_photos_trip ON photos (trip_id);

-- =============================================================================
-- 5. AI ASSISTANT, CONVERSATIONS, MESSAGES & PROPOSALS
-- =============================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    title VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conversations_trip_updated ON conversations (trip_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_user ON conversations (user_id);

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(12) NOT NULL,
    content TEXT,
    tool_calls_json JSONB,
    tool_call_id VARCHAR(64),
    token_usage JSONB,
    attachments_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT chk_messages_role CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT', 'TOOL'))
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_created ON messages (conversation_id, created_at);

CREATE TABLE IF NOT EXISTS ai_tool_executions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id BIGINT,
    message_id BIGINT,
    trip_id BIGINT,
    user_id BIGINT,
    tool_name VARCHAR(64) NOT NULL,
    arguments JSONB,
    result JSONB,
    status VARCHAR(12) NOT NULL,
    error_message TEXT,
    execution_time_ms INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_ai_tool_executions_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_tool_executions_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_tool_executions_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_tool_executions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_ai_tool_executions_status CHECK (status IN ('OK', 'ERROR', 'TIMEOUT'))
);

CREATE INDEX IF NOT EXISTS idx_ai_tool_executions_user ON ai_tool_executions (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_tool_executions_tool ON ai_tool_executions (tool_name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_tool_executions_conversation ON ai_tool_executions (conversation_id, created_at);

CREATE TABLE IF NOT EXISTS ai_proposals (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    message_id BIGINT,
    summary TEXT NOT NULL,
    changes_json JSONB NOT NULL,
    estimated_cost_delta BIGINT,
    travel_time_delta INTEGER,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    kind VARCHAR(12) NOT NULL DEFAULT 'ITINERARY',
    expense_json JSONB,
    option_group VARCHAR(16),
    option_title VARCHAR(60),
    metrics_json JSONB,
    constraints_json JSONB,
    evidence_json JSONB,
    applied_seq BIGINT,
    undo_json JSONB,
    reverted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_ai_proposals_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_proposals_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_proposals_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL,
    CONSTRAINT chk_ai_proposals_status CHECK (status IN ('PENDING', 'APPLIED', 'REJECTED', 'EXPIRED', 'REVERTED')),
    CONSTRAINT chk_ai_proposals_kind CHECK (kind IN ('ITINERARY', 'EXPENSE'))
);

CREATE INDEX IF NOT EXISTS idx_ai_proposals_trip_status ON ai_proposals (trip_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_proposals_conversation ON ai_proposals (conversation_id);
CREATE INDEX IF NOT EXISTS idx_ai_proposals_option_group ON ai_proposals (option_group);

-- Foreign key link from activities to ai_proposals
ALTER TABLE activities ADD CONSTRAINT fk_activities_from_proposal
    FOREIGN KEY (from_proposal_id) REFERENCES ai_proposals(id) ON DELETE SET NULL;

-- =============================================================================
-- 6. SEED DATA (POPULAR DESTINATIONS)
-- =============================================================================

INSERT INTO destinations (provider, name, country, latitude, longitude, timezone, metadata)
VALUES
    ('MANUAL', 'Da Nang', 'Vietnam', 16.054407, 108.202167, 'Asia/Ho_Chi_Minh', '{"region": "Central", "description": "Coastal city famous for My Khe Beach, Ba Na Hills, Dragon Bridge, and rich culinary culture."}'),
    ('MANUAL', 'Hoi An', 'Vietnam', 15.880058, 108.338047, 'Asia/Ho_Chi_Minh', '{"region": "Central", "description": "UNESCO World Heritage ancient town with lantern-lit streets and historic architecture."}'),
    ('MANUAL', 'Hue', 'Vietnam', 16.463713, 107.590866, 'Asia/Ho_Chi_Minh', '{"region": "Central", "description": "Historic imperial capital with royal palaces, tombs, and traditional royal cuisine."}'),
    ('MANUAL', 'Hanoi', 'Vietnam', 21.028511, 105.854167, 'Asia/Ho_Chi_Minh', '{"region": "North", "description": "Thousand-year-old capital with French quarter, Old Quarter, and famous street food culture."}'),
    ('MANUAL', 'Ha Long', 'Vietnam', 20.950454, 107.073364, 'Asia/Ho_Chi_Minh', '{"region": "North", "description": "World wonder bay featuring thousands of limestone karsts and emerald waters."}'),
    ('MANUAL', 'Sa Pa', 'Vietnam', 22.336361, 103.843773, 'Asia/Ho_Chi_Minh', '{"region": "North", "description": "Mountain town famed for terraced rice paddies, Fansipan peak, and ethnic minority culture."}'),
    ('MANUAL', 'Ninh Binh', 'Vietnam', 20.250614, 105.974457, 'Asia/Ho_Chi_Minh', '{"region": "North", "description": "Ha Long Bay on land featuring Trang An landscape complex, caves, and boat tours."}'),
    ('MANUAL', 'Ho Chi Minh City', 'Vietnam', 10.823099, 106.629664, 'Asia/Ho_Chi_Minh', '{"region": "South", "description": "Dynamic economic powerhouse with buzzing nightlife, skyscrapers, and vibrant cafe culture."}'),
    ('MANUAL', 'Da Lat', 'Vietnam', 11.940419, 108.458313, 'Asia/Ho_Chi_Minh', '{"region": "Central Highlands", "description": "City of Eternal Spring with pine hills, cool weather, flower valleys, and romance."}'),
    ('MANUAL', 'Nha Trang', 'Vietnam', 12.238791, 109.196749, 'Asia/Ho_Chi_Minh', '{"region": "Central", "description": "Famous beach resort with diving sites, islands, amusement parks, and seafood."}'),
    ('MANUAL', 'Phu Quoc', 'Vietnam', 10.289879, 103.984020, 'Asia/Ho_Chi_Minh', '{"region": "South", "description": "Tropical paradise island known for white sand beaches, sunset views, and luxury resorts."}'),
    ('MANUAL', 'Quy Nhon', 'Vietnam', 13.782967, 109.219666, 'Asia/Ho_Chi_Minh', '{"region": "Central", "description": "Pristine coastal destination with Ky Co beach, Eo Gio, and Cham heritage."}'),
    ('MANUAL', 'Can Tho', 'Vietnam', 10.045162, 105.746857, 'Asia/Ho_Chi_Minh', '{"region": "South", "description": "Heart of the Mekong Delta famous for Cai Rang floating market and fruit orchards."}'),
    ('MANUAL', 'Vung Tau', 'Vietnam', 10.345991, 107.084267, 'Asia/Ho_Chi_Minh', '{"region": "South", "description": "Popular weekend coastal getaway near Ho Chi Minh City."}'),
    ('MANUAL', 'Phan Thiet', 'Vietnam', 10.980461, 108.261475, 'Asia/Ho_Chi_Minh', '{"region": "South", "description": "Coastal town famed for Mui Ne red and white sand dunes, resorts, and water sports."}')
ON CONFLICT (country, name) WHERE external_id IS NULL DO NOTHING;
