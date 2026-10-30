package kz.kbtu.sisforhighload.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

public class UserIdInterceptor implements ServerInterceptor {

    public static final Context.Key<Long> USER_ID = Context.key("userId");

    private static final Metadata.Key<String> USER_ID_HEADER =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        String rawUserId = headers.get(USER_ID_HEADER);
        long userId;
        try {
            userId = Long.parseLong(rawUserId);
        } catch (NumberFormatException exception) {
            call.close(Status.INVALID_ARGUMENT.withDescription("Missing or invalid x-user-id"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        Context context = Context.current().withValue(USER_ID, userId);
        return Contexts.interceptCall(context, call, headers, next);
    }
}