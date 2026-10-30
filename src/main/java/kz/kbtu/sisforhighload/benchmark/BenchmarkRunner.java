package kz.kbtu.sisforhighload.benchmark;

import kz.kbtu.sisforhighload.client.GrpcCounterClient;
import kz.kbtu.sisforhighload.client.RestCounterClient;
import kz.kbtu.sisforhighload.client.RetryExecutor;
import kz.kbtu.sisforhighload.counter.CounterSnapshot;
import kz.kbtu.sisforhighload.grpc.proto.BatchCountersReply;
import kz.kbtu.sisforhighload.grpc.proto.CountersReply;
import kz.kbtu.sisforhighload.grpc.proto.FinishViewRequest;
import kz.kbtu.sisforhighload.rest.dto.BatchCountersResponse;
import kz.kbtu.sisforhighload.rest.dto.FinishViewBody;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.LongStream;

public class BenchmarkRunner {

    private static final String REST_BASE_URL = "http://localhost:8080/api/v1";
    private static final String GRPC_HOST = "localhost";
    private static final int GRPC_PORT = 9090;

    private static final int WARMUP_REQUESTS = 2_000;
    private static final int MEASURED_REQUESTS = 10_000;
    private static final int BATCH_SIZE = 20;
    private static final int SEED_USERS = 5;
    private static final long POST_ID = 1;
    private static final long USER_ID = 1;

    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(1);
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(100);

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    @FunctionalInterface
    private interface BenchmarkCall {
        void run();
    }

    public static void main(String[] args) throws Exception {
        RetryExecutor restRetry = new RetryExecutor(MAX_ATTEMPTS, INITIAL_BACKOFF);
        RetryExecutor grpcRetry = new RetryExecutor(MAX_ATTEMPTS, INITIAL_BACKOFF);
        List<Long> batchPostIds = LongStream.rangeClosed(1, BATCH_SIZE).boxed().toList();

        try (RestCounterClient rest = new RestCounterClient(REST_BASE_URL, CLIENT_TIMEOUT, restRetry);
             GrpcCounterClient grpc = new GrpcCounterClient(GRPC_HOST, GRPC_PORT, CLIENT_TIMEOUT, grpcRetry)) {

            seedData(grpc, batchPostIds);

            List<LatencyStats> results = new ArrayList<>();
            results.add(measure("REST getCounters", () -> rest.getCounters(POST_ID, USER_ID)));
            results.add(measure("gRPC getCounters", () -> grpc.getCounters(POST_ID, USER_ID)));
            results.add(measure("REST batch (20 posts)", () -> rest.getCountersBatch(batchPostIds, USER_ID)));
            results.add(measure("gRPC batch (20 posts)", () -> grpc.getCountersBatch(batchPostIds, USER_ID)));
            results.add(measure("REST likePost", () -> rest.likePost(POST_ID, USER_ID)));
            results.add(measure("gRPC likePost", () -> grpc.likePost(POST_ID, USER_ID)));

            printLatencyTable(results);
            printPayloadTable(grpc, batchPostIds);
            System.out.printf("%nRetries during benchmark: REST=%d, gRPC=%d%n",
                    restRetry.getRetryCount(), grpcRetry.getRetryCount());
        }
    }

    private static void seedData(GrpcCounterClient grpc, List<Long> postIds) {
        for (long postId : postIds) {
            for (long userId = 1; userId <= SEED_USERS; userId++) {
                grpc.likePost(postId, userId);
                String viewToken = grpc.startView(postId, userId);
                grpc.finishView(postId, userId, viewToken);
            }
        }
    }

    private static LatencyStats measure(String name, BenchmarkCall call) {
        for (int i = 0; i < WARMUP_REQUESTS; i++) {
            call.run();
        }
        long[] durationsNanos = new long[MEASURED_REQUESTS];
        for (int i = 0; i < MEASURED_REQUESTS; i++) {
            long start = System.nanoTime();
            call.run();
            durationsNanos[i] = System.nanoTime() - start;
        }
        return LatencyStats.from(name, durationsNanos);
    }

    private static void printLatencyTable(List<LatencyStats> results) {
        System.out.printf("%n### Latency (%d requests, ms)%n", MEASURED_REQUESTS);
        System.out.println("| Operation | avg | p50 | p95 | p99 |");
        System.out.println("|---|---|---|---|---|");
        results.forEach(stats -> System.out.println(stats.toMarkdownRow()));
    }

    private static void printPayloadTable(GrpcCounterClient grpc, List<Long> batchPostIds) {
        CountersReply singleReply = grpc.getCounters(POST_ID, USER_ID);
        BatchCountersReply batchReply = grpc.getCountersBatch(batchPostIds, USER_ID);
        String viewToken = UUID.randomUUID().toString();

        BatchCountersResponse batchJson = new BatchCountersResponse(
                batchReply.getCountersList().stream().map(BenchmarkRunner::toSnapshot).toList());
        FinishViewRequest finishViewProto = FinishViewRequest.newBuilder()
                .setPostId(POST_ID)
                .setViewToken(viewToken)
                .build();

        System.out.println("\n### Payload size (body only, bytes)");
        System.out.println("| Message | JSON | Protobuf | JSON / Protobuf |");
        System.out.println("|---|---|---|---|");
        printPayloadRow("getCounters response", jsonSize(toSnapshot(singleReply)), singleReply.getSerializedSize());
        printPayloadRow("batch response (20 posts)", jsonSize(batchJson), batchReply.getSerializedSize());
        printPayloadRow("finishView request", jsonSize(new FinishViewBody(viewToken)), finishViewProto.getSerializedSize());
    }

    private static void printPayloadRow(String name, int jsonBytes, int protobufBytes) {
        System.out.printf(Locale.US, "| %s | %d | %d | %.2fx |%n",
                name, jsonBytes, protobufBytes, (double) jsonBytes / protobufBytes);
    }

    private static int jsonSize(Object body) {
        return JSON_MAPPER.writeValueAsBytes(body).length;
    }

    private static CounterSnapshot toSnapshot(CountersReply reply) {
        return new CounterSnapshot(reply.getPostId(), reply.getLikes(), reply.getViews(), reply.getLikedByMe());
    }
}