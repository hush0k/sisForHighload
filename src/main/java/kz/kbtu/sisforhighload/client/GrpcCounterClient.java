package kz.kbtu.sisforhighload.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import kz.kbtu.sisforhighload.grpc.proto.BatchCountersReply;
import kz.kbtu.sisforhighload.grpc.proto.BatchCountersRequest;
import kz.kbtu.sisforhighload.grpc.proto.CountersReply;
import kz.kbtu.sisforhighload.grpc.proto.FinishViewRequest;
import kz.kbtu.sisforhighload.grpc.proto.PostCounterGrpc;
import kz.kbtu.sisforhighload.grpc.proto.PostRequest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GrpcCounterClient implements AutoCloseable {

    private static final Metadata.Key<String> USER_ID_HEADER =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);

    private final ManagedChannel channel;
    private final PostCounterGrpc.PostCounterBlockingStub baseStub;
    private final Duration deadline;
    private final RetryExecutor retryExecutor;

    public GrpcCounterClient(String host, int port, Duration deadline, RetryExecutor retryExecutor) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.baseStub = PostCounterGrpc.newBlockingStub(channel);
        this.deadline = deadline;
        this.retryExecutor = retryExecutor;
    }

    public void likePost(long postId, long userId) {
        PostRequest request = postRequest(postId);
        retryExecutor.execute("grpc.likePost", () -> stubFor(userId).likePost(request), this::isRetryable);
    }

    public void unlikePost(long postId, long userId) {
        PostRequest request = postRequest(postId);
        retryExecutor.execute("grpc.unlikePost", () -> stubFor(userId).unlikePost(request), this::isRetryable);
    }

    public String startView(long postId, long userId) {
        PostRequest request = postRequest(postId);
        return retryExecutor.execute("grpc.startView", () -> stubFor(userId).startView(request), this::isRetryable)
                .getViewToken();
    }

    public void finishView(long postId, long userId, String viewToken) {
        FinishViewRequest request = FinishViewRequest.newBuilder()
                .setPostId(postId)
                .setViewToken(viewToken)
                .build();
        retryExecutor.execute("grpc.finishView", () -> stubFor(userId).finishView(request), this::isRetryable);
    }

    public CountersReply getCounters(long postId, long userId) {
        PostRequest request = postRequest(postId);
        return retryExecutor.execute("grpc.getCounters", () -> stubFor(userId).getCounters(request), this::isRetryable);
    }

    public BatchCountersReply getCountersBatch(List<Long> postIds, long userId) {
        BatchCountersRequest request = BatchCountersRequest.newBuilder().addAllPostIds(postIds).build();
        return retryExecutor.execute("grpc.batchGetCounters", () -> stubFor(userId).batchGetCounters(request), this::isRetryable);
    }

    @Override
    public void close() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private PostCounterGrpc.PostCounterBlockingStub stubFor(long userId) {
        Metadata headers = new Metadata();
        headers.put(USER_ID_HEADER, Long.toString(userId));
        return baseStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
    }

    private PostRequest postRequest(long postId) {
        return PostRequest.newBuilder().setPostId(postId).build();
    }

    private boolean isRetryable(Exception error) {
        if (!(error instanceof StatusRuntimeException statusError)) {
            return false;
        }
        Status.Code code = statusError.getStatus().getCode();
        return code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED;
    }
}