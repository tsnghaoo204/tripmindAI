-- V4: tệp đính kèm của tin nhắn trợ lý.
-- Đề xuất, ứng viên địa điểm (propose_places) và gợi ý checklist (suggest_checklist) không
-- ghi vào bảng nghiệp vụ nào cho tới khi người dùng bấm chọn. Chúng đi theo tin nhắn để mở
-- lại hội thoại vẫn thấy đúng những gì trợ lý đã đưa ra.
ALTER TABLE messages ADD COLUMN attachments_json JSONB;
