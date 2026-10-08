# Engineering decisions

Decisions made where the brief was ambiguous or where tooling forced a choice. Newest at the bottom of each section.

## Step 1 — Scaffold

| # | Decision | Why |
| --- | --- | --- |
| 1 | **Spring Boot 3.3.13** (latest 3.3.x), springdoc **2.6.0**, MapStruct **1.6.3**, Testcontainers **1.21.4**. | Brief pins Boot 3.3. springdoc 2.6 is the line built for Boot 3.3. Testcontainers ≥ 1.21 is required for Docker Engine 29+, which rejects the older API version 1.20.x negotiates. |
| 2 | Ship the **Maven Wrapper** (`./mvnw`, Maven 3.9.9). | No global Maven needed locally, in CI or in the Docker build. |
| 3 | **React 18.3 + React Router 6.30** kept as specified, even though newer majors exist. | Brief pins both. React Router 6.x has two moderate advisories: (a) SSR hydration, which doesn't apply to a client-only SPA; (b) an open redirect via backslash paths, mitigated by `safeRedirect()` in `lib/utils.ts`, which every user-controlled redirect (e.g. `?next=`) must go through. |
| 4 | **TypeScript 5.9** rather than 7.x. | TS 7 is the new native compiler; 5.9 is the mature, ecosystem-compatible line. |
| 5 | **Tailwind CSS v4** (CSS-first `@theme`) + `tw-animate-css`; shadcn/ui components authored in-repo with `components.json` so `npx shadcn add` works. | v4 is what current shadcn/ui targets. Design tokens live once as CSS variables in `styles/globals.css` and are mapped to utilities (`bg-surface`, `text-muted`, `text-accent-ink`, …). |
| 6 | **Primary button = ink-on-paper; accent button uses dark text on `#E8663D`.** Text-on-light accent uses `--accent-text: #B5461F`. | White on `#E8663D` is 3.3:1, below the 4.5:1 target. Dark ink on the accent is 5.3:1, and `#B5461F` on `#FAF7F2` is 5.1:1. Dark-mode accent `#FF7A4D` is 7.3:1 on its background as specified. |
| 7 | Access token **in memory only** (Zustand, not persisted); refresh token in an **httpOnly cookie**; Axios shares a single in-flight refresh across concurrent 401s. | Keeps the access token out of XSS-readable storage; the page reload path does a silent refresh. |
| 8 | Theme preference persisted in `localStorage` (`connecthub-theme`) and applied by an inline script in `index.html` before first paint. | Avoids a light→dark flash. |
| 9 | Flyway split: **V1 identity** (users, interests, user_interests, refresh_tokens), **V2 circles**, **V3 posts/reactions/comments/saved**, **V4 moments/messaging/notifications/moderation**, **V5 search** (tsvector + GIN). | Matches the brief's V1–V5 and the build order. Each lands with its feature step. |
| 10 | `updated_at` is maintained both by JPA auditing **and** a `set_updated_at()` trigger. | Keeps the column correct for native/bulk SQL updates (scheduler, seeding) too. |
| 11 | Interests are seeded in **V1** (all environments). Users, circles, posts etc. are seeded only by `DataSeeder` under the `dev` profile. | Onboarding needs interests in prod; demo content must not reach prod. |
| 12 | Usernames are stored lowercase, `^[a-z0-9_]{3,30}$`, nullable until onboarding; email uniqueness is case-insensitive (`lower(email)` index). | OAuth sign-ups don't have a username until onboarding. |
| 13 | Refresh tokens stored as **SHA-256 hashes** with rotation (`replaced_by`). | A DB leak doesn't yield usable tokens; reuse of a rotated token can be detected. |
| 14 | Unauthenticated / forbidden responses from the security layer use the same JSON envelope as controllers. | Uniform client error handling. |
| 15 | `/ws/**` is `permitAll` at the HTTP layer; STOMP `CONNECT` frames are authenticated with the JWT by a channel interceptor (Step 6). | SockJS handshakes can't carry an `Authorization` header reliably. |
| 16 | Integration tests use `@Testcontainers(disabledWithoutDocker = true)` and one shared container. | `mvn package` stays green on machines without Docker; CI runners have Docker, so the tests always run there. |
| 17 | Vite 8 (Rolldown) with function-form `manualChunks` splitting react / motion / query vendors. | Rolldown only accepts the function form; vendor splitting helps the Lighthouse ≥ 90 target. |
| 18 | A dev-only `/design` route acts as a living style guide; it isn't registered in production builds. | Lets each step's UI be reviewed against the design system. |

