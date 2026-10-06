-- V6: đánh giá địa điểm sau chuyến. Mỗi người một ý kiến cho một chỗ; đánh giá lại thì ghi đè.
-- Trợ lý dùng bảng này bằng mã: lọc bỏ chỗ đã chê khỏi kết quả tìm kiếm và khỏi lịch trình sinh tự động.
CREATE TABLE place_ratings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    -- Chuyến lúc đánh giá; xoá chuyến vẫn giữ đánh giá.
    trip_id BIGINT,
    verdict VARCHAR(8) NOT NULL,
    note VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_place_ratings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_place_ratings_place FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    CONSTRAINT fk_place_ratings_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL,
    CONSTRAINT chk_place_ratings_verdict CHECK (verdict IN ('LIKE', 'DISLIKE')),
    CONSTRAINT uq_place_ratings_user_place UNIQUE (user_id, place_id)
);

CREATE INDEX idx_place_ratings_user_verdict ON place_ratings (user_id, verdict, updated_at DESC);
