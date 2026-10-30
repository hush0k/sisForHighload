package kz.kbtu.sisforhighload.fault;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestFaultFilter extends OncePerRequestFilter {

    private final FaultInjector faultInjector;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (faultInjector.shouldFail()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Injected failure");
            return;
        }
        if (faultInjector.shouldRespondSlowly()) {
            faultInjector.delayResponse();
        }
        filterChain.doFilter(request, response);
    }
}