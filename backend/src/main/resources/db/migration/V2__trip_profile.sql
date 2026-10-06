-- V2: sở thích theo từng chuyến + thông tin nhóm đi; cho phép dời ngày của chuyến.

-- Trước V2, POST /api/trips nhận travelStyle/budgetPreference/preferences nhưng bảng không
-- có cột để giữ. Mỗi chuyến có sở thích riêng (đi với gia đình khác đi với bạn bè);
-- user_preferences chỉ còn là giá trị mặc định khi tạo chuyến mới.
ALTER TABLE trips
    ADD COLUMN travel_style VARCHAR(16),
    ADD COLUMN budget_preference VARCHAR(16),
    ADD COLUMN preferences_json JSONB NOT NULL DEFAULT '[]',
    -- Thông tin nhóm đi: số trẻ nhỏ, người lớn tuổi, chế độ ăn, đi lại khó khăn.
    ADD COLUMN group_profile JSONB NOT NULL DEFAULT '{}';

ALTER TABLE trips ADD CONSTRAINT chk_trips_travel_style
    CHECK (travel_style IS NULL OR travel_style IN ('RELAXED', 'BALANCED', 'FAST_PACED'));
ALTER TABLE trips ADD CONSTRAINT chk_trips_budget_pref
    CHECK (budget_preference IS NULL OR budget_preference IN ('BUDGET', 'MODERATE', 'LUXURY'));

-- Dời chuyến đi (ví dụ lùi một ngày) phải đổi ngày của từng itinerary_day. Giữa chừng hai
-- dòng có thể tạm trùng ngày, nên ràng buộc phải được kiểm lúc commit chứ không kiểm từng câu.
-- Phải là table constraint: PostgreSQL không hoãn được UNIQUE INDEX.
ALTER TABLE itinerary_days DROP CONSTRAINT uq_itinerary_days_trip_date;
ALTER TABLE itinerary_days ADD CONSTRAINT uq_itinerary_days_trip_date
    UNIQUE (trip_id, date) DEFERRABLE INITIALLY DEFERRED;
