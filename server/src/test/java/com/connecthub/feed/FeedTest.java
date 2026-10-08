package com.connecthub.feed;

import com.connecthub.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class FeedTest extends AuthTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void feedContainsOnlyMyCirclesAndEngagementBoostsRank() throws Exception {
        Session me = register();
        Session other = register();
        String mine = createCircle(me, "Feed Mine", false, null);
        String theirs = createCircle(other, "Feed Theirs", false, null);

        String quietNew = createPost(me, circleId(mine), "quiet new").get("id").asText();
        String livelyOld = createPost(me, circleId(mine), "lively old").get("id").asText();
        createPost(other, circleId(theirs), "not in my circles");

        // lively post is 3h old but has lots of engagement; quiet post is 1h old with none
        jdbc.update("update posts set created_at = now() - interval '3 hours', reaction_count = 12, comment_count = 6 where id = ?::uuid", livelyOld);
        jdbc.update("update posts set created_at = now() - interval '1 hour' where id = ?::uuid", quietNew);

        JsonNode items = json.readTree(mvc.perform(get("/api/v1/feed").header("Authorization", me.bearer()))
                .andReturn().getResponse().getContentAsString()).at("/data/items");
        List<String> ids = new ArrayList<>();
        items.forEach(i -> ids.add(i.get("id").asText()));
        assertThat(ids).containsExactly(livelyOld, quietNew);
    }

    @Test
    void cursorPaginationWalksTheWholeFeedWithoutDuplicates() throws Exception {
        Session me = register();
        String slug = createCircle(me, "Paged Feed", false, null);
        String cid = circleId(slug);
        Set<String> created = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            created.add(createPost(me, cid, "post " + i).get("id").asText());
            rateLimiter.reset(); // posting limit is 10 / 15 min
        }

        Set<String> seen = new HashSet<>();
        String cursor = null;
        int pages = 0;
        do {
            var req = get("/api/v1/feed").param("limit", "3").header("Authorization", me.bearer());
            if (cursor != null) req.param("cursor", cursor);
            JsonNode data = json.readTree(mvc.perform(req).andReturn().getResponse().getContentAsString()).get("data");
            data.get("items").forEach(i -> assertThat(seen.add(i.get("id").asText())).isTrue());
            cursor = data.get("nextCursor").isNull() ? null : data.get("nextCursor").asText();
            pages++;
        } while (cursor != null && pages < 10);

        assertThat(seen).isEqualTo(created);
        assertThat(pages).isEqualTo(3);
    }
}
