package kz.kbtu.sisforhighload.counter.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_counters")
@Getter
@NoArgsConstructor
public class PostCounterEntity {

    @Id
    private long postId;

    private long likesCount;

    private long viewsCount;

    private LocalDateTime updatedAt;
}