## Step 2: Auth and onboarding

| # | Decision | Why |
| --- | --- | --- |
| 19 | **Same-origin API.** The SPA calls relative `/api/...`; Vite proxies in dev, Vercel rewrites to Render in prod. WebSockets connect directly to Render (`VITE_WS_URL`) and authenticate with the JWT in the STOMP `CONNECT` frame. | A refresh cookie set by `*.onrender.com` and read from `*.vercel.app` is a third-party cookie, which Safari blocks and Chrome restricts. Same-origin keeps it first-party and lets it be `SameSite=Lax`. |
| 20 | Refresh cookie `ch_rt`: httpOnly, `Secure` in prod, `SameSite=Lax`, `Path=/api/v1/auth`. | Sent only to the auth endpoints, never to the rest of the API, which limits its CSRF surface. |
| 21 | Refresh rotation has a **30-second grace window**: presenting a just-rotated token returns a fresh access token without a new cookie. Outside the window, reuse revokes **all** of the user's sessions. | Two tabs refreshing at the same moment must not log the user out, but a replayed stolen token must kill every session. The token row is locked (`SELECT … FOR UPDATE`) during rotation. |
| 22 | Access tokens carry only `sub` (user id) and are trusted for their 15-minute life without a database lookup. | Keeps every request DB-free for auth. Account deletion revokes refresh tokens, so access ends within 15 minutes. |
| 23 | Google OAuth2 is **registered only when `GOOGLE_CLIENT_ID`/`SECRET` are set**, and `/auth/providers` tells the client whether to show the button. | The app boots and works fully without Google credentials (local dev, CI). |
| 24 | OAuth2 authorization requests are stored **server-side in a 10-minute Caffeine cache keyed by `state`**, not in the HTTP session or a serialized cookie. The provider's tokens are discarded (no-op `OAuth2AuthorizedClientRepository`). | Keeps the API sessionless and avoids deserializing attacker-controlled cookie bytes. We only need Google for identity. Single-instance limitation: see #27. |
| 25 | After Google sign-in, the server sets the refresh cookie and redirects to `/auth/callback`, which calls `/auth/refresh`. **Tokens never appear in URLs.** | Avoids leaking tokens through history, logs or Referer headers. |
| 26 | Google accounts are **linked to an existing email/password account only if Google reports `email_verified`**. | Prevents account takeover via an unverified Google email. |
| 27 | Rate limiting (Bucket4j) and OAuth state are **in-memory**. | Fine for a single Render instance. Scaling out would mean Bucket4j's JDBC/Redis backend and a shared state store. |
| 28 | Auth rate limits: login/register **10 per 5 min per IP**, refresh 30/min per IP, uploads 20 per 10 min per user. | Brute-force protection on top of the brief's post and message limits. |
| 29 | Uploads: allow-list JPEG/PNG/WebP/GIF, **≤ 10 MB, ≤ 40 MP** checked before full decode, re-encoded as JPEG via Thumbnailator (strips EXIF/GPS), transparent PNGs flattened on white. **Local-disk fallback** (`/uploads/**`) when Cloudinary isn't configured. | Decompression-bomb and metadata-leak protection. Dev and CI work offline. Render's disk is ephemeral, so prod must set Cloudinary. |
| 30 | Onboarding avatar step is **skippable**; username and ≥ 3 interests are required to finish. The "suggested circles" step is added in Step 3, when circles exist. | A photo shouldn't block joining. Suggestions need the circles model. |
| 31 | Usernames: reserved list (route names like `settings` and `discover`, plus impersonation-prone names like `admin` and `official`), lowercase only, and a DB unique index as the final arbiter for races. | Prevents profile URLs shadowing app routes and confusing handles. |
| 32 | Plain-text user input goes through `TextSanitizer`: OWASP sanitizer with **no allowed elements**, then entities decoded back to characters. React escapes on render. | Strips markup without double-escaping, so `&` stays `&`. |
| 33 | Login for an unknown email still runs a BCrypt comparison. | Equalizes timing so response time doesn't reveal which emails are registered. (Register does reveal it with a 409, a deliberate UX trade-off.) |
| 34 | In the `prod` profile `JWT_SECRET` has no default, and startup fails with a clear message if it's missing. | No accidental deploys with the dev secret. |
| 35 | Postgres enums map to **lowercase Java enum constants** (`AuthProvider.local`) via Hibernate's `PostgreSQLEnumJdbcType`. | Native PG enums, as the brief asks, with no converter layer. The same convention will apply to roles, statuses and reaction types. |
| 36 | OpenAPI: a customizer marks every **response** property as required, and nullable fields carry `@Schema(nullable = true)`. The `{ success, data, error }` envelope is typed by hand on the client. | Generated TS types are exact (`string \| null`, never `string \| undefined`), so strict mode stays honest. |
| 37 | Dev seed avatars use DiceBear (`api.dicebear.com/9.x/notionists`). | Consistent, license-free illustrated avatars without bundling images. |
| 38 | Playwright was installed in Step 2 (needed for Step 8) and used to verify the sign-up → onboarding → home flow at desktop and mobile sizes. | Earlier real-browser verification. The formal e2e suite lands in Step 8. |

