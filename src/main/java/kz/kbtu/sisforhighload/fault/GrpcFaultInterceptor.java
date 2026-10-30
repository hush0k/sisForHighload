package kz.kbtu.sisforhighload.fault;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcFaultInterceptor implements ServerInterceptor {

    private final FaultInjector faultInjector;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        if (faultInjector.shouldFail()) {
            call.close(Status.UNAVAILABLE.withDescription("Injected failure"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
        if (faultInjector.shouldRespondSlowly()) {
            faultInjector.delayResponse();
        }
        return next.startCall(call, headers);
    }
}