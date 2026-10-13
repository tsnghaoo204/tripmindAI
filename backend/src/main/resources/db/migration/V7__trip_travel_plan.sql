-- V7: thông tin hành trình (xuất phát, giờ đến/về, phương tiện, nơi ở) để AI xếp lịch sát thực tế.
ALTER TABLE trips ADD COLUMN travel_plan JSONB NOT NULL DEFAULT '{}';