## Step 3: Profiles and circles

| # | Decision | Why |
| --- | --- | --- |
| 39 | **Circle categories reuse the interests taxonomy** (`circles.category_id → interests`). | One vocabulary: onboarding interests directly drive circle suggestions and the Discover filters. |
| 40 | Circle URLs are `/c/{slug}`; slugs come from the name (`Slugs.slugify`, deduped with `-2`…) and **never change on rename**. | Shared links keep working. |
| 41 | Member limit is per circle, **2–150, default 150**. It can't be lowered below the current member count. | The brief caps circles at 150. Some groups want to be smaller. |
| 42 | `member_count` is denormalised and only changed while holding a **`SELECT … FOR UPDATE` lock on the circle row**. A DB CHECK enforces `member_count <= max_members`. | Cheap trending and sorting, with no way to overshoot capacity under concurrent joins. |
| 43 | Joining is idempotent. Private circles create a `pending` row; approving re-checks capacity. | Double-taps and retries are safe. A circle can fill while a request is pending. |
| 44 | **Last admin leaves → the longest-standing active member becomes admin; last member leaves → the circle is deleted.** Admins can't remove other admins. | A circle is never left without a moderator, and there are no empty zombie circles. |
| 45 | **Trending = active joins in the last 7 days × 3 + member count**, excluding circles you already belong to or have requested. | Favours momentum over size, so small new circles can surface. |
| 46 | **Suggestions = circles in your interest categories that have room and that you haven't joined**, public first, topped up with trending. | Covers new users whose interests have no circles yet. |
| 47 | Private circles: name, description, category, member count and admins are public; **feed and member list are members-only**. | People need enough information to decide whether to request access. |
| 48 | **Private profile = only people who share an active circle with you** see your posts, circles, location, website and cover. Name, avatar and bio stay visible. | Fits the circles model: no followers to approve, so the circle *is* the trust boundary. |
| 49 | A user's circle list hides private circles unless the viewer is also in them. | Membership of a private circle is itself private information. |
| 50 | Member counts are shown as capacity ("12/150"), never as follower-style popularity. | The brief bans follower numbers. Capacity is functional information. |
| 51 | List endpoints return `viewerStatus` per circle, filled with **one batched membership query**. | Join buttons render correctly (Join / Requested / Joined) without N+1 queries or extra client calls. |
| 52 | Join/approve events (`CircleJoinRequestedEvent`, `CircleJoinApprovedEvent`) are published now. Step 6 notifications just add listeners. | Circles don't need to know about notifications. |
| 53 | Circles and profiles without an uploaded cover get a **deterministic generative cover** (overlapping circles from the vibe palette, seeded by slug or username). | Every circle looks designed from day one with no stock-photo dependency, and it adapts to light and dark themes. |
| 54 | Circle search in Step 3 uses `ILIKE` on name and description (with escaped wildcards). Step 7 replaces it with PostgreSQL full-text search (V5). | Matches the build order. The API contract (`?q=`) stays the same. |
| 55 | Pinning posts lands in Step 4. `circles.pinned_post_id` exists now, and its FK is added in V3. | Posts don't exist until Step 4. |
| 56 | Offset pagination (`PageResponse`) for circles and members; keyset cursors are reserved for time-ordered feeds. | Browse lists need "page N" and stable sorting by size. Feeds need cursors. |
| 57 | JPQL compares enums through **bound parameters**, not enum literals. | Hibernate renders enum literals as a cast to a guessed PG type (`memberstatus`), which doesn't exist. |
| 58 | OpenAPI: a nullable field whose type is another DTO is emitted as `allOf: [$ref], nullable: true` (`PropertyCustomizer`). | swagger-core otherwise marks the *referenced component* nullable everywhere, which made `JoinResult.membership` wrongly nullable in TS. |

