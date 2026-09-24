package mz.com.sgp.security.jwt;

import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;

    public JwtTokenFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // The refresh endpoint validates its own refresh token, never as an access token.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/auth/signin") || path.startsWith("/auth/refresh/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String token = tokenProvider.resolveToken(request);
        if (token != null) {
            try {
                SecurityContextHolder.getContext().setAuthentication(tokenProvider.getAuthentication(token));
            } catch (AuthenticationException ex) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Credenciais inválidas");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
