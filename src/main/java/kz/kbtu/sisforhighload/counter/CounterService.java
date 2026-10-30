package kz.kbtu.sisforhighload.counter;

import kz.kbtu.sisforhighload.counter.exception.InvalidViewTokenException;
import kz.kbtu.sisforhighload.counter.exception.ViewTooShortException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class CounterService {

    private final CounterProperties properties;

    private final Map<Long, Set<Long>> likesByPost = new ConcurrentHashMap<>();

    private final Map<Long, AtomicLong> viewsByPost = new ConcurrentHashMap<>();

    private final Map<String, ViewSession> viewSessions = new ConcurrentHashMap<>();

    private final Map<String, Instant> lastCountedViews = new ConcurrentHashMap<>();

    public void likePost(long postId, long userId) {
        likesByPost.computeIfAbsent(postId, id -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void unlikePost(long postId, long userId) {
        Set<Long> likedUsers = likesByPost.get(postId);
        if (likedUsers != null) {
            likedUsers.remove(userId);
        }
    }

    public String startView(long postId, long userId) {
        String viewToken = UUID.randomUUID().toString();
        viewSessions.put(viewToken, new ViewSession(postId, userId, Instant.now()));
        return viewToken;
    }

    public void finishView(long postId, long userId, String viewToken) {
        ViewSession session = findValidSession(postId, userId, viewToken);
        Instant now = Instant.now();
        ensureViewLongEnough(session, now);

        String dedupKey = postId + ":" + userId;
        Instant countedAt = lastCountedViews.compute(dedupKey,
                (key, previous) -> isDedupActive(previous, now) ? previous : now);

        boolean isNewView = countedAt.equals(now);
        if (isNewView) {
            viewsByPost.computeIfAbsent(postId, id -> new AtomicLong()).incrementAndGet();
        }
    }

    public CounterSnapshot getCounters(long postId, long userId) {
        Set<Long> likedUsers = likesByPost.getOrDefault(postId, Set.of());
        AtomicLong views = viewsByPost.get(postId);
        long viewCount = views == null ? 0 : views.get();
        return new CounterSnapshot(postId, likedUsers.size(), viewCount, likedUsers.contains(userId));
    }

    public List<CounterSnapshot> getCountersBatch(List<Long> postIds, long userId) {
        List<CounterSnapshot> result = new ArrayList<>(postIds.size());
        for (Long postId : postIds) {
            result.add(getCounters(postId, userId));
        }
        return result;
    }

    private ViewSession findValidSession(long postId, long userId, String viewToken) {
        ViewSession session = viewSessions.get(viewToken);
        if (session == null || session.postId() != postId || session.userId() != userId) {
            throw new InvalidViewTokenException("View token is invalid for this post or user");
        }
        return session;
    }

    private void ensureViewLongEnough(ViewSession session, Instant now) {
        Duration watched = Duration.between(session.startedAt(), now);
        if (watched.compareTo(properties.minViewDuration()) < 0) {
            throw new ViewTooShortException("View is shorter than " + properties.minViewDuration());
        }
    }

    private boolean isDedupActive(Instant previous, Instant now) {
        return previous != null
                && Duration.between(previous, now).compareTo(properties.viewDedupTtl()) < 0;
    }
}