## Step 4: Posts, vibes, comments, feed

| # | Decision | Why |
| --- | --- | --- |
| 59 | **The API never exposes reaction counts.** Posts and comments carry a `vibe` of *proportions* (0–1 per type, summing to 1) plus your own reaction. Counts exist only server-side for ranking. | The brief: "vibe shown as a colour-blended ring, not a number". Enforcing that in the API stops any client from turning vibes into a popularity score. |
| 60 | Reacting is one atomic `INSERT … ON CONFLICT (user_id, post_id) DO UPDATE` that returns the previous type. The client toggles by sending DELETE when you pick your current vibe again. | One reaction per user under concurrency, and the denormalised `reaction_count` only changes for new or removed reactions, never for a change of type. |
| 61 | **Feed ranking = "rank time"**: `created_at + min(24h, 6h × ln(1 + reactions + 2 × comments))`, computed in a native query. Keyset pagination on `(rank_time, id)` with cursor `<rankTime>,<id>`. | Recency plus engagement in one sortable value; keyset-friendly; logarithmic, so a viral post can't pin itself to the top forever. Comments count double because conversation matters more than a tap. |
| 62 | Because rank times move as engagement arrives, a post can occasionally shift between pages, so **the client de-dupes by id**. | The accepted trade-off of keyset over a live score, which is far cheaper than offset pagination or snapshotting. |
| 63 | Circle and profile timelines are plain chronological keyset (`created_at, id`). The home feed is the only ranked surface. | Inside a circle, people expect time order. |
| 64 | Posts are created by **one multipart request** (`post` JSON part + ≤ 4 `images`), validated and processed server-side. The text or at least one image is required. | Atomic create with no orphan "upload first" flow, and the server controls every stored URL. (Cloud uploads of a post that then fails validation can orphan files; acceptable at this scale.) |
| 65 | Stored images record **width and height**; the client reserves the aspect ratio before load. | No layout shift (Lighthouse CLS). |
| 66 | Edit window: **text only, 15 minutes**, enforced server-side. Edited posts show "edited". Images can't be swapped after posting. | Matches the brief, and stops a post being repurposed after it gathers vibes. |
| 67 | Delete: author **or circle admin**. Comments: comment author, post author or circle admin. | Admins need to moderate their circle. Authors own the conversation under their post. |
| 68 | Private-circle posts answer **404 (not 403)** to outsiders. | Doesn't confirm that a post exists. |
| 69 | Posting, reacting and commenting require **active membership** of the post's circle. Reading follows circle visibility. | Circles are the participation boundary: you can browse public circles but join to take part. |
| 70 | Comments: oldest-first (conversation order), top-level paginated, **all replies inlined**. Replying to a reply attaches to its top-level parent. | One level of nesting, as the brief asks, without losing replies-to-replies. |
| 71 | Comments are **kept in sync with posts** via a denormalised `comment_count` (deleting a top-level comment subtracts its replies too). | Counts in lists without COUNT queries per post. |
| 72 | The `reports` table landed in V3 (polymorphic `target_type` + `target_id`, unique per reporter and target). Comment and user reporting UI comes in Step 7. | Post reporting is a Step 4 feature. Re-reporting is a silent no-op. |
| 73 | `PostAssembler` hydrates a page of posts with **5 fixed queries** (images, vibe groups, my reactions, saves, admin circles). | No N+1 on feeds. |
| 74 | Domain events `PostReactedEvent` (new reactions only, not self) and `CommentCreatedEvent` are published now for Step 6 notifications. | Decoupled. |
| 75 | Client caching: every post list lives under `['posts', …]` and `patchPost()` updates a post in all lists at once. Reactions and saves are optimistic with rollback. | One source of truth for a post across feed, circle, profile and saved views. |
| 76 | The "Pinned" badge only shows on the circle's own page. | Pinning is circle context; in the home feed it was noise. |
| 77 | Dev seed post photos come from `picsum.photos` (deterministic seeds). | Realistic demo content without committing binaries. Dev profile only. |
| 78 | Profanity filtering is deferred to Step 7 (moderation), as the brief's build order places it. | Keeps Step 4 focused. |

