-- ConnectHub V4: moments (Step 5), messaging + notifications (Step 6), blocks (Step 7).
-- One migration for the remaining social tables keeps the brief's V1–V5 layout; V5 is search.

-- ------------------------------------------------------------------ moments
CREATE TYPE moment_kind AS ENUM ('image', 'text');

CREATE TABLE moments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind         moment_kind NOT NULL,
    image_url    VARCHAR(512),
    image_width  INT,
    image_height INT,
    text         VARCHAR(200),
    background   VARCHAR(20),                       -- gradient key for text moments
    expires_at   TIMESTAMPTZ NOT NULL,
    archived     BOOLEAN NOT NULL DEFAULT FALSE,    -- set by the expiry job; archived = in the author's Rewind
    view_count   INT NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT moments_shape CHECK (
        (kind = 'image' AND image_url IS NOT NULL AND image_width IS NOT NULL AND image_height IS NOT NULL)
        OR (kind = 'text' AND text IS NOT NULL AND background IS NOT NULL))
);
CREATE INDEX moments_author_created_idx ON moments (author_id, created_at DESC, id DESC);
CREATE INDEX moments_live_idx ON moments (expires_at) WHERE archived = FALSE;
CREATE TRIGGER moments_updated_at BEFORE UPDATE ON moments FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE moment_views (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id  UUID NOT NULL REFERENCES moments (id) ON DELETE CASCADE,
    viewer_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT moment_views_uk UNIQUE (moment_id, viewer_id)
);
CREATE INDEX moment_views_moment_idx ON moment_views (moment_id);
CREATE INDEX moment_views_viewer_idx ON moment_views (viewer_id);
CREATE TRIGGER moment_views_updated_at BEFORE UPDATE ON moment_views FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------------ messaging
CREATE TABLE conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    direct_key      VARCHAR(73) NOT NULL UNIQUE,     -- "<smaller uuid>:<larger uuid>": one 1:1 thread per pair
    last_message_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX conversations_last_message_idx ON conversations (last_message_at DESC);
CREATE TRIGGER conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE conversation_participants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_read_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT conversation_participants_uk UNIQUE (conversation_id, user_id)
);
CREATE INDEX conversation_participants_conversation_idx ON conversation_participants (conversation_id);
CREATE INDEX conversation_participants_user_idx ON conversation_participants (user_id);
CREATE TRIGGER conversation_participants_updated_at BEFORE UPDATE ON conversation_participants FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content         VARCHAR(2000),
    shared_post_id  UUID REFERENCES posts (id) ON DELETE SET NULL,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT messages_not_empty CHECK (content IS NOT NULL OR shared_post_id IS NOT NULL)
);
CREATE INDEX messages_conversation_created_idx ON messages (conversation_id, created_at, id);
CREATE INDEX messages_sender_idx ON messages (sender_id);
CREATE INDEX messages_shared_post_idx ON messages (shared_post_id);
CREATE INDEX messages_unread_idx ON messages (conversation_id) WHERE read_at IS NULL;
CREATE TRIGGER messages_updated_at BEFORE UPDATE ON messages FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------------ notifications
CREATE TYPE notification_type AS ENUM ('reaction', 'comment', 'reply', 'circle_request', 'circle_approved', 'message');

CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    actor_id     UUID REFERENCES users (id) ON DELETE CASCADE,
    type         notification_type NOT NULL,
    post_id      UUID REFERENCES posts (id) ON DELETE CASCADE,
    comment_id   UUID REFERENCES comments (id) ON DELETE CASCADE,
    circle_id    UUID REFERENCES circles (id) ON DELETE CASCADE,
    read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX notifications_recipient_created_idx ON notifications (recipient_id, created_at DESC, id DESC);
CREATE INDEX notifications_unread_idx ON notifications (recipient_id) WHERE read = FALSE;
CREATE INDEX notifications_actor_idx ON notifications (actor_id);
CREATE INDEX notifications_post_idx ON notifications (post_id);
CREATE INDEX notifications_comment_idx ON notifications (comment_id);
CREATE INDEX notifications_circle_idx ON notifications (circle_id);
CREATE TRIGGER notifications_updated_at BEFORE UPDATE ON notifications FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ------------------------------------------------------------------ moderation
CREATE TABLE blocks (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT blocks_uk UNIQUE (blocker_id, blocked_id),
    CONSTRAINT blocks_not_self CHECK (blocker_id <> blocked_id)
);
CREATE INDEX blocks_blocker_idx ON blocks (blocker_id);
CREATE INDEX blocks_blocked_idx ON blocks (blocked_id);
CREATE TRIGGER blocks_updated_at BEFORE UPDATE ON blocks FOR EACH ROW EXECUTE FUNCTION set_updated_at();
