package mz.com.sgp.security.jwt;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class JwtTokenFilter extends GenericFilterBean {

	private JwtTokenProvider tokenProvider;

	public JwtTokenFilter(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filter)
	        throws IOException, ServletException {

	    HttpServletRequest httpRequest = (HttpServletRequest) request;

	    try {
	        String token = tokenProvider.resolveToken(httpRequest);

	        if (StringUtils.isNotBlank(token)) {

	            try {
	                if (tokenProvider.validateToken(token)) {
	                    Authentication auth = tokenProvider.getAuthentication(token);
	                    SecurityContextHolder.getContext().setAuthentication(auth);
	                }
	            } catch (Exception ex) {
	                System.out.println("Token inválido: " + ex.getMessage());
	            }
	        }

	        filter.doFilter(request, response);

	    } catch (Exception e) {
	        throw e;
	    }
	}
}