package mz.com.sgp.security;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import mz.com.sgp.config.SecurityConfig;

class PasswordEncodingTests {
    @Test void strongerNewHashesStillAcceptExistingHashes() {
        org.springframework.security.crypto.password.PasswordEncoder encoder =
                ReflectionTestUtils.invokeMethod(new SecurityConfig(), "passwordEncoder");
        var legacy = new Pbkdf2PasswordEncoder("", 8, 185000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        String password = "A-long-password-123";
        String legacyHash = legacy.encode(password);
        assertThat(encoder.matches(password, "{pbkdf2}" + legacyHash)).isTrue();
        assertThat(encoder.matches(password, legacyHash)).isTrue();
        String current = encoder.encode(password);
        assertThat(current).startsWith("{pbkdf2-v2}");
        assertThat(encoder.matches(password, current)).isTrue();
        assertThat(encoder.matches("incorrect", current)).isFalse();
    }
}