## Step 5: Moments and Rewind

| # | Decision | Why |
| --- | --- | --- |
| 79 | **V4 creates every remaining social table** (moments, messaging, notifications, blocks); V5 will be search. Unmapped tables are ignored by Hibernate validation until their entities arrive. | Keeps the brief's exact V1–V5 layout while still landing features step by step. |
| 80 | **A moment's audience is everyone who shares at least one active circle with the author**, not a single circle. | Moments are personal ("today"), unlike posts, which are circle conversations. There are no followers, so circle-mates are the natural audience. |
| 81 | Reads filter on `expires_at > now()`, so an expired moment vanishes **on time** even between scheduler runs. The `@Scheduled` job (every 10 min, configurable via `app.moments.archive-interval`) flips `archived = true`. | Correctness doesn't depend on the job's timing. The job makes the Rewind state explicit and indexable. |
| 82 | **Rewind = your archived moments, `/me/rewind` only.** There is no endpoint to read anyone else's. The Rewind tab only renders on your own profile. | "Private, owner only" enforced by construction, not by a permission check that could be forgotten. |
| 83 | Views: `INSERT … ON CONFLICT DO NOTHING` on `(moment_id, viewer_id)`, and the counter only increments when a row was inserted. **`viewCount` is null in every payload except the author's.** Authors' own views don't count. | Count-once semantics under retries. "View count visible only to author" holds at the API level. |
| 84 | Strip ordering: **you first → people with unseen moments → people you've caught up on**, each by freshest moment. Playback starts at a person's first unseen moment. | The strip highlights what's new and resumes where you left off. |
| 85 | Text moments use one of 6 server-validated gradient keys (`sunset, ocean, forest, dusk, ember, plum`), rendered by design-system CSS classes and dark enough for white text. Photo moments are fitted within 1080×1920 and can carry a short caption. | No arbitrary colours from clients, so contrast is guaranteed. |
| 86 | Viewer interactions: tap left third = back, right two-thirds = forward; **press-and-hold pauses**; horizontal swipe = next/previous person; swipe down or Esc closes. Space pauses and the arrow keys navigate; focusable Previous/Next buttons appear on keyboard focus. It also pauses when the tab is hidden. Reduced motion swaps slides for fades. | Familiar story-viewer gestures, fully operable without a pointer. |
| 87 | Photo moments start their timer **only once the image has loaded**. | Slow networks don't skip a photo before it appears. |
| 88 | Marking seen updates the strip cache in place (no refetch). The strip refetches every 5 minutes. | Rings settle smoothly. Moments expire continuously, so the strip stays fresh. |
| 89 | Moment creation is rate-limited to 15 per hour per user. | Abuse protection, in line with the other write limits. |

