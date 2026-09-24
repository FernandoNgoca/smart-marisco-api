package mz.com.sgp.services;

import static mz.com.sgp.mapper.ObjectMapper.parseObject;

import java.util.List;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import mz.com.sgp.repository.RefreshTokenRepository;
import mz.com.sgp.repository.UserRepository;
import mz.com.sgp.security.PasswordPolicy;
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

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Transactional
    public ResponseEntity<TokenDTO> signIn(AccountCredentialsDTO credentials) {
        var user = repository.findForUpdateByUsername(credentials.getUsername());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword()));
        JwtTokenProvider.requireActive(user);
        return ResponseEntity.ok(tokenProvider.createAccessToken(user.getUsername()));
    }

    public ResponseEntity<TokenDTO> refreshToken(String username, String refreshToken) {
        return ResponseEntity.ok(tokenProvider.refreshToken(username, refreshToken));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AccountCredentialsDTO create(AccountCredentialsDTO user) {

        if (user == null) {
            throw new RequiredObjectIsNullException();
        }

        if (user.getUsername() == null || user.getUsername().isBlank()
                || user.getUsername().length() > 20) {
            throw new IllegalArgumentException("Nome de utilizador deve ter entre 1 e 20 caracteres");
        }
        PasswordPolicy.validate(user.getPassword());
        logger.info("Creating one new User!");

        UserEntity entity = new UserEntity();
        entity.setFullName(user.getFullname());
        entity.setUserName(user.getUsername());
        entity.setPassword(passwordEncoder.encode(user.getPassword()));

        entity.setAccountNonExpired(true);
        entity.setAccountNonLocked(true);
        entity.setCredentialsNonExpired(true);
        entity.setEnabled(true);

        entity.setImage(user.getImage());

        List<String> roles = user.getRoles() == null || user.getRoles().isEmpty()
                ? List.of("ROLE_USER") : user.getRoles().stream().distinct().toList();
        if (!List.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_USER").containsAll(roles)) {
            throw new IllegalArgumentException("Permissões inválidas");
        }
        List<PermissionEntity> permissions = permissionRepository.findByDescriptionIn(roles);
        if (permissions.size() != roles.size()) throw new IllegalArgumentException("Permissões inválidas");
        entity.setPermissions(permissions);

        UserEntity saved = repository.save(entity);

        return new AccountCredentialsDTO(saved.getUsername(), null, // nunca devolver password
                saved.getFullName());
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Acesso negado");
        }
        return auth.getName();
    }

    @Transactional
    public void changePassword(ChangePasswordDTO dto) {
        if (dto == null || dto.getOldPassword() == null || dto.getNewPassword() == null) {
            throw new IllegalArgumentException("Dados inválidos para alterar a senha");
        }
        String username = currentUsername();
        if (!username.equals(dto.getUsername())) throw new AccessDeniedException("Acesso negado");
        var user = repository.findForUpdateByUsername(username);
        JwtTokenProvider.requireActive(user);
        if (dto.getOldPassword().length() > 128
                || !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        PasswordPolicy.validate(dto.getNewPassword());
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new IllegalArgumentException("A nova senha não pode ser igual à antiga");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        repository.save(user);
        refreshTokens.revokeForUser(user.getId());
    }

    @Transactional
    public UserDTO update(UserDTO user) {
        String username = currentUsername();
        if (user == null || !username.equals(user.getUserName())) {
            throw new AccessDeniedException("Acesso negado");
        }
        UserEntity entity = repository.findForUpdateByUsername(username);
        JwtTokenProvider.requireActive(entity);
        entity.setImage(user.getImage());
        return parseObject(repository.save(entity), UserDTO.class);
    }

    @PreAuthorize("hasRole('ADMIN')")
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
