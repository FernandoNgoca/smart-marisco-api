package mz.com.sgp.security;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Local, bounded rate limit. Multi-instance deployments also need a shared gateway limit. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    private final int limit;
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_CLIENTS = 10_000;
    private record Window(long start, int count) {}

    public AuthRateLimitFilter() { this(Clock.systemUTC(), 20); }
    public AuthRateLimitFilter(Clock clock, int limit) { this.clock = clock; this.limit = limit; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equals(request.getMethod()) || !(path.equals("/auth/signin")
                || path.equals("/auth/change-password") || path.startsWith("/auth/refresh/"));
    }

    private synchronized boolean allow(String client) {
        long now = clock.millis();
        windows.entrySet().removeIf(e -> now - e.getValue().start() >= WINDOW_MS);
        Window window = windows.get(client);
        if (window == null) {
            if (windows.size() >= MAX_CLIENTS) return false;
            windows.put(client, new Window(now, 1));
            return true;
        }
        if (window.count() >= limit) return false;
        windows.put(client, new Window(window.start(), window.count() + 1));
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Do not trust a client-supplied X-Forwarded-For header.
        if (!allow(request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60");
            response.sendError(429, "Demasiadas tentativas. Tente novamente mais tarde.");
            return;
        }
        chain.doFilter(request, response);
    }
}
