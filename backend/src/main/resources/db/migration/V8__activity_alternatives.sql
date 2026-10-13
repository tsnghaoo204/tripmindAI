-- V8: Lựa chọn thay thế cho hoạt động (Top 3 options cho Ăn uống & Lưu trú theo ngân sách)
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
