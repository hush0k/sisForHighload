package kz.kbtu.sisforhighload.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import kz.kbtu.sisforhighload.fault.GrpcFaultInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
public class GrpcServerConfig {

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(@Value("${grpc.port}") int port,
                             PostCounterGrpcService postCounterService,
                             GrpcFaultInterceptor faultInterceptor) throws IOException {
        Server server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(
                        postCounterService, new UserIdInterceptor(), faultInterceptor))
                .build()
                .start();
        log.info("gRPC server started on port {}", server.getPort());
        return server;
    }
}