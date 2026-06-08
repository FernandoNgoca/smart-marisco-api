package mz.com.sgp.config.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

	@Override
	public Optional<String> getCurrentAuditor() {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		// ❌ rejeita autenticação inválida ou anónima
		if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}

		Object principal = auth.getPrincipal();

		// ✔ utilizador Spring Security real
		if (principal instanceof UserDetails userDetails) {
			return Optional.of(userDetails.getUsername());
		}

		String name = auth.getName();

		// ❌ elimina fallback "anonymousUser"
		if ("anonymousUser".equalsIgnoreCase(name)) {
			return Optional.empty();
		}

		return Optional.of(name);
	}
}
