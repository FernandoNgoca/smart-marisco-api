package mz.com.sgp.security.jwt;

import static mz.com.sgp.mapper.ObjectMapper.parseObject;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.data.dto.UserDTO;
import mz.com.sgp.data.dto.security.TokenDTO;
import mz.com.sgp.model.RefreshTokenEntity;
import mz.com.sgp.model.UserEntity;
import mz.com.sgp.repository.RefreshTokenRepository;
import mz.com.sgp.repository.UserRepository;

@Service
public class JwtTokenProvider {
    @Value("${security.jwt.token.secret-key}")
    private String secretKey;
    @Value("${security.jwt.token.issuer:smart-marisco-api}")
    private String issuer;
    @Value("${security.jwt.token.audience:smart-marisco-client}")
    private String audience;
    @Value("${security.jwt.token.expire-length:900000}")
    private long accessLifetime;
    @Value("${security.jwt.token.refresh-length:10800000}")
    private long refreshLifetime;

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private Algorithm algorithm;
    private JWTVerifier accessVerifier;
    private JWTVerifier refreshVerifier;

    public JwtTokenProvider(UserRepository users, RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.refreshTokens = refreshTokens;
    }

    @PostConstruct
    void init() {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(secretKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET must be Base64 encoding of at least 32 random bytes");
        }
        if (key.length < 32 || accessLifetime <= 0 || refreshLifetime <= accessLifetime) {
            throw new IllegalStateException("Invalid JWT key or token lifetimes");
        }
        algorithm = Algorithm.HMAC256(key);
        accessVerifier = verifier("access");
        refreshVerifier = verifier("refresh");
    }

    private JWTVerifier verifier(String type) {
        return JWT.require(algorithm).withIssuer(issuer).withAudience(audience)
                .withClaim("token_type", type).withClaimPresence("sub")
                .withClaimPresence("exp").withClaimPresence("iat")
                .withClaimPresence("jti").withClaimPresence("version").build();
    }

    @Transactional
    public TokenDTO createAccessToken(String username) {
        UserEntity user = users.findForUpdateByUsername(username);
        requireActive(user);
        return issue(user);
    }

    private TokenDTO issue(UserEntity user) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plusMillis(accessLifetime);
        Instant refreshExpiry = now.plusMillis(refreshLifetime);
        String refreshId = UUID.randomUUID().toString();
        refreshTokens.deleteExpired(now);
        refreshTokens.save(new RefreshTokenEntity(refreshId, user.getId(), refreshExpiry));
        return new TokenDTO(user.getUsername(), true, Date.from(now), Date.from(accessExpiry),
                token(user, "access", UUID.randomUUID().toString(), now, accessExpiry),
                token(user, "refresh", refreshId, now, refreshExpiry), parseObject(user, UserDTO.class));
    }

    private String token(UserEntity user, String type, String id, Instant now, Instant expiry) {
        return JWT.create().withIssuer(issuer).withAudience(audience).withSubject(user.getUsername())
                .withJWTId(id).withIssuedAt(now).withExpiresAt(expiry).withClaim("token_type", type)
                .withClaim("version", user.getTokenVersion()).withClaim("roles", user.getRoles())
                .sign(algorithm);
    }

    @Transactional
    public TokenDTO refreshToken(String username, String bearerToken) {
        DecodedJWT jwt = verify(refreshVerifier, bearer(bearerToken));
        if (!jwt.getSubject().equals(username)) throw invalid();
        UserEntity user = users.findForUpdateByUsername(username);
        requireValidUser(user, jwt);
        if (refreshTokens.consume(jwt.getId(), user.getId(), Instant.now()) != 1) throw invalid();
        return issue(user);
    }

    public Authentication getAuthentication(String token) {
        DecodedJWT jwt = verify(accessVerifier, token);
        UserEntity user = users.findByUsername(jwt.getSubject());
        requireValidUser(user, jwt);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private DecodedJWT verify(JWTVerifier verifier, String token) {
        try {
            if (token == null) throw invalid();
            return verifier.verify(token);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            throw invalid();
        }
    }

    private void requireValidUser(UserEntity user, DecodedJWT jwt) {
        requireActive(user);
        Long version = jwt.getClaim("version").asLong();
        if (version == null || version != user.getTokenVersion()) throw invalid();
    }

    public static void requireActive(UserEntity user) {
        if (user == null || user.getStatus() != EntityState.ACTIVE || !user.isEnabled()
                || !user.isAccountNonLocked() || !user.isAccountNonExpired()
                || !user.isCredentialsNonExpired()) throw invalid();
    }

    private static BadCredentialsException invalid() {
        return new BadCredentialsException("Credenciais inválidas");
    }

    private String bearer(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    public String resolveToken(HttpServletRequest request) {
        return bearer(request.getHeader("Authorization"));
    }
}
