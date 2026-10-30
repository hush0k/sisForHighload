package kz.kbtu.sisforhighload.counter;

public record CounterSnapshot(
        long postId,
        long likes,
        long views,
        boolean likedByMe
) {}
