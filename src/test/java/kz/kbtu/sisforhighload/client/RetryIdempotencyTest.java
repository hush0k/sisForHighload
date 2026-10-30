package kz.kbtu.sisforhighload.client;

import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import kz.kbtu.sisforhighload.counter.CounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "grpc.port=0",
                "counter.min-view-duration=0s",
                "fault.enabled=true",
                "fault.failure-rate=0.2",
                "fault.slow-response-rate=0.1",
                "fault.slow-response-delay=500ms"
        })
class RetryIdempotencyTest {

    private static final Duration CLIENT_TIMEOUT = Duration.ofMillis(300);
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(50);
    private static final int MAX_ATTEMPTS = 10;
    private static final int USERS = 30;
    private static final long ANY_USER = 0;

    @Value("${local.server.port}")
    private int restPort;

    @Autowired
    private Server grpcServer;

    @Autowired
    private CounterService counterService;

    @Test
    void grpcLikesAreCountedExactlyOnceDespiteFaults() throws Exception {
        long postId = 1001;
        RetryExecutor retryExecutor = newRetryExecutor();

        try (GrpcCounterClient client = newGrpcClient(retryExecutor)) {
            for (long userId = 1; userId <= USERS; userId++) {
                client.likePost(postId, userId);
                client.likePost(postId, userId);
            }
        }

        assertEquals(USERS, counterService.getCounters(postId, ANY_USER).likes());
        assertTrue(retryExecutor.getRetryCount() > 0, "Injected faults should cause retries");
    }

    @Test
    void grpcViewsAreCountedExactlyOnceDespiteFaults() throws Exception {
        long postId = 1002;
        RetryExecutor retryExecutor = newRetryExecutor();

        try (GrpcCounterClient client = newGrpcClient(retryExecutor)) {
            for (long userId = 1; userId <= USERS; userId++) {
                String viewToken = client.startView(postId, userId);
                client.finishView(postId, userId, viewToken);
                client.finishView(postId, userId, viewToken);
            }
        }

        assertEquals(USERS, counterService.getCounters(postId, ANY_USER).views());
    }

    @Test
    void restLikesAreCountedExactlyOnceDespiteFaults() {
        long postId = 2001;
        RetryExecutor retryExecutor = newRetryExecutor();

        try (RestCounterClient client = newRestClient(retryExecutor)) {
            for (long userId = 1; userId <= USERS; userId++) {
                client.likePost(postId, userId);
                client.likePost(postId, userId);
            }
        }

        assertEquals(USERS, counterService.getCounters(postId, ANY_USER).likes());
        assertTrue(retryExecutor.getRetryCount() > 0, "Injected faults should cause retries");
    }

    @Test
    void restViewsAreCountedExactlyOnceDespiteFaults() {
        long postId = 2002;
        RetryExecutor retryExecutor = newRetryExecutor();

        try (RestCounterClient client = newRestClient(retryExecutor)) {
            for (long userId = 1; userId <= USERS; userId++) {
                String viewToken = client.startView(postId, userId);
                client.finishView(postId, userId, viewToken);
                client.finishView(postId, userId, viewToken);
            }
        }

        assertEquals(USERS, counterService.getCounters(postId, ANY_USER).views());
    }

    @Test
    void grpcInvalidTokenIsNotRetried() throws Exception {
        try (GrpcCounterClient client = newGrpcClient(newRetryExecutor())) {
            StatusRuntimeException error = assertThrows(StatusRuntimeException.class,
                    () -> client.finishView(3001, 1, "fake-token"));
            assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
        }
    }

    @Test
    void restInvalidTokenIsNotRetried() {
        try (RestCounterClient client = newRestClient(newRetryExecutor())) {
            HttpStatusException error = assertThrows(HttpStatusException.class,
                    () -> client.finishView(3002, 1, "fake-token"));
            assertEquals(400, error.getStatusCode());
        }
    }

    private RetryExecutor newRetryExecutor() {
        return new RetryExecutor(MAX_ATTEMPTS, INITIAL_BACKOFF);
    }

    private GrpcCounterClient newGrpcClient(RetryExecutor retryExecutor) {
        return new GrpcCounterClient("localhost", grpcServer.getPort(), CLIENT_TIMEOUT, retryExecutor);
    }

    private RestCounterClient newRestClient(RetryExecutor retryExecutor) {
        return new RestCounterClient("http://localhost:" + restPort + "/api/v1", CLIENT_TIMEOUT, retryExecutor);
    }
}