package com.connecthub.seed;

import com.connecthub.circle.Circle;
import com.connecthub.circle.CircleMember;
import com.connecthub.circle.CircleMemberRepository;
import com.connecthub.circle.CircleRepository;
import com.connecthub.circle.CircleRole;
import com.connecthub.circle.MemberStatus;
import com.connecthub.comment.Comment;
import com.connecthub.comment.CommentRepository;
import com.connecthub.post.Post;
import com.connecthub.post.PostImage;
import com.connecthub.post.PostRepository;
import com.connecthub.post.ReactionRepository;
import com.connecthub.post.ReactionType;
import com.connecthub.user.AuthProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.connecthub.user.Interest;
import com.connecthub.user.InterestRepository;
import com.connecthub.user.User;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Demo content for local development (profile {@code dev}) and public demos (profile {@code demo},
 * e.g. {@code SPRING_PROFILES_ACTIVE=prod,demo}). Each section is idempotent
 * (skipped when its table already has rows). Every account's password is {@value #PASSWORD}.
 * Also seeds sample DMs and notifications for the demo accounts.
 */
@Slf4j
@Component
@Profile({"dev", "demo"})
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    public static final String PASSWORD = "Password123";

    /** username, display name, bio, location */
    private static final String[][] PEOPLE = {
            {"maya", "Maya Okafor", "Film photographer. Chasing golden hour on Portra 400.", "Lagos"},
            {"theo", "Theo Lindqvist", "Synths, field recordings and long walks.", "Stockholm"},
            {"priya", "Priya Raman", "Reading my way through every Booker shortlist.", "Bengaluru"},
            {"sam", "Sam Duarte", "Home cook. Sourdough evangelist. Will share starter.", "Lisbon"},
            {"aiko", "Aiko Tanaka", "Product designer who sketches on trains.", "Osaka"},
            {"jonah", "Jonah Brooks", "Trail runner. Slow coffee. Fast miles.", "Denver"},
            {"lena", "Lena Fischer", "Astrophysics PhD — ask me about exoplanets.", "Heidelberg"},
            {"omar", "Omar Haddad", "Indie game dev. Pixel art at 2am.", "Amman"},
            {"zoe", "Zoë Martin", "Vintage fashion & mending what others throw away.", "Montréal"},
            {"kofi", "Kofi Mensah", "Founder, tinkerer, occasional poet.", "Accra"},
            {"ines", "Inès Moreau", "Watercolour every morning before email.", "Lyon"},
            {"diego", "Diego Rivera Soto", "Football tactics nerd. Barça through and through.", "Barcelona"},
            {"hana", "Hana Kim", "Therapist-in-training. Gentle reminders to rest.", "Seoul"},
            {"liam", "Liam O'Connor", "Birder with a long lens and more patience.", "Galway"},
            {"sofia", "Sofía Herrera", "Backpacking South America, one bus at a time.", "Bogotá"},
            {"noah", "Noah Weiss", "Frontend dev. Accessibility is not optional.", "Berlin"},
            {"amara", "Amara Nwosu", "Short stories, long letters.", "London"},
            {"ravi", "Ravi Patel", "Specialty coffee roaster. V60 or nothing.", "Melbourne"},
            {"elena", "Elena Petrova", "Cinephile. 400 films a year and counting.", "Sofia"},
            {"felix", "Felix Wagner", "My dog has a better social life than me.", "Vienna"},
    };

    /** slug, name, description, category slug, private?, creator username */
    private static final String[][] CIRCLES = {
            {"golden-hour-club", "Golden Hour Club", "Film and digital shooters chasing the best light of the day. Share a frame, say what you'd change.", "photography", "false", "maya"},
            {"slow-readers-society", "Slow Readers Society", "One chapter at a time. Monthly picks, no spoilers past the bookmark.", "books", "false", "priya"},
            {"sourdough-and-co", "Sourdough & Co", "Starters, crumb shots and gentle troubleshooting for wild-yeast bakers.", "cooking", "false", "sam"},
            {"late-night-synths", "Late Night Synths", "Patches, field recordings and works-in-progress. Headphones recommended.", "music", "false", "theo"},
            {"trail-miles", "Trail Miles", "Routes, elevation and post-run snacks. All paces welcome.", "fitness", "false", "jonah"},
            {"exoplanet-watch", "Exoplanet Watch", "New worlds, transit curves and the occasional wild hypothesis.", "science", "false", "lena"},
            {"pixel-workshop", "Pixel Workshop", "Indie devs and pixel artists sharing devlogs and sprite sheets.", "gaming", "false", "omar"},
            {"mend-and-make", "Mend & Make", "Visible mending, thrift flips and slow fashion. Show your patches.", "fashion", "true", "zoe"},
            {"founders-quiet-room", "Founders' Quiet Room", "A calm, private space for early-stage founders to think out loud.", "startups", "true", "kofi"},
            {"morning-watercolour", "Morning Watercolour", "Ten minutes of paint before the inbox. Loose, imperfect, daily.", "art", "false", "ines"},
            {"gentle-minds", "Gentle Minds", "Kind check-ins and small wins. Not a substitute for professional care.", "mental-health", "true", "hana"},
            {"third-wave-coffee", "Third Wave Coffee", "Recipes, grinders, beans worth the price. V60 debates encouraged.", "coffee", "false", "ravi"},
    };

    private final UserRepository users;
    private final InterestRepository interests;
    private final CircleRepository circles;
    private final CircleMemberRepository members;
    private final PostRepository posts;
    private final ReactionRepository reactions;
    private final CommentRepository comments;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        seedCircles();
        seedPosts();
        seedMoments();
        seedConversations();
        seedNotifications();
    }

    private void seedUsers() {
        if (users.count() > 0) {
            log.info("DataSeeder: users exist, skipping");
            return;
        }
        Random random = new Random(42);
        List<Interest> allInterests = interests.findAll();
        String hash = passwordEncoder.encode(PASSWORD); // BCrypt is slow; one hash shared by all demo users

        List<User> created = new ArrayList<>();
        for (String[] p : PEOPLE) {
            User u = new User();
            u.setEmail(p[0] + "@connecthub.dev");
            u.setPasswordHash(hash);
            u.setProvider(AuthProvider.local);
            u.setUsername(p[0]);
            u.setDisplayName(p[1]);
            u.setBio(p[2]);
            u.setLocation(p[3]);
            u.setAvatarUrl("https://api.dicebear.com/9.x/notionists/svg?seed=" + p[0] + "&backgroundColor=f3eee6");
            u.setOnboarded(true);
            List<Interest> shuffled = new ArrayList<>(allInterests);
            java.util.Collections.shuffle(shuffled, random);
            u.setInterests(new HashSet<>(shuffled.subList(0, 3 + random.nextInt(3))));
            created.add(u);
        }
        users.saveAll(created);
        log.info("DataSeeder: created {} users (e.g. maya@connecthub.dev / {})", created.size(), PASSWORD);
    }

    private void seedCircles() {
        if (circles.count() > 0) {
            log.info("DataSeeder: circles exist, skipping");
            return;
        }
        Map<String, User> byUsername = users.findAll().stream()
                .filter(u -> u.getUsername() != null)
                .collect(Collectors.toMap(User::getUsername, Function.identity()));
        Map<String, Interest> categories = interests.findAll().stream()
                .collect(Collectors.toMap(Interest::getSlug, Function.identity()));
        List<User> people = new ArrayList<>(java.util.Arrays.stream(PEOPLE).map(p -> byUsername.get(p[0])).toList());
        if (people.contains(null)) {
            log.warn("DataSeeder: demo users missing, skipping circles");
            return;
        }
        Random random = new Random(7);
        for (String[] c : CIRCLES) {
            User creator = byUsername.get(c[5]);
            Circle circle = new Circle();
            circle.setSlug(c[0]);
            circle.setName(c[1]);
            circle.setDescription(c[2]);
            circle.setCategory(categories.get(c[3]));
            circle.setPrivateCircle(Boolean.parseBoolean(c[4]));
            circle.setCreatedBy(creator);
            circles.save(circle);
            members.save(new CircleMember(circle, creator, CircleRole.admin, MemberStatus.active));

            List<User> others = new ArrayList<>(people);
            others.remove(creator);
            Collections.shuffle(others, random);
            int active = 4 + random.nextInt(9);
            for (User u : others.subList(0, active)) {
                members.save(new CircleMember(circle, u, CircleRole.member, MemberStatus.active));
            }
            if (circle.isPrivateCircle()) {
                for (User u : others.subList(active, active + 2)) {
                    members.save(new CircleMember(circle, u, CircleRole.member, MemberStatus.pending));
                }
            }
            circle.setMemberCount(active + 1);
        }
        log.info("DataSeeder: created {} circles", CIRCLES.length);
    }

    /**
     * ~80 posts spread over the last 10 days, with reactions from circle members, comments and
     * replies. Some posts carry photos from picsum.photos (dev only; external).
     */
    private void seedPosts() {
        if (posts.count() > 0) {
            log.info("DataSeeder: posts exist, skipping");
            return;
        }
        Random random = new Random(11);
        ReactionType[] types = ReactionType.values();
        int created = 0;
        for (Circle circle : circles.findAll()) {
            List<String> texts = SeedPosts.BY_CIRCLE.get(circle.getSlug());
            if (texts == null) {
                continue;
            }
            List<User> active = members.findByCircleIdAndStatus(circle.getId(), MemberStatus.active, PageRequest.of(0, 200))
                    .map(CircleMember::getUser).getContent();
            for (String text : texts) {
                User author = active.get(random.nextInt(active.size()));
                Post post = new Post();
                post.setAuthor(author);
                post.setCircle(circle);
                post.setContent(text);
                if (random.nextInt(5) == 0) {
                    int n = 1 + random.nextInt(random.nextBoolean() ? 1 : 3);
                    for (int i = 0; i < n; i++) {
                        boolean portrait = random.nextBoolean();
                        int w = portrait ? 900 : 1200, h = portrait ? 1200 : 800;
                        String seed = circle.getSlug() + "-" + created + "-" + i;
                        post.getImages().add(new PostImage(post, "https://picsum.photos/seed/" + seed + "/" + w + "/" + h, w, h, i));
                    }
                }
                posts.saveAndFlush(post); // flush: the JdbcTemplate update below bypasses Hibernate
                // Backdate: spread over the last 10 days.
                jdbc.update("UPDATE posts SET created_at = now() - make_interval(mins => ?) WHERE id = ?",
                        random.nextInt(60 * 24 * 10), post.getId());

                List<User> others = new ArrayList<>(active);
                Collections.shuffle(others, random);
                int reactors = random.nextInt(Math.min(others.size(), 9));
                for (User u : others.subList(0, reactors)) {
                    // Each circle leans towards a couple of vibes so the rings look distinct.
                    ReactionType t = random.nextInt(3) == 0 ? types[random.nextInt(types.length)]
                            : types[(Math.abs(circle.getSlug().hashCode()) + random.nextInt(2)) % types.length];
                    reactions.upsert(post.getId(), u.getId(), t.name());
                }
                int commenters = random.nextInt(4);
                for (int i = 0; i < commenters; i++) {
                    Comment c = new Comment();
                    c.setPost(post);
                    c.setAuthor(others.get(random.nextInt(others.size())));
                    c.setContent(SeedPosts.COMMENTS.get(random.nextInt(SeedPosts.COMMENTS.size())));
                    comments.save(c);
                    if (random.nextInt(3) == 0) {
                        Comment reply = new Comment();
                        reply.setPost(post);
                        reply.setParent(c);
                        reply.setAuthor(author);
                        reply.setContent(SeedPosts.REPLIES.get(random.nextInt(SeedPosts.REPLIES.size())));
                        comments.save(reply);
                    }
                }
                created++;
            }
        }
        comments.flush();
        // Comments follow their post in time; then recompute the denormalised counters.
        jdbc.update("""
                UPDATE comments c SET created_at = p.created_at + make_interval(mins => 5 + (abs(hashtext(c.id::text)) % 600))
                FROM posts p WHERE p.id = c.post_id""");
        jdbc.update("""
                UPDATE comments r SET created_at = c.created_at + interval '7 minutes'
                FROM comments c WHERE r.parent_id = c.id""");
        jdbc.update("""
                UPDATE posts p SET
                  reaction_count = (SELECT count(*) FROM reactions r WHERE r.post_id = p.id),
                  comment_count  = (SELECT count(*) FROM comments c WHERE c.post_id = p.id)""");
        // Each circle pins its most-discussed post.
        jdbc.update("""
                UPDATE circles ci SET pinned_post_id = (
                  SELECT p.id FROM posts p WHERE p.circle_id = ci.id ORDER BY p.comment_count DESC, p.created_at DESC LIMIT 1)
                WHERE ci.slug IN ('golden-hour-club', 'sourdough-and-co', 'founders-quiet-room')""");
        log.info("DataSeeder: created {} posts with reactions and comments", created);
    }

    private static final String[] MOMENT_TEXTS = {
            "First coffee, first light ☕", "Tiny win: inbox zero.", "Rain on the window, synths in my ears.",
            "Ran further than ever today 🏃", "Bread came out perfect. Not sharing.", "Clear skies — Jupiter is out tonight!",
            "Finished the book. Need a minute.", "New sprite, who dis", "Mended my favourite sweater 🧵",
            "Painting before the world wakes up.", "Grateful for this circle today.", "Trying a new bean — fruity!",
    };
    private static final String[] BACKGROUNDS = {"sunset", "ocean", "forest", "dusk", "ember", "plum"};

    /** 22 live moments from the last 23h (some already viewed) + 8 archived into Rewind (5 of them Maya's). */
    private void seedMoments() {
        if (jdbc.queryForObject("SELECT count(*) FROM moments", Long.class) > 0) {
            log.info("DataSeeder: moments exist, skipping");
            return;
        }
        Random random = new Random(23);
        List<User> people = new ArrayList<>(users.findAll().stream().filter(u -> u.getUsername() != null && u.getEmail().endsWith("@connecthub.dev")).toList());
        User maya = people.stream().filter(u -> "maya".equals(u.getUsername())).findFirst().orElse(people.get(0));
        for (int i = 0; i < 30; i++) {
            boolean archived = i >= 22;
            User author = archived && i < 27 ? maya : people.get(random.nextInt(people.size()));
            int ageMinutes = archived ? (60 * 24 * 2 + random.nextInt(60 * 24 * 5)) : (10 + random.nextInt(60 * 23));
            boolean image = random.nextInt(5) < 2;
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO moments (id, author_id, kind, image_url, image_width, image_height, text, background,
                                         created_at, expires_at, archived)
                    VALUES (?, ?, CAST(? AS moment_kind), ?, ?, ?, ?, ?,
                            now() - make_interval(mins => ?), now() - make_interval(mins => ?) + interval '24 hours', ?)""",
                    id, author.getId(), image ? "image" : "text",
                    image ? "https://picsum.photos/seed/moment-" + i + "/1080/1920" : null, image ? 1080 : null, image ? 1920 : null,
                    image ? null : MOMENT_TEXTS[random.nextInt(MOMENT_TEXTS.length)],
                    image ? null : BACKGROUNDS[random.nextInt(BACKGROUNDS.length)],
                    ageMinutes, ageMinutes, archived);
            // A few circle-mates have already watched it.
            jdbc.update("""
                    INSERT INTO moment_views (moment_id, viewer_id)
                    SELECT DISTINCT ?, b.user_id FROM circle_members a JOIN circle_members b ON a.circle_id = b.circle_id
                    WHERE a.user_id = ? AND b.user_id <> ? AND a.status = 'active' AND b.status = 'active'
                      AND random() < 0.35
                    ON CONFLICT DO NOTHING""", id, author.getId(), author.getId());
        }
        jdbc.update("UPDATE moments m SET view_count = (SELECT count(*) FROM moment_views v WHERE v.moment_id = m.id)");
        log.info("DataSeeder: created 30 moments (22 live, 8 in Rewind)");
    }

    /** partner username, then alternating lines starting with the partner. Last partner line stays unread. */
    private static final String[][] CHATS = {
            {"ines", "Your golden hour shot this week was gorgeous", "Thank you!! It was pure luck with the clouds",
                    "Want to do a photo walk on Saturday?", "Yes! Morning or evening?", "Evening — golden hour obviously 😄"},
            {"sam", "Did you ever fix your flat loaves?", "Shaping was the problem. Watched 4 videos and it clicked",
                    "Bring one to the cupping on Sunday?"},
            {"lena", "Saw Saturn last night thanks to your post", "Amazing! Did you see the rings?",
                    "Barely, but yes!! Binoculars next time", "There's a meteor shower on the 21st 🌠"},
    };

    private void seedConversations() {
        if (jdbc.queryForObject("SELECT count(*) FROM conversations", Long.class) > 0) {
            log.info("DataSeeder: conversations exist, skipping");
            return;
        }
        User maya = users.findByUsername("maya").orElse(null);
        if (maya == null) return;
        int c = 0;
        for (String[] chat : CHATS) {
            User other = users.findByUsername(chat[0]).orElse(null);
            if (other == null) continue;
            UUID conversationId = UUID.randomUUID();
            String key = maya.getId().compareTo(other.getId()) < 0 ? maya.getId() + ":" + other.getId() : other.getId() + ":" + maya.getId();
            jdbc.update("INSERT INTO conversations (id, direct_key) VALUES (?, ?)", conversationId, key);
            jdbc.update("INSERT INTO conversation_participants (conversation_id, user_id) VALUES (?, ?), (?, ?)",
                    conversationId, maya.getId(), conversationId, other.getId());
            int lines = chat.length - 1;
            for (int i = 0; i < lines; i++) {
                boolean fromOther = i % 2 == 0;
                boolean last = i == lines - 1;
                int minutesAgo = (c + 1) * 90 + (lines - i) * 7;
                // Everything is read except a final message from the other person.
                jdbc.update("""
                        INSERT INTO messages (conversation_id, sender_id, content, created_at, read_at)
                        VALUES (?, ?, ?, now() - make_interval(mins => ?), CASE WHEN ? THEN NULL ELSE now() END)""",
                        conversationId, fromOther ? other.getId() : maya.getId(), chat[i + 1], minutesAgo, last && fromOther);
            }
            jdbc.update("UPDATE conversations c SET last_message_at = (SELECT max(created_at) FROM messages m WHERE m.conversation_id = c.id) WHERE id = ?", conversationId);
            c++;
        }
        log.info("DataSeeder: created {} conversations for maya", c);
    }

    /** Notifications for maya derived from real activity on her posts (a few already read). */
    private void seedNotifications() {
        if (jdbc.queryForObject("SELECT count(*) FROM notifications", Long.class) > 0) {
            log.info("DataSeeder: notifications exist, skipping");
            return;
        }
        int n = jdbc.update("""
                INSERT INTO notifications (recipient_id, actor_id, type, post_id, created_at, read)
                SELECT p.author_id, r.user_id, 'reaction', p.id, r.created_at + interval '3 minutes', random() < 0.4
                FROM reactions r JOIN posts p ON p.id = r.post_id JOIN users u ON u.id = p.author_id
                WHERE u.username = 'maya' AND r.user_id <> p.author_id
                LIMIT 8""");
        n += jdbc.update("""
                INSERT INTO notifications (recipient_id, actor_id, type, post_id, comment_id, created_at, read)
                SELECT p.author_id, c.author_id, 'comment', p.id, c.id, c.created_at, random() < 0.4
                FROM comments c JOIN posts p ON p.id = c.post_id JOIN users u ON u.id = p.author_id
                WHERE u.username = 'maya' AND c.author_id <> p.author_id AND c.parent_id IS NULL
                LIMIT 6""");
        log.info("DataSeeder: created {} notifications", n);
    }
}