## Step 6: Realtime messaging and notifications

| # | Decision | Why |
| --- | --- | --- |
| 90 | STOMP `CONNECT` is authenticated by a channel interceptor using the **JWT in the STOMP `Authorization` header**. The client refreshes the token before every (re)connect. | SockJS can't set HTTP headers. Long-lived sockets survive the 15-minute token through reconnect-time refresh. |
| 91 | **Clients may only `SUBSCRIBE` to `/user/queue/**`.** Typing, read receipts, messages and notifications are all delivered per user, and there are no shared topics. | No way to eavesdrop on other conversations by guessing a topic name. No per-topic ACL to get wrong. |
| 92 | `AuthUser` implements `Principal` with `getName() = userId`. | Spring's user destinations route on the principal name, so `convertAndSendToUser(userId, …)` just works. |
| 93 | **Realtime pushes are deferred until the transaction commits** (`RealtimePublisher`). | Clients never receive a message they can't read back over REST yet, or one that was rolled back. |
| 94 | Sending goes over STOMP when connected and **falls back to REST**. Optimistic messages carry a `clientId` that the server echoes back to the sender only, for reconciliation. STOMP failures go to `/user/queue/errors` with the `clientId` so the optimistic bubble is removed. | Instant UX, resilient to socket drops, and no duplicate bubbles. |
| 95 | **You can start a conversation only with someone who shares an active circle with you.** Existing conversations keep working even if you later leave the circle. | Keeps DMs consistent with the circles model and limits unsolicited messages. Blocking (Step 7) adds a hard stop. |
| 96 | 1:1 uniqueness via `conversations.direct_key = "<min uuid>:<max uuid>"` (UNIQUE). Concurrent "start" calls race safely (`REQUIRES_NEW` creator plus retry on unique violation). | Exactly one thread per pair. |
| 97 | Read state: `messages.read_at` (per message, 1:1) plus `participants.last_read_at`. Opening or focusing a thread marks it read and sends one receipt. The UI shows Sending / Sent / **Seen** under your latest message. | Simple and exact for 1:1. |
| 98 | Typing indicators are **ephemeral** (never persisted): sent at most every 3s while typing, `false` after 3s idle or on send. The receiver expires them after 6s anyway. | Low traffic, and a stuck indicator is impossible even if a "stop" frame is lost. |
| 99 | A shared post is embedded **per recipient's visibility**. If the recipient can't read it (private circle, deleted), the message says so instead of leaking it. | Sharing must not bypass circle privacy. |
| 100 | The Messages badge counts **conversations** with unread messages, not messages. | A calmer number that matches what the user has to act on. |
| 101 | Notifications are created by **`@TransactionalEventListener(AFTER_COMMIT)`** listeners in a `REQUIRES_NEW` transaction, and failures are logged rather than propagated. | A rolled-back action never notifies, and a notification bug never breaks posting, reacting or joining. |
| 102 | Notification rules: never notify yourself. Respect `users.notification_prefs` per type. **Repeated unread reactions (same actor and post) or messages (same sender) fold into one**, bumped to the top. A reply notifies the parent author, and the post author also gets a "comment" unless they *are* the parent author. | The bell stays meaningful instead of noisy. |
| 103 | Reaction notifications show the actor's **current** vibe, looked up when listing (one batched query) instead of stored. | Changing a vibe shouldn't leave a stale notification, and no extra column is needed. |
| 104 | Message toasts carry the sender in the payload (`MessageDto.sender`). | Toasts must not depend on what happens to be in the client cache. |
| 105 | The simple in-memory broker sends 20s heartbeats on a private scheduler (not a bean). | A scheduler bean would silently replace Boot's default for `@Scheduled` jobs (moment expiry). A relay broker (RabbitMQ) is the path to multiple instances. |
| 106 | A WebSocket integration test runs on a real port with a SockJS STOMP client: it checks that an unauthenticated CONNECT is rejected and that messages, typing and notifications arrive live. | Covers the whole realtime path, not just services. |

