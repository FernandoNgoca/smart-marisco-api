package mz.com.sgp.services;

import static mz.com.sgp.mapper.ObjectMapper.parseObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.controllers.AuthController;
import mz.com.sgp.data.dto.UserDTO;
import mz.com.sgp.data.dto.security.AccountCredentialsDTO;
import mz.com.sgp.data.dto.security.ChangePasswordDTO;
import mz.com.sgp.data.dto.security.TokenDTO;
import mz.com.sgp.exception.RequiredObjectIsNullException;
import mz.com.sgp.model.PermissionEntity;
import mz.com.sgp.model.UserEntity;
import mz.com.sgp.repository.PermissionRepository;
import mz.com.sgp.repository.UserRepository;
import mz.com.sgp.security.jwt.JwtTokenProvider;

@Service
public class AuthService {

	Logger logger = LoggerFactory.getLogger(AuthService.class);

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtTokenProvider tokenProvider;

	@Autowired
	private UserRepository repository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	PagedResourcesAssembler<UserDTO> assembler;

	public ResponseEntity<TokenDTO> signIn(AccountCredentialsDTO credentials) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword()));

		var user = repository.findByUsername(credentials.getUsername());
		if (user == null) {
			throw new UsernameNotFoundException("Username " + credentials.getUsername() + " not found!");
		}

		var userDTO = parseObject(user, UserDTO.class);
		var token = tokenProvider.createAccessToken(credentials.getUsername(), user.getRoles(), userDTO);
		return ResponseEntity.ok(token);
	}

	public ResponseEntity<TokenDTO> refreshToken(String username, String refreshToken) {
		var user = repository.findByUsername(username);
		TokenDTO token;
		if (user != null) {
			token = tokenProvider.refreshToken(refreshToken);
		} else {
			throw new UsernameNotFoundException("Username " + username + " not found!");
		}
		return ResponseEntity.ok(token);
	}

	@Transactional
	public AccountCredentialsDTO create(AccountCredentialsDTO user) {

		if (user == null) {
			throw new RequiredObjectIsNullException();
		}

		logger.info("Creating one new User!");

		UserEntity entity = new UserEntity();
		entity.setFullName(user.getFullname());
		entity.setUserName(user.getUsername());
		entity.setPassword(generateHashedPassword(user.getPassword()));

		entity.setAccountNonExpired(true);
		entity.setAccountNonLocked(true);
		entity.setCredentialsNonExpired(true);
		entity.setEnabled(true);

		entity.setImage(user.getImage());

		// ROLES (proteção)
		if (user.getRoles() != null && !user.getRoles().isEmpty()) {
			List<PermissionEntity> permissions = permissionRepository.findByDescriptionIn(user.getRoles());

			entity.setPermissions(permissions);
		}

		UserEntity saved = repository.save(entity);

		return new AccountCredentialsDTO(saved.getUsername(), null, // nunca devolver password
				saved.getFullName());
	}

	private String generateHashedPassword(String password) {

		PasswordEncoder pbkdf2Encoder = new Pbkdf2PasswordEncoder("", 8, 185000,
				Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);

		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put("pbkdf2", pbkdf2Encoder);
		DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder("pbkdf2", encoders);

		passwordEncoder.setDefaultPasswordEncoderForMatches(pbkdf2Encoder);
		return passwordEncoder.encode(password);
	}

	public void changePassword(ChangePasswordDTO dto) {
		// Validações
		if (dto == null || dto.getUsername() == null || dto.getOldPassword() == null || dto.getNewPassword() == null) {
			throw new RequiredObjectIsNullException("Dados inválidos para alterar a senha!");
		}

		var user = repository.findByUsername(dto.getUsername());
		if (user == null) {
			throw new UsernameNotFoundException("Usuário " + dto.getUsername() + " não encontrado!");
		}

		if (dto.getOldPassword().equals(dto.getNewPassword())) {
			throw new IllegalArgumentException("A nova senha não pode ser igual à antiga!");
		}

		// Verificar senha antiga usando o encoder injetado
		if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Senha antiga incorreta!");
		}

		// Validar força da nova senha
		//validatePasswordStrength(dto.getNewPassword());

		// Codificar e salvar nova senha
		user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
		repository.save(user);
	}

	public UserDTO update(UserDTO user) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		System.out.println("AUTH: " + SecurityContextHolder.getContext().getAuthentication());
	    if (auth == null || !auth.isAuthenticated()) {
	        throw new RuntimeException("Usuário não autenticado");
	    }
		
		logger.info("Atualizando User!");
		UserEntity entity = repository.findByUsername(user.getUserName());
		if (entity == null) {
			throw new UsernameNotFoundException("Username " + user.getUserName() + " not found!");
		}

		entity.setImage(user.getImage());

		return parseObject(repository.save(entity), UserDTO.class);
	}

	private void validatePasswordStrength(String password) {
		if (password.length() < 8) {
			throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres");
		}
		if (!password.matches(".*[A-Z].*")) {
			throw new IllegalArgumentException("A senha deve conter pelo menos uma letra maiúscula");
		}
		if (!password.matches(".*[a-z].*")) {
			throw new IllegalArgumentException("A senha deve conter pelo menos uma letra minúscula");
		}
		if (!password.matches(".*\\d.*")) {
			throw new IllegalArgumentException("A senha deve conter pelo menos um número");
		}
		if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
			throw new IllegalArgumentException("A senha deve conter pelo menos um caractere especial");
		}
	}
	
	public PagedModel<EntityModel<UserDTO>> findAll(Pageable pageable, String search) {

		Page<UserEntity> user;

		if (search != null && !search.isBlank()) {
			user = repository.search(search.toLowerCase(), EntityState.ACTIVE, pageable);
		} else {
			user = repository.findAll(pageable, EntityState.ACTIVE);
		}

		return buildPagedModel(pageable, user, search);
	}
	
	private PagedModel<EntityModel<UserDTO>> buildPagedModel(Pageable pageable, Page<UserEntity> userEntity,
			String search) {

		var users = userEntity.map(u -> {
			var dto = parseObject(u, UserDTO.class);
			return dto;
		});

		// Extrair sort corretamente
		String sortField = pageable.getSort().stream().findFirst().map(order -> order.getProperty()).orElse("name");

		String direction = pageable.getSort().stream().findFirst()
				.map(order -> order.getDirection().name().toLowerCase()).orElse("asc");

		Link findAllLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(AuthController.class)
				.findAll(pageable.getPageNumber(), pageable.getPageSize(), direction, sortField, search)).withSelfRel();

		return assembler.toModel(users, findAllLink);
	}
}
