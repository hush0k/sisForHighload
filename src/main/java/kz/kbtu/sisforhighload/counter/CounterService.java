package kz.kbtu.sisforhighload.counter;

import kz.kbtu.sisforhighload.counter.exception.InvalidViewTokenException;
import kz.kbtu.sisforhighload.counter.exception.ViewTooShortException;
import kz.kbtu.sisforhighload.counter.repository.LikeCountRow;
import kz.kbtu.sisforhighload.counter.repository.LikeRepository;
import kz.kbtu.sisforhighload.counter.repository.PostCounterRepository;
import kz.kbtu.sisforhighload.counter.repository.PostCounterRow;
import kz.kbtu.sisforhighload.counter.repository.ViewDedupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CounterService {

    private final CounterProperties properties;

    private final TuningProperties tuning;

    private final LikeRepository likeRepository;

    private final PostCounterRepository postCounterRepository;

    private final ViewDedupRepository viewDedupRepository;

    private final Map<String, ViewSession> viewSessions = new ConcurrentHashMap<>();

    @Transactional
    public void likePost(long postId, long userId) {
        LocalDateTime now = utcNow();
        int inserted = insertLike(postId, userId, now);
        if (inserted > 0) {
            postCounterRepository.incrementLikes(postId, now);
        }
    }

    @Transactional
    public void unlikePost(long postId, long userId) {
        int deleted = likeRepository.deleteLike(postId, userId);
        if (deleted > 0) {
            postCounterRepository.decrementLikes(postId, utcNow());
        }
    }

    public String startView(long postId, long userId) {
        String viewToken = UUID.randomUUID().toString();
        viewSessions.put(viewToken, new ViewSession(postId, userId, Instant.now()));
        return viewToken;
    }

    @Transactional
    public void finishView(long postId, long userId, String viewToken) {
        ViewSession session = findValidSession(postId, userId, viewToken);
        Instant now = Instant.now();
        ensureViewLongEnough(session, now);

        LocalDateTime countedAt = toUtc(now);
        LocalDateTime threshold = toUtc(now.minus(properties.viewDedupTtl()));
        int changed = viewDedupRepository.upsertIfExpired(postId, userId, countedAt, threshold);
        if (changed > 0) {
            postCounterRepository.incrementViews(postId, countedAt);
        }
    }

    @Transactional(readOnly = true)
    public CounterSnapshot getCounters(long postId, long userId) {
        return getCountersBatch(List.of(postId), userId).get(0);
    }

    @Transactional(readOnly = true)
    public List<CounterSnapshot> getCountersBatch(List<Long> postIds, long userId) {
        Long[] ids = postIds.toArray(Long[]::new);
        Map<Long, PostCounterRow> counters = loadCounters(ids);
        Map<Long, Long> likes = loadLikes(ids, counters);
        Set<Long> likedByMe = Set.copyOf(likeRepository.findLikedPostIds(userId, ids));
        return postIds.stream()
                .map(postId -> toSnapshot(postId, counters.get(postId), likes, likedByMe))
                .toList();
    }

    private int insertLike(long postId, long userId, LocalDateTime now) {
        if (tuning.hasPrimaryKey()) {
            return likeRepository.insertOnConflictDoNothing(postId, userId, now);
        }
        return likeRepository.insertIfAbsent(postId, userId, now);
    }

    private Map<Long, PostCounterRow> loadCounters(Long[] ids) {
        return postCounterRepository.findCountersByPostIds(ids).stream()
                .collect(Collectors.toMap(PostCounterRow::getPostId, row -> row));
    }

    private Map<Long, Long> loadLikes(Long[] ids, Map<Long, PostCounterRow> counters) {
        Map<Long, Long> likes = new HashMap<>();
        if (tuning.countersFromTable()) {
            counters.forEach((postId, row) -> likes.put(postId, row.getLikesCount()));
            return likes;
        }
        for (LikeCountRow row : likeRepository.countLikesByPostIds(ids)) {
            likes.put(row.getPostId(), row.getLikesCount());
        }
        return likes;
    }

    private CounterSnapshot toSnapshot(long postId, PostCounterRow counter,
                                       Map<Long, Long> likes, Set<Long> likedByMe) {
        long views = counter == null ? 0 : counter.getViewsCount();
        return new CounterSnapshot(postId, likes.getOrDefault(postId, 0L), views, likedByMe.contains(postId));
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

    private LocalDateTime utcNow() {
        return toUtc(Instant.now());
    }

    private LocalDateTime toUtc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
