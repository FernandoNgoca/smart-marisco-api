package mz.com.sgp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import mz.com.sgp.repository.UserRepository;

/** One-time replacement of the original seed password; never resets an existing custom password. */
@Component
public class BootstrapAdmin implements ApplicationRunner {
    private static final String SEED_HASH = "{pbkdf2}d1655d5ac31b92342b58152d794f19adf7e9965a3b9a270ee90d7e2c009302d4b41c3b3ceeee88cb";
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String password;

    public BootstrapAdmin(UserRepository users, PasswordEncoder encoder,
            @Value("${security.bootstrap-admin-password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var admin = users.findForUpdateByUsername("admin");
        if (admin == null || !SEED_HASH.equals(admin.getPassword())) return;
        if (password.isBlank()) {
            throw new IllegalStateException("Set BOOTSTRAP_ADMIN_PASSWORD to replace the disabled seed credential");
        }
        PasswordPolicy.validate(password);
        admin.setPassword(encoder.encode(password));
        admin.setEnabled(true);
        admin.setTokenVersion(admin.getTokenVersion() + 1);
        users.save(admin);
    }
}
