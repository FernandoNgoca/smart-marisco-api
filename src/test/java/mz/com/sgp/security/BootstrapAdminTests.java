package mz.com.sgp.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import mz.com.sgp.model.UserEntity;
import mz.com.sgp.repository.UserRepository;

class BootstrapAdminTests {
    static final String SEED = "{pbkdf2}d1655d5ac31b92342b58152d794f19adf7e9965a3b9a270ee90d7e2c009302d4b41c3b3ceeee88cb";
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test void refusesOriginalSeedWithoutReplacement() {
        var user = new UserEntity();
        user.setPassword(SEED);
        when(users.findForUpdateByUsername("admin")).thenReturn(user);
        assertThatThrownBy(() -> new BootstrapAdmin(users, encoder, "").run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class);
        verify(users, never()).save(any());
    }

    @Test void replacesSeedAndInvalidatesOldSessions() {
        var user = new UserEntity();
        user.setPassword(SEED);
        user.setEnabled(false);
        when(users.findForUpdateByUsername("admin")).thenReturn(user);
        when(encoder.encode("New-admin-password-123")).thenReturn("new-hash");
        new BootstrapAdmin(users, encoder, "New-admin-password-123").run(new DefaultApplicationArguments());
        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(1);
    }

    @Test void neverResetsCustomAdminPassword() {
        var user = new UserEntity();
        user.setPassword("custom-hash");
        when(users.findForUpdateByUsername("admin")).thenReturn(user);
        new BootstrapAdmin(users, encoder, "New-admin-password-123").run(new DefaultApplicationArguments());
        assertThat(user.getPassword()).isEqualTo("custom-hash");
        verify(users, never()).save(any());
    }
}
