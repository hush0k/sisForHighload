package kz.kbtu.sisforhighload.counter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "bench")
public record BenchProperties(
        @DefaultValue("1000000") long posts,
        @DefaultValue("20") int likesPerPost,
        @DefaultValue("1000000") long users) {
}
