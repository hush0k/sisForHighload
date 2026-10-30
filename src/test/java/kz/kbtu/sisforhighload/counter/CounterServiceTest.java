package kz.kbtu.sisforhighload.counter;

import kz.kbtu.sisforhighload.counter.exception.InvalidViewTokenException;
import kz.kbtu.sisforhighload.counter.exception.ViewTooShortException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CounterServiceTest {

    private static final long POST_ID = 777;
    private static final long USER_ID = 1;
    private static final long OTHER_USER_ID = 2;

    @Test
    void likeTwiceCountsOnce() {
        CounterService service = serviceWithMinViewDuration(Duration.ZERO);

        service.likePost(POST_ID, USER_ID);
        service.likePost(POST_ID, USER_ID);

        CounterSnapshot counters = service.getCounters(POST_ID, USER_ID);
        assertEquals(1, counters.likes());
        assertTrue(counters.likedByMe());
    }

    @Test
    void unlikeRemovesLike() {
        CounterService service = serviceWithMinViewDuration(Duration.ZERO);

        service.likePost(POST_ID, USER_ID);
        service.unlikePost(POST_ID, USER_ID);

        CounterSnapshot counters = service.getCounters(POST_ID, USER_ID);
        assertEquals(0, counters.likes());
        assertFalse(counters.likedByMe());
    }

    @Test
    void finishViewTwiceWithSameTokenCountsOnce() {
        CounterService service = serviceWithMinViewDuration(Duration.ZERO);
        String viewToken = service.startView(POST_ID, USER_ID);

        service.finishView(POST_ID, USER_ID, viewToken);
        service.finishView(POST_ID, USER_ID, viewToken);

        assertEquals(1, service.getCounters(POST_ID, USER_ID).views());
    }

    @Test
    void secondViewWithinDedupWindowIsNotCounted() {
        CounterService service = serviceWithMinViewDuration(Duration.ZERO);

        String firstToken = service.startView(POST_ID, USER_ID);
        service.finishView(POST_ID, USER_ID, firstToken);
        String secondToken = service.startView(POST_ID, USER_ID);
        service.finishView(POST_ID, USER_ID, secondToken);

        assertEquals(1, service.getCounters(POST_ID, USER_ID).views());
    }

    @Test
    void tooShortViewIsRejected() {
        CounterService service = serviceWithMinViewDuration(Duration.ofHours(1));
        String viewToken = service.startView(POST_ID, USER_ID);

        assertThrows(ViewTooShortException.class,
                () -> service.finishView(POST_ID, USER_ID, viewToken));
    }

    @Test
    void tokenOfAnotherUserIsRejected() {
        CounterService service = serviceWithMinViewDuration(Duration.ZERO);
        String viewToken = service.startView(POST_ID, USER_ID);

        assertThrows(InvalidViewTokenException.class,
                () -> service.finishView(POST_ID, OTHER_USER_ID, viewToken));
    }

    private CounterService serviceWithMinViewDuration(Duration minViewDuration) {
        return new CounterService(new CounterProperties(minViewDuration, Duration.ofHours(1)));
    }
}