## Step 7: Search, settings, moderation

| # | Decision | Why |
| --- | --- | --- |
| 107 | **Generated `tsvector` columns** (`GENERATED ALWAYS … STORED`) with GIN indexes on posts, users and circles. | Always in sync with no triggers or app code. The index serves `@@` directly. |
| 108 | Names and usernames use the `simple` config (no stemming). Free text (posts, bios, descriptions) uses `english` (stemming: "baked" finds "bake"). Circle names are indexed under both. | "Maya" must match "maya" exactly, and prose should match word forms. |
| 109 | Queries are built as **prefix tsqueries** (`word:*`, AND-ed, max 6 terms) from letters and digits only. | Results appear while typing, and user input can't inject tsquery operators (`!:*&|()`). |
| 110 | Search respects everything else: private-circle posts only for members, blocked people excluded, and only onboarded users (with usernames) shown. Ranked by `ts_rank` (circles also by size). | Search must never be a side door around privacy. |
| 111 | **Discover's `?q=` now uses the same full-text search** (replacing Step 3's ILIKE, per decision 54). | One search behaviour across the app. |
| 112 | Recent searches are **stored only in the browser** (`localStorage`, max 8). A search is remembered only once it returned something. | As the brief asks. Nothing about what you search for is kept on the server. |
| 113 | **Blocking is symmetric in effect**: either side blocking hides both people from each other in the feed, circle timelines, post pages (404), comments, saved posts, moments, search, contacts, member lists and profiles (404). It also stops messages and notifications. Only the blocker can unblock, and the other person isn't told. | "Hides content both ways", with no "you've been blocked" signal that could escalate harassment. |
| 114 | Blocks are enforced **server-side** (SQL `NOT IN` in the ranked feed and timelines, and access checks in `PostAccess`, which also covers reactions, comments and shares). | A client can't opt out. One choke point covers many endpoints. |
| 115 | **Profanity is softened, not rejected**: listed words are masked (`f***`), including suffixes, stretched letters and leetspeak. It applies to posts, comments, messages, moments, circle text, bios and names. Profane circle names and usernames are refused. The client shows a hint before you post. | A calm nudge rather than a wall. Usernames and circle names are permanent identifiers, so they're refused instead of masked. |
| 116 | The word list is intentionally short and whole-word (so "Scunthorpe", "peacock", "cocktail" and "Dickens" pass). The client keeps a copy only for the warning; the server is authoritative. | Few false positives. Serious moderation remains reports plus human review. |
| 117 | Users and comments can be reported (posts since Step 4). The same dialog, reasons and once-per-target rule apply everywhere. | Consistent and spam-resistant. |
| 118 | Settings: privacy and notification switches are **optimistic** and roll back on error. Unknown notification keys are rejected. | Instant feedback, and rapid toggling acts on the latest state. |
| 119 | **Changing your password signs out every other session** (all refresh tokens revoked) and returns a fresh session for this device. Google-only accounts can *set* a password. Changing your email needs your current password, and Google-only accounts must set a password first. | Standard account-takeover hygiene. |
| 120 | **Deleting an account** first leaves every circle through the normal path (admin hand-over or closing empty circles), then deletes the user and lets FKs cascade everything else. It needs your password, or typing DELETE for Google-only accounts. Uploaded images on Cloudinary are not purged yet (noted for later). | No orphaned admin-less circles, and no accidental deletion. |

