package kz.kbtu.sisforhighload.counter;

import kz.kbtu.sisforhighload.counter.repository.BenchSeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("bench")
@RequiredArgsConstructor
public class BenchSeeder {

    private final BenchProperties properties;

    private final BenchSeedRepository seedRepository;

    public void seedIfEmpty() {
        if (!seedRepository.tablesAreEmpty()) {
            log.info("Данные уже есть, генерация пропущена");
            return;
        }
        log.info("Генерация: {} постов, {} лайков на пост, {} пользователей",
                properties.posts(), properties.likesPerPost(), properties.users());
        int counters = seedRepository.seedLikesAndCounters(
                properties.posts(), properties.likesPerPost(), properties.users());
        seedRepository.analyze();
        log.info("Готово, строк в post_counters: {}", counters);
    }
}
