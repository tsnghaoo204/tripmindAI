-- V12: chỉ mục cho các khoá ngoại hay được tra / xoá dây chuyền, và tra tri thức theo điểm đến.

CREATE INDEX IF NOT EXISTS idx_activities_from_proposal ON activities (from_proposal_id) WHERE from_proposal_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_journal_entries_activity ON journal_entries (activity_id) WHERE activity_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_photos_activity ON photos (activity_id) WHERE activity_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_activity_alternatives_place ON activity_alternatives (place_id) WHERE place_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_blog_post_upvotes_post ON blog_post_upvotes (post_id);
CREATE INDEX IF NOT EXISTS idx_blog_comment_upvotes_comment ON blog_comment_upvotes (comment_id);
CREATE INDEX IF NOT EXISTS idx_ai_tool_executions_trip ON ai_tool_executions (trip_id, created_at DESC);

-- Tri thức: tra theo destination_id, dòng chưa gắn điểm đến thì so đúng tên (LOWER).
CREATE INDEX IF NOT EXISTS idx_destination_knowledge_dest_id ON destination_knowledge (destination_id, is_active);
CREATE INDEX IF NOT EXISTS idx_destination_knowledge_lower_name ON destination_knowledge (LOWER(destination_name)) WHERE destination_id IS NULL;
