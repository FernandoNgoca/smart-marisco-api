package mz.com.sgp.security;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.data.dto.security.TokenDTO;
import mz.com.sgp.model.PermissionEntity;
import mz.com.sgp.model.UserEntity;
import mz.com.sgp.repository.PermissionRepository;
import mz.com.sgp.repository.UserRepository;
import mz.com.sgp.security.jwt.JwtTokenProvider;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PermissionRepository permissions;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtTokenProvider tokens;
    @Autowired ObjectMapper mapper;
    static final AtomicInteger addresses = new AtomicInteger();
    String username;
    String ip;
    TokenDTO pair;

    @BeforeEach
    void setup() {
        ip = "test-" + addresses.incrementAndGet();
        username = "u" + UUID.randomUUID().toString().substring(0, 12);
        createUser(username, "ROLE_USER");
        pair = tokens.createAccessToken(username);
    }

    UserEntity createUser(String name, String role) {
        var found = permissions.findByDescriptionIn(List.of(role));
        PermissionEntity permission;
        if (found.isEmpty()) {
            permission = new PermissionEntity();
            permission.setDescription(role);
            permission = permissions.save(permission);
        } else permission = found.get(0);
        UserEntity user = new UserEntity();
        user.setUserName(name);
        user.setFullName(name);
        user.setPassword(encoder.encode("Original-password-123"));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setPermissions(List.of(permission));
        return users.save(user);
    }

    MockHttpServletRequestBuilder request(MockHttpServletRequestBuilder request) {
        return request.with(r -> { r.setRemoteAddr(ip); return r; });
    }

    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request(request).header("Authorization", "Bearer " + token);
    }

    @Test void anonymousCannotCreateAdmin() throws Exception {
        mvc.perform(request(post("/auth/createUser")).contentType("application/json")
                .content("{\"username\":\"attacker\",\"password\":\"Long-password-123\",\"roles\":[\"ROLE_ADMIN\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void ordinaryUserCannotCreateUsersOrListThem() throws Exception {
        mvc.perform(auth(post("/auth/createUser"), pair.getAccessToken()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(auth(get("/auth"), pair.getAccessToken())).andExpect(status().isForbidden());
    }

    @Test void adminCanCreateUserAndListWithoutPasswordHashes() throws Exception {
        String admin = username + "a";
        createUser(admin, "ROLE_ADMIN");
        String token = tokens.createAccessToken(admin).getAccessToken();
        mvc.perform(auth(post("/auth/createUser"), token).contentType("application/json")
                .content("{\"username\":\"" + username + "b\",\"password\":\"Long-password-123\",\"fullname\":\"Test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(auth(get("/auth"), token)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
        assertThat(users.findByUsername(username + "b").getRoles()).containsExactly("ROLE_USER");
    }

    @Test void usersCanReadButCannotChangeCatalogue() throws Exception {
        mvc.perform(auth(get("/api/category/v1"), pair.getAccessToken())).andExpect(status().isOk());
        mvc.perform(auth(post("/api/category/v1"), pair.getAccessToken()).contentType("application/json")
                .content("{\"name\":\"New\",\"description\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void managerCanChangeCatalogueButCannotManageUsers() throws Exception {
        String name = username + "m";
        createUser(name, "ROLE_MANAGER");
        String token = tokens.createAccessToken(name).getAccessToken();
        mvc.perform(auth(post("/api/category/v1"), token).contentType("application/json")
                .content("{\"name\":\"New\",\"description\":\"Test\"}"))
                .andExpect(status().isOk());
        mvc.perform(auth(get("/auth"), token)).andExpect(status().isForbidden());
    }

    @Test void profileUpdateIsBoundToCurrentUser() throws Exception {
        String target = username + "b";
        createUser(target, "ROLE_USER");
        mvc.perform(auth(put("/auth/update-user"), pair.getAccessToken()).contentType("application/json")
                .content("{\"userName\":\"" + target + "\",\"image\":\"changed\"}"))
                .andExpect(status().isForbidden());
        assertThat(users.findByUsername(target).getImage()).isNull();
        mvc.perform(auth(put("/auth/update-user"), pair.getAccessToken()).contentType("application/json")
                .content("{\"userName\":\"" + username + "\",\"image\":\"changed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test void tokenTypesCannotBeInterchanged() throws Exception {
        mvc.perform(auth(get("/api/category/v1"), pair.getRefreshToken())).andExpect(status().isUnauthorized());
        mvc.perform(auth(put("/auth/refresh/" + username), pair.getAccessToken())).andExpect(status().isUnauthorized());
    }

    @Test void refreshIsSingleUseAndBoundToUsername() throws Exception {
        mvc.perform(auth(put("/auth/refresh/someone-else"), pair.getRefreshToken())).andExpect(status().isUnauthorized());
        String body = mvc.perform(auth(put("/auth/refresh/" + username), pair.getRefreshToken()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("password");
        String newToken = mapper.readTree(body).path("body").path("refreshToken").asText();
        assertThat(newToken).isNotBlank().isNotEqualTo(pair.getRefreshToken());
        mvc.perform(auth(put("/auth/refresh/" + username), pair.getRefreshToken())).andExpect(status().isUnauthorized());
        mvc.perform(auth(put("/auth/refresh/" + username), newToken)).andExpect(status().isOk());
    }

    @Test void passwordChangeRevokesAccessAndRefresh() throws Exception {
        mvc.perform(auth(put("/auth/change-password"), pair.getAccessToken()).contentType("application/json")
                .content("{\"username\":\"" + username + "\",\"oldPassword\":\"Original-password-123\",\"newPassword\":\"Changed-password-123\"}"))
                .andExpect(status().isOk());
        mvc.perform(auth(get("/api/category/v1"), pair.getAccessToken())).andExpect(status().isUnauthorized());
        mvc.perform(auth(put("/auth/refresh/" + username), pair.getRefreshToken())).andExpect(status().isUnauthorized());
        assertThat(encoder.matches("Changed-password-123", users.findByUsername(username).getPassword())).isTrue();
    }

    @Test void cannotChangeAnotherUsersPasswordOrUseWeakPassword() throws Exception {
        mvc.perform(auth(put("/auth/change-password"), pair.getAccessToken()).contentType("application/json")
                .content("{\"username\":\"other\",\"oldPassword\":\"Original-password-123\",\"newPassword\":\"Changed-password-123\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(auth(put("/auth/change-password"), pair.getAccessToken()).contentType("application/json")
                .content("{\"username\":\"" + username + "\",\"oldPassword\":\"Original-password-123\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void disabledAndInactiveAccountsLoseAccess() throws Exception {
        var user = users.findByUsername(username);
        user.setEnabled(false);
        users.save(user);
        mvc.perform(auth(get("/api/category/v1"), pair.getAccessToken())).andExpect(status().isUnauthorized());
        mvc.perform(auth(put("/auth/refresh/" + username), pair.getRefreshToken())).andExpect(status().isUnauthorized());
        user.setEnabled(true);
        user.setStatus(EntityState.INACTIVE);
        users.save(user);
        mvc.perform(auth(get("/api/category/v1"), pair.getAccessToken())).andExpect(status().isUnauthorized());
    }

    @Test void loginReturnsNoPasswordAndBadCredentialsAreGeneric() throws Exception {
        mvc.perform(request(post("/auth/signin")).contentType("application/json")
                .content("{\"username\":\"" + username + "\",\"password\":\"Original-password-123\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
        mvc.perform(request(post("/auth/signin")).contentType("application/json")
                .content("{\"username\":\"missing\",\"password\":\"invalid\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test void untrustedCorsOriginIsRejected() throws Exception {
        mvc.perform(request(options("/auth/signin")).header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(request(options("/auth/signin")).header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test void authenticationRequestsAreRateLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(request(post("/auth/signin")).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(request(post("/auth/signin")).contentType("application/json").content("{}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
    }
    @Test void lockedAndExpiredAccountsCannotRenew() throws Exception {
        var user = users.findByUsername(username);
        user.setAccountNonLocked(false);
        users.save(user);
        mvc.perform(auth(get("/api/category/v1"), pair.getAccessToken())).andExpect(status().isUnauthorized());
        mvc.perform(auth(put("/auth/refresh/" + username), pair.getRefreshToken())).andExpect(status().isUnauthorized());
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(false);
        users.save(user);
        mvc.perform(auth(get("/api/category/v1"), pair.getAccessToken())).andExpect(status().isUnauthorized());
    }

    @Test void simultaneousRefreshAllowsOnlyOneSuccess() throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> refresh = () -> {
            start.await();
            try {
                tokens.refreshToken(username, "Bearer " + pair.getRefreshToken());
                return true;
            } catch (org.springframework.security.authentication.BadCredentialsException ex) {
                return false;
            }
        };
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(refresh);
            var second = executor.submit(refresh);
            start.countDown();
            assertThat(List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test void invalidJwtClaimsAndSignaturesAreRejected() throws Exception {
        var algorithm = com.auth0.jwt.algorithms.Algorithm.HMAC256(java.util.Base64.getDecoder().decode(
                "dGVzdC1vbmx5LXNlY3JldC1uZXZlci11c2UtaW4tcHJvZHVjdGlvbg=="));
        for (String variant : List.of("issuer", "audience", "expired", "version", "signature", "legacy")) {
            var builder = com.auth0.jwt.JWT.create().withSubject(username)
                    .withIssuer(variant.equals("issuer") ? "wrong" : "smart-marisco-api")
                    .withAudience(variant.equals("audience") ? "wrong" : "smart-marisco-client")
                    .withIssuedAt(java.time.Instant.now().minusSeconds(120))
                    .withExpiresAt(java.time.Instant.now().plusSeconds(variant.equals("expired") ? -60 : 60))
                    .withJWTId(UUID.randomUUID().toString())
                    .withClaim("version", variant.equals("version") ? 99L : 0L);
            if (!variant.equals("legacy")) builder.withClaim("token_type", "access");
            String token = builder.sign(variant.equals("signature")
                    ? com.auth0.jwt.algorithms.Algorithm.HMAC256("another-key") : algorithm);
            mvc.perform(auth(get("/api/category/v1"), token)).andExpect(status().isUnauthorized());
        }
    }

}
