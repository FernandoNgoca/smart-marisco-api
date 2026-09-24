package mz.com.sgp.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import mz.com.sgp.repository.RefreshTokenRepository;
import mz.com.sgp.repository.UserRepository;

class JwtConfigurationTests {
    @Test void rejectsDefaultAndShortSigningSecrets() {
        for (String secret : new String[] {"secret", "c2VjcmV0", ""}) {
            var provider = new JwtTokenProvider(mock(UserRepository.class), mock(RefreshTokenRepository.class));
            ReflectionTestUtils.setField(provider, "secretKey", secret);
            assertThatThrownBy(provider::init).isInstanceOf(IllegalStateException.class);
        }
    }
}
