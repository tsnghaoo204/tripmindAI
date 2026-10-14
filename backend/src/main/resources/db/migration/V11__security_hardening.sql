-- V11: Thu hồi JWT khi đổi mật khẩu / khoá tài khoản. Phiếu mang claim "tv", khác cột này thì bị từ chối.
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INT NOT NULL DEFAULT 0;