## Step 8: Quality, performance and deployment

| # | Decision | Why |
| --- | --- | --- |
| 121 | **Every route is code-split** with React Router `lazy`, and the composers, moment viewer and realtime client load on demand. Rolldown `codeSplitting` groups only React (a stable vendor chunk) and the icons (instead of dozens of tiny chunks). | Entry dropped from 166 KB to 43 KB gzipped. Broader groups were tried and *hurt*: they dragged forms and motion into the entry. |
| 122 | **The session restore starts at module load**, and route `loader`s **prefetch** each page's data (feed, circle, conversations) as soon as the session is known, without awaiting it. | Removes whole round trips from first render, in parallel with chunk downloads. |
| 123 | `/auth/refresh` without a cookie returns `200 { data: null }` instead of 401. Bad or expired cookies still get 401. | Every signed-out page load probes for a session, and that's not an error (it was polluting consoles and Lighthouse). |
| 124 | **Native WebSocket first** (`/ws/websocket`, the raw endpoint Spring exposes on a SockJS mapping), with `sockjs-client` loaded on demand only after repeated connect failures. | SockJS registers a deprecated `unload` listener (Best Practices, bfcache) and adds weight. It remains the fallback for networks that block WebSockets, as the brief asks. |
| 125 | Google Fonts load **non-blocking** (`preload` + swap) with **metric-matched local fallbacks** (`size-adjust`/`ascent-override`). | No render-blocking CSS, and the font swap causes zero layout shift (CLS 0 on every page). |
| 126 | A static, theme-aware splash lives in `index.html`. Hero covers load eagerly with `fetchpriority=high`. Cloudinary images use `f_auto,q_auto` with `srcset`. | Earlier first paint, faster LCP images, and AVIF/WebP at the right size in production. |
| 127 | Accessibility: 24px minimum touch targets in post meta, no `aria-label` overriding visible text in conversation rows, and **100** on every audited page. | Meets the brief's ≥ 95. |
| 128 | Performance is **93–94 on the sign-in page and 78–87 on signed-in pages** in local throttled-mobile runs. The remaining gap is two unavoidable API round trips (session, then data) before content in a client-rendered SPA. | Recorded honestly. Closing it fully needs SSR or edge-rendering, which is outside the stack. Re-measure on Vercel (HTTP/2, CDN). |
| 129 | Rate limits are scaled by **`app.rate-limit.multiplier`** (1 in prod, 25 in the dev profile). | All e2e users share one IP, and dev/test must not trip brute-force limits that production keeps strict. |
| 130 | **E2E tests create users through the API** and test the real UI for the flow under test. Each test uses fresh users, so they run in parallel and are order-independent. CI runs them against `vite preview` (production build) proxying to a real API with seed data. | Fast, stable (3/3 green runs locally) and close to production. |
| 131 | The seeder also runs under a **`demo`** profile (`prod,demo` on Render by default) so the public deployment has content to explore. | The brief: "all features work end-to-end with seed data" on the live site. Remove `demo` to start empty. |
| 132 | `render.yaml` blueprint: Docker, free plan, health check, `JWT_SECRET` generated by Render, all other secrets entered in the dashboard (`sync: false`). The JVM is capped at 60% of container RAM. | Reproducible infrastructure with no secrets in git. Fits the 512 MB free instance (measured about 330 MB). |
| 133 | Neon is used through its **direct** (non-pooler) endpoint with a small Hikari pool (5). | Avoids PgBouncer transaction-mode pitfalls with JDBC prepared statements, and is well within free-tier connection limits. |
| 134 | **`/actuator/health` is a liveness check: the DB and disk-space indicators are disabled.** | Render uses it to decide whether to keep the instance. With Neon's free tier suspending idle compute, a DB-dependent check flips DOWN (503) or times out during wake-up, and Render stops a healthy server. Verified by freezing Postgres under the prod image: health stays 200 UP in ~3 ms. DB failures surface on real requests and Hikari reconnects. |
