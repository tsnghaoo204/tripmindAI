-- V5: checklist chuẩn bị cho chuyến đi — đồ cần mang (PACK) và việc cần làm trước chuyến (TODO).
-- Gợi ý (từ bộ luật hoặc từ trợ lý) chỉ được ghi vào đây khi người dùng chọn.
CREATE TABLE trip_checklist_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    kind VARCHAR(8) NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(16),
    -- Chỉ dùng cho TODO, ví dụ check-in online trước ngày bay.
    due_date DATE,
    is_done BOOLEAN NOT NULL DEFAULT false,
    source VARCHAR(10) NOT NULL DEFAULT 'USER',
    -- Vì sao mục này được gợi ý, ví dụ "Ngày 2 dự báo mưa 70%".
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

-- Không thêm trùng một mục hai lần (so tên không phân biệt hoa thường).
CREATE UNIQUE INDEX uq_checklist_trip_kind_title ON trip_checklist_items (trip_id, kind, lower(title));
CREATE INDEX idx_checklist_trip_order ON trip_checklist_items (trip_id, kind, order_index);
