-- V9: Dataset tri thức du lịch & Mẹo thực tế từ cộng đồng (Facebook, TikTok, diễn đàn) cho AI
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

