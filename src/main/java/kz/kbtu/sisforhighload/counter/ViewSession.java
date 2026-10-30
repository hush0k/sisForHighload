package kz.kbtu.sisforhighload.counter;

import java.time.Instant;

public record ViewSession(
        long postId,
        long userId,
        Instant startedAt
) {
}