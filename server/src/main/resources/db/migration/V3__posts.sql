-- ConnectHub V3: posts, images, vibe reactions, comments, saves, reports.

CREATE TYPE reaction_type AS ENUM ('spark', 'deep', 'funny', 'support', 'mindblown');
CREATE TYPE report_target AS ENUM ('post', 'comment', 'user');
CREATE TYPE report_reason AS ENUM ('spam', 'harassment', 'hate', 'nudity', 'violence', 'misinformation', 'self_harm', 'other');
CREATE TYPE report_status AS ENUM ('open', 'reviewed', 'dismissed');

CREATE TABLE posts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    circle_id      UUID NOT NULL REFERENCES circles (id) ON DELETE CASCADE,
    content        VARCHAR(500),
    edited_at      TIMESTAMPTZ,
    -- Denormalised engagement used by feed ranking; updated atomically.
    reaction_count INT NOT NULL DEFAULT 0 CHECK (reaction_count >= 0),
    comment_count  INT NOT NULL DEFAULT 0 CHECK (comment_count >= 0),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX posts_circle_created_idx ON posts (circle_id, created_at DESC, id DESC);
CREATE INDEX posts_author_created_idx ON posts (author_id, created_at DESC, id DESC);
CREATE TRIGGER posts_updated_at BEFORE UPDATE ON posts FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE circles
    ADD CONSTRAINT circles_pinned_post_fk FOREIGN KEY (pinned_post_id) REFERENCES posts (id) ON DELETE SET NULL;
CREATE INDEX circles_pinned_post_idx ON circles (pinned_post_id);

CREATE TABLE post_images (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    url        VARCHAR(512) NOT NULL,
    width      INT NOT NULL,
    height     INT NOT NULL,
    position   SMALLINT NOT NULL CHECK (position BETWEEN 0 AND 3),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT post_images_position_uk UNIQUE (post_id, position)
);
CREATE INDEX post_images_post_idx ON post_images (post_id);
CREATE TRIGGER post_images_updated_at BEFORE UPDATE ON post_images FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE reactions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       reaction_type NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reactions_user_post_uk UNIQUE (user_id, post_id)
);
CREATE INDEX reactions_post_idx ON reactions (post_id, type);
CREATE INDEX reactions_user_idx ON reactions (user_id);
CREATE TRIGGER reactions_updated_at BEFORE UPDATE ON reactions FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    author_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    parent_id  UUID REFERENCES comments (id) ON DELETE CASCADE,   -- one level of nesting
    content    VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX comments_post_created_idx ON comments (post_id, created_at, id) WHERE parent_id IS NULL;
CREATE INDEX comments_parent_idx ON comments (parent_id, created_at);
CREATE INDEX comments_author_idx ON comments (author_id);
CREATE TRIGGER comments_updated_at BEFORE UPDATE ON comments FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE comment_reactions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       reaction_type NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT comment_reactions_user_comment_uk UNIQUE (user_id, comment_id)
);
CREATE INDEX comment_reactions_comment_idx ON comment_reactions (comment_id, type);
CREATE INDEX comment_reactions_user_idx ON comment_reactions (user_id);
CREATE TRIGGER comment_reactions_updated_at BEFORE UPDATE ON comment_reactions FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE saved_posts (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    post_id    UUID NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT saved_posts_user_post_uk UNIQUE (user_id, post_id)
);
CREATE INDEX saved_posts_user_created_idx ON saved_posts (user_id, created_at DESC, id DESC);
CREATE INDEX saved_posts_post_idx ON saved_posts (post_id);
CREATE TRIGGER saved_posts_updated_at BEFORE UPDATE ON saved_posts FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Reports are created from posts now; user/comment reporting UI lands with moderation (Step 7).
CREATE TABLE reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type report_target NOT NULL,
    target_id   UUID NOT NULL,                     -- polymorphic: post / comment / user id
    reason      report_reason NOT NULL,
    details     VARCHAR(500),
    status      report_status NOT NULL DEFAULT 'open',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reports_once_per_target UNIQUE (reporter_id, target_type, target_id)
);
CREATE INDEX reports_reporter_idx ON reports (reporter_id);
CREATE INDEX reports_target_idx ON reports (target_type, target_id);
CREATE INDEX reports_status_idx ON reports (status, created_at);
CREATE TRIGGER reports_updated_at BEFORE UPDATE ON reports FOR EACH ROW EXECUTE FUNCTION set_updated_at();
