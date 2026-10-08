-- ConnectHub V5: PostgreSQL full-text search (generated tsvector columns + GIN indexes).
-- Names use the 'simple' configuration (no stemming: "Maya" must match "maya"); free text uses
-- 'english' so "baking" finds "bake". Queries use prefix matching (word:*) for search-as-you-type.

ALTER TABLE posts
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (to_tsvector('english', coalesce(content, ''))) STORED;
CREATE INDEX posts_search_idx ON posts USING GIN (search_vector);

ALTER TABLE users
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('simple', coalesce(username, '')), 'A') ||
            setweight(to_tsvector('simple', coalesce(display_name, '')), 'A') ||
            setweight(to_tsvector('english', coalesce(bio, '')), 'C')
        ) STORED;
CREATE INDEX users_search_idx ON users USING GIN (search_vector);

ALTER TABLE circles
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('simple', coalesce(name, '')), 'A') ||
            setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
            setweight(to_tsvector('english', coalesce(description, '')), 'B')
        ) STORED;
CREATE INDEX circles_search_idx ON circles USING GIN (search_vector);
