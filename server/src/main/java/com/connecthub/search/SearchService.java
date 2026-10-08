package com.connecthub.search;

import com.connecthub.circle.Circle;
import com.connecthub.circle.CircleMapper;
import com.connecthub.circle.CircleRepository;
import com.connecthub.circle.CircleService;
import com.connecthub.moderation.BlockService;
import com.connecthub.post.Post;
import com.connecthub.post.PostAssembler;
import com.connecthub.post.PostRepository;
import com.connecthub.search.dto.SearchResults;
import com.connecthub.user.User;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Global search across people, circles and posts (PostgreSQL full-text, ranked). */
@Service
@RequiredArgsConstructor
public class SearchService {

    public enum Scope { all, people, circles, posts }

    private final SearchQueries queries;
    private final UserRepository users;
    private final CircleRepository circles;
    private final CircleService circleService;
    private final PostRepository posts;
    private final PostAssembler postAssembler;
    private final CircleMapper mapper;
    private final BlockService blocks;

    @Transactional(readOnly = true)
    public SearchResults search(String raw, Scope scope, int limit, UUID viewerId) {
        String q = SearchQueries.toPrefixQuery(raw);
        if (q == null) {
            return SearchResults.EMPTY;
        }
        int n = Math.max(1, Math.min(limit, 30));
        Collection<UUID> hidden = blocks.hiddenForQuery(viewerId);

        var people = wants(scope, Scope.people)
                ? inOrder(queries.people(q, hidden, n), users::findAllById, User::getId).stream().map(mapper::toUserSummary).toList()
                : List.<com.connecthub.user.dto.UserSummary>of();
        var circleList = wants(scope, Scope.circles)
                ? circleService.withViewer(inOrder(queries.circles(q, null, n, 0), circles::findAllByIdIn, Circle::getId)
                        .stream().map(mapper::toSummary).toList(), viewerId)
                : List.<com.connecthub.circle.dto.CircleSummary>of();
        var postList = wants(scope, Scope.posts)
                ? postAssembler.toDtos(inOrder(queries.posts(q, viewerId, hidden, n), posts::findAllByIdIn, Post::getId), viewerId)
                : List.<com.connecthub.post.dto.PostDto>of();
        return new SearchResults(people, circleList, postList);
    }

    private static boolean wants(Scope scope, Scope part) {
        return scope == Scope.all || scope == part;
    }

    /** Load entities by id and return them in the ranked order of {@code ids}. */
    private static <T> List<T> inOrder(List<UUID> ids, Function<List<UUID>, ? extends Iterable<T>> loader, Function<T, UUID> idOf) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, T> byId = new java.util.HashMap<>();
        loader.apply(ids).forEach(t -> byId.put(idOf.apply(t), t));
        return ids.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
