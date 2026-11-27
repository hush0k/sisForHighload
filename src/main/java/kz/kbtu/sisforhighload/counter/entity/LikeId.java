package kz.kbtu.sisforhighload.counter.entity;

import java.io.Serializable;

public record LikeId(long postId, long userId) implements Serializable {
}
