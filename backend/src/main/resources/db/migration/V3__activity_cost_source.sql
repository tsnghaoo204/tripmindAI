-- V3: ghi nguồn của chi phí ước tính để giao diện và AI nói rõ con số lấy từ đâu.
--   USER        người dùng tự nhập hoặc đã sửa con số gợi ý
--   PRICE_LEVEL gợi ý từ mức giá Google (price_level) × số người, người dùng giữ nguyên
--   AI          trợ lý đặt khi dựng đề xuất hoặc sinh lịch trình
-- NULL khi hoạt động chưa có chi phí ước tính.
ALTER TABLE activities ADD COLUMN estimated_cost_source VARCHAR(12);
ALTER TABLE activities ADD CONSTRAINT chk_activities_cost_source
    CHECK (estimated_cost_source IS NULL OR estimated_cost_source IN ('USER', 'PRICE_LEVEL', 'AI'));
