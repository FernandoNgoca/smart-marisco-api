package mz.com.sgp.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import mz.com.sgp.security.AuthRateLimitFilter;
import mz.com.sgp.security.jwt.JwtTokenFilter;
import mz.com.sgp.security.jwt.JwtTokenProvider;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        PasswordEncoder legacy = new Pbkdf2PasswordEncoder("", 8, 185000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        PasswordEncoder current = new Pbkdf2PasswordEncoder("", 16, 600000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder("pbkdf2-v2",
                Map.of("pbkdf2", legacy, "pbkdf2-v2", current));
        encoder.setDefaultPasswordEncoderForMatches(legacy);
        return encoder;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider tokens) throws Exception {
        return http.httpBasic(AbstractHttpConfigurer::disable)
                // Tokens are supplied in Authorization, never automatically in cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtTokenFilter(tokens), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new AuthRateLimitFilter(), JwtTokenFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401, "Autenticação necessária"))
                        .accessDeniedHandler((req, res, ex) -> res.sendError(403, "Acesso negado")))
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signin").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/auth/refresh/*").permitAll()
                        .requestMatchers("/auth/createUser", "/auth", "/auth/").hasRole("ADMIN")
                        .requestMatchers("/auth/change-password", "/auth/update-user").authenticated()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "MANAGER", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/sale/v1", "/api/client/v1")
                            .hasAnyRole("ADMIN", "MANAGER", "USER")
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "MANAGER")
                        .anyRequest().denyAll())
                .cors(Customizer.withDefaults()).build();
    }
}
