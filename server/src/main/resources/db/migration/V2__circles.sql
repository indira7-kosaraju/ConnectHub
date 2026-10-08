-- ConnectHub V2: circles and memberships.

CREATE TYPE circle_role AS ENUM ('admin', 'member');
CREATE TYPE member_status AS ENUM ('active', 'pending');

CREATE TABLE circles (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           VARCHAR(60)  NOT NULL UNIQUE,
    name           VARCHAR(50)  NOT NULL,
    description    VARCHAR(300),
    cover_url      VARCHAR(512),
    category_id    UUID NOT NULL REFERENCES interests (id) ON DELETE RESTRICT,
    is_private     BOOLEAN NOT NULL DEFAULT FALSE,
    max_members    INT NOT NULL DEFAULT 150,
    member_count   INT NOT NULL DEFAULT 0,          -- active members; maintained under a row lock
    pinned_post_id UUID,                            -- FK added in V3 once posts exist
    created_by     UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT circles_max_members_range CHECK (max_members BETWEEN 2 AND 150),
    CONSTRAINT circles_member_count_range CHECK (member_count >= 0 AND member_count <= max_members),
    CONSTRAINT circles_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);
CREATE INDEX circles_category_idx ON circles (category_id);
CREATE INDEX circles_created_by_idx ON circles (created_by);
CREATE INDEX circles_member_count_idx ON circles (member_count DESC);
CREATE TRIGGER circles_updated_at BEFORE UPDATE ON circles FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE circle_members (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circle_id  UUID NOT NULL REFERENCES circles (id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       circle_role NOT NULL DEFAULT 'member',
    status     member_status NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT circle_members_uk UNIQUE (circle_id, user_id)
);
CREATE INDEX circle_members_circle_idx ON circle_members (circle_id, status, created_at);
CREATE INDEX circle_members_user_idx ON circle_members (user_id, status);
CREATE TRIGGER circle_members_updated_at BEFORE UPDATE ON circle_members FOR EACH ROW EXECUTE FUNCTION set_updated_at();
