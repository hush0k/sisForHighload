package kz.kbtu.sisforhighload.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import kz.kbtu.sisforhighload.counter.CounterService;
import kz.kbtu.sisforhighload.counter.CounterSnapshot;
import kz.kbtu.sisforhighload.counter.exception.InvalidViewTokenException;
import kz.kbtu.sisforhighload.counter.exception.ViewTooShortException;
import kz.kbtu.sisforhighload.grpc.proto.Ack;
import kz.kbtu.sisforhighload.grpc.proto.BatchCountersReply;
import kz.kbtu.sisforhighload.grpc.proto.BatchCountersRequest;
import kz.kbtu.sisforhighload.grpc.proto.CountersReply;
import kz.kbtu.sisforhighload.grpc.proto.FinishViewRequest;
import kz.kbtu.sisforhighload.grpc.proto.PostCounterGrpc;
import kz.kbtu.sisforhighload.grpc.proto.PostRequest;
import kz.kbtu.sisforhighload.grpc.proto.StartViewReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PostCounterGrpcService extends PostCounterGrpc.PostCounterImplBase {

    private static final Ack ACK = Ack.getDefaultInstance();
    private static final int MAX_BATCH_SIZE = 100;

    private final CounterService counterService;

    @Override
    public void likePost(PostRequest request, StreamObserver<Ack> responseObserver) {
        respond(responseObserver, () -> {
            counterService.likePost(requireValidPostId(request.getPostId()), currentUserId());
            return ACK;
        });
    }

    @Override
    public void unlikePost(PostRequest request, StreamObserver<Ack> responseObserver) {
        respond(responseObserver, () -> {
            counterService.unlikePost(requireValidPostId(request.getPostId()), currentUserId());
            return ACK;
        });
    }

    @Override
    public void startView(PostRequest request, StreamObserver<StartViewReply> responseObserver) {
        respond(responseObserver, () -> {
            String viewToken = counterService.startView(requireValidPostId(request.getPostId()), currentUserId());
            return StartViewReply.newBuilder().setViewToken(viewToken).build();
        });
    }

    @Override
    public void finishView(FinishViewRequest request, StreamObserver<Ack> responseObserver) {
        respond(responseObserver, () -> {
            counterService.finishView(requireValidPostId(request.getPostId()), currentUserId(), request.getViewToken());
            return ACK;
        });
    }

    @Override
    public void getCounters(PostRequest request, StreamObserver<CountersReply> responseObserver) {
        respond(responseObserver, () -> {
            CounterSnapshot snapshot = counterService.getCounters(requireValidPostId(request.getPostId()), currentUserId());
            return toReply(snapshot);
        });
    }

    @Override
    public void batchGetCounters(BatchCountersRequest request, StreamObserver<BatchCountersReply> responseObserver) {
        respond(responseObserver, () -> {
            List<Long> postIds = requireValidBatch(request.getPostIdsList());
            List<CounterSnapshot> snapshots = counterService.getCountersBatch(postIds, currentUserId());

            BatchCountersReply.Builder reply = BatchCountersReply.newBuilder();
            for (CounterSnapshot snapshot : snapshots) {
                reply.addCounters(toReply(snapshot));
            }
            return reply.build();
        });
    }

    private <T> void respond(StreamObserver<T> responseObserver, Supplier<T> action) {
        try {
            responseObserver.onNext(action.get());
            responseObserver.onCompleted();
        } catch (InvalidViewTokenException | ViewTooShortException | IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }

    private long currentUserId() {
        return UserIdInterceptor.USER_ID.get();
    }

    private long requireValidPostId(long postId) {
        if (postId <= 0) {
            throw new IllegalArgumentException("post_id must be positive");
        }
        return postId;
    }

    private List<Long> requireValidBatch(List<Long> postIds) {
        if (postIds.isEmpty() || postIds.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("post_ids size must be between 1 and " + MAX_BATCH_SIZE);
        }
        postIds.forEach(this::requireValidPostId);
        return postIds;
    }

    private CountersReply toReply(CounterSnapshot snapshot) {
        return CountersReply.newBuilder()
                .setPostId(snapshot.postId())
                .setLikes(snapshot.likes())
                .setViews(snapshot.views())
                .setLikedByMe(snapshot.likedByMe())
                .build();
    }
}