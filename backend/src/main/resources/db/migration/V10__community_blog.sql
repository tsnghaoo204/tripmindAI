-- V10: Cộng đồng Blog, Đánh giá chuyến đi & Trích xuất Dataset tri thức cho AI

CREATE TABLE IF NOT EXISTS blog_posts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    trip_id BIGINT,
    destination_name VARCHAR(160) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    photo_urls JSONB NOT NULL DEFAULT '[]',
    upvote_count INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    converted_to_knowledge_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_blog_post_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_post_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL,
    CONSTRAINT fk_blog_post_knowledge FOREIGN KEY (converted_to_knowledge_id) REFERENCES destination_knowledge(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS blog_comments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    upvote_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_blog_comment_post FOREIGN KEY (post_id) REFERENCES blog_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS blog_post_upvotes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_blog_post_upvote UNIQUE (user_id, post_id),
    CONSTRAINT fk_blog_post_upvote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_post_upvote_post FOREIGN KEY (post_id) REFERENCES blog_posts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS blog_comment_upvotes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_blog_comment_upvote UNIQUE (user_id, comment_id),
    CONSTRAINT fk_blog_comment_upvote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_comment_upvote_comment FOREIGN KEY (comment_id) REFERENCES blog_comments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_blog_posts_dest ON blog_posts(destination_name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_blog_posts_user ON blog_posts(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_blog_posts_upvotes ON blog_posts(upvote_count DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_blog_comments_post ON blog_comments(post_id, created_at ASC);
