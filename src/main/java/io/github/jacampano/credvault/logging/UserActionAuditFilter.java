package io.github.jacampano.credvault.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class UserActionAuditFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final Logger LOG = LoggerFactory.getLogger(UserActionAuditFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.startsWith("/h2-console/")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        long startNanos = System.nanoTime();
        String method = request.getMethod();
        String path = request.getRequestURI();
        boolean oauthTrace = isOauthFlowPath(path);
        if (oauthTrace) {
            LOG.info("OAUTH_TRACE phase=request method={} path={} query={} sessionId={} ip={} userAgent={}",
                    method,
                    path,
                    sanitizeQuery(request.getQueryString()),
                    request.getRequestedSessionId(),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
        }
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            if (oauthTrace) {
                LOG.error("OAUTH_TRACE phase=exception method={} path={} status={} errorType={} message={}",
                        method,
                        path,
                        response.getStatus(),
                        ex.getClass().getSimpleName(),
                        ex.getMessage());
            }
            throw ex;
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            AuditActionClassifier.AuditRoute route = AuditActionClassifier.classify(method, path);
            String username = resolveUsername();
            LOG.info("AUDIT user={} section={} action={} method={} path={} status={} durationMs={} ip={}",
                    username,
                    route.section(),
                    route.action(),
                    method,
                    path,
                    response.getStatus(),
                    elapsedMs,
                    request.getRemoteAddr());
            if (oauthTrace) {
                LOG.info("OAUTH_TRACE phase=response method={} path={} status={} durationMs={} location={}",
                        method,
                        path,
                        response.getStatus(),
                        elapsedMs,
                        response.getHeader("Location"));
            }
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String fromHeader = request.getHeader(CORRELATION_ID_HEADER);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "anonymous" : name;
    }

    private boolean isOauthFlowPath(String path) {
        return path != null && (path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/oauth/authorize"));
    }

    private String sanitizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return query
                .replaceAll("code=[^&]+", "code=***")
                .replaceAll("state=[^&]+", "state=***")
                .replaceAll("nonce=[^&]+", "nonce=***");
    }
}
