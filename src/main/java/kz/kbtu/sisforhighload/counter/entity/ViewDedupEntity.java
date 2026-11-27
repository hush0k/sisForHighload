package kz.kbtu.sisforhighload.counter.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_dedup")
@IdClass(LikeId.class)
@Getter
@NoArgsConstructor
public class ViewDedupEntity {

    @Id
    private long postId;

    @Id
    private long userId;

    private LocalDateTime countedAt;
}
