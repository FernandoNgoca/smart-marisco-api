package mz.com.sgp.services;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import mz.com.sgp.repository.*;
import mz.com.sgp.model.UserEntity;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.data.dto.UserDTO;
import mz.com.sgp.security.jwt.JwtTokenProvider;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {
    private final UserRepository users;
    private final PermissionRepository permissions;
    private final RefreshTokenRepository tokens;
    public AdminUserService(UserRepository users,PermissionRepository permissions,RefreshTokenRepository tokens){this.users=users;this.permissions=permissions;this.tokens=tokens;}
    public record Edit(String fullName,List<String> roles) {}
    public record Enabled(Boolean enabled) {}
    @Transactional(readOnly=true)
    public UserDTO detail(Long id){return dto(users.findById(id).filter(u->u.getStatus()==EntityState.ACTIVE).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Utilizador não encontrado")));}
    @Transactional
    public UserDTO edit(Long id,Edit request){
        if(request==null || request.fullName()==null || request.fullName().isBlank() || request.fullName().trim().length()>255 || request.roles()==null || request.roles().isEmpty())throw error(HttpStatus.BAD_REQUEST,"Indique o nome e pelo menos uma permissão");
        var roles=request.roles().stream().distinct().toList();
        if(!new HashSet<>(List.of("ROLE_ADMIN","ROLE_MANAGER","ROLE_USER")).containsAll(roles))throw error(HttpStatus.BAD_REQUEST,"Permissões inválidas");
        var actor=actor(); var target=target(id);
        if(actor.getId().equals(id) && !roles.contains("ROLE_ADMIN"))throw error(HttpStatus.CONFLICT,"Não pode retirar a sua própria permissão de administrador");
        var selected=permissions.findByDescriptionIn(roles);
        if(selected.size()!=roles.size())throw error(HttpStatus.BAD_REQUEST,"Permissões indisponíveis");
        boolean changed=!new HashSet<>(target.getRoles()).equals(new HashSet<>(roles));
        target.setFullName(request.fullName().trim()); target.setPermissions(selected);
        if(changed)revoke(target);
        return dto(users.save(target));
    }
    @Transactional
    public UserDTO enabled(Long id,Enabled request){
        if(request==null||request.enabled()==null)throw error(HttpStatus.BAD_REQUEST,"Estado obrigatório");
        var actor=actor();var target=target(id);
        if(actor.getId().equals(id) && !request.enabled())throw error(HttpStatus.CONFLICT,"Não pode desativar a sua própria conta");
        if(target.isEnabled()!=request.enabled()){target.setEnabled(request.enabled());revoke(target);}
        return dto(users.save(target));
    }
    private UserEntity actor(){
        // Serialize administrative account changes so concurrent administrators cannot remove each other's access.
        permissions.lockAdminRole().orElseThrow(()->error(HttpStatus.CONFLICT,"Permissão de administrador indisponível"));
        var actor=users.findForUpdateByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        JwtTokenProvider.requireActive(actor);
        if(!actor.getRoles().contains("ROLE_ADMIN"))throw error(HttpStatus.FORBIDDEN,"Acesso negado");
        return actor;
    }
    private UserEntity target(Long id){return users.lockById(id).filter(u->u.getStatus()==EntityState.ACTIVE).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Utilizador não encontrado"));}
    private void revoke(UserEntity user){user.setTokenVersion(user.getTokenVersion()+1);tokens.revokeForUser(user.getId());}
    private UserDTO dto(UserEntity user){var dto=new UserDTO();dto.setId(user.getId());dto.setUserName(user.getUsername());dto.setFullName(user.getFullName());dto.setImage(user.getImage());dto.setRoles(user.getRoles());dto.setEnabled(user.isEnabled());return dto;}
    private ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
