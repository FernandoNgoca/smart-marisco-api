package mz.com.sgp.security;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import mz.com.sgp.repository.*;
import mz.com.sgp.model.*;
import mz.com.sgp.security.jwt.JwtTokenProvider;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserTests {
 @Autowired MockMvc mvc;
 @Autowired UserRepository users;
 @Autowired PermissionRepository permissions;
 @Autowired JwtTokenProvider tokens;
 UserEntity admin,target;
 @BeforeEach void setup(){admin=create("admin-"+UUID.randomUUID().toString().substring(0,8),"ROLE_ADMIN");target=create("user-"+UUID.randomUUID().toString().substring(0,8),"ROLE_USER");create("manager-"+UUID.randomUUID().toString().substring(0,8),"ROLE_MANAGER");}
 UserEntity create(String name,String role){
  var found=permissions.findByDescriptionIn(List.of(role));PermissionEntity permission;
  if(found.isEmpty()){permission=new PermissionEntity();permission.setDescription(role);permission=permissions.save(permission);}else permission=found.get(0);
  var entity=new UserEntity();entity.setUserName(name);entity.setFullName(name);entity.setPassword("not-exposed");entity.setEnabled(true);entity.setAccountNonExpired(true);entity.setAccountNonLocked(true);entity.setCredentialsNonExpired(true);entity.setPermissions(new ArrayList<>(List.of(permission)));return users.saveAndFlush(entity);
 }
 @Test void editAndDetailsDoNotExposePasswords()throws Exception{
  mvc.perform(get("/auth/users/"+target.getId()).with(user(admin.getUsername()).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
  mvc.perform(put("/auth/users/"+target.getId()).with(user(admin.getUsername()).roles("ADMIN")).contentType("application/json").content("{\"fullName\":\"Novo nome\",\"roles\":[\"ROLE_MANAGER\"]}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Novo nome")).andExpect(jsonPath("$.roles[0]").value("ROLE_MANAGER")).andExpect(jsonPath("$.password").doesNotExist());
  org.junit.jupiter.api.Assertions.assertEquals(1,target.getTokenVersion());
 }
 @Test void disablingRevokesSessionsAndAllowsReactivation()throws Exception{
  var pair=tokens.createAccessToken(target.getUsername());
  mvc.perform(patch("/auth/users/"+target.getId()+"/enabled").with(user(admin.getUsername()).roles("ADMIN")).contentType("application/json").content("{\"enabled\":false}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
  mvc.perform(get("/api/stock/v1").header("Authorization","Bearer "+pair.getAccessToken())).andExpect(status().isUnauthorized());
  mvc.perform(patch("/auth/users/"+target.getId()+"/enabled").with(user(admin.getUsername()).roles("ADMIN")).contentType("application/json").content("{\"enabled\":true}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
  mvc.perform(get("/auth").param("search",target.getUsername()).with(user(admin.getUsername()).roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.user[0].enabled").value(true));
 }
 @Test void protectsOwnAdminAccess()throws Exception{
  mvc.perform(patch("/auth/users/"+admin.getId()+"/enabled").with(user(admin.getUsername()).roles("ADMIN")).contentType("application/json").content("{\"enabled\":false}")).andExpect(status().isConflict());
  mvc.perform(put("/auth/users/"+admin.getId()).with(user(admin.getUsername()).roles("ADMIN")).contentType("application/json").content("{\"fullName\":\"Admin\",\"roles\":[\"ROLE_USER\"]}")).andExpect(status().isConflict());
 }
 @Test void otherRolesCannotManageUsers()throws Exception{
  for(String role:List.of("USER","MANAGER")){
   mvc.perform(get("/auth/users/"+target.getId()).with(user("someone").roles(role))).andExpect(status().isForbidden());
   mvc.perform(put("/auth/users/"+target.getId()).with(user("someone").roles(role)).contentType("application/json").content("{}")).andExpect(status().isForbidden());
   mvc.perform(patch("/auth/users/"+target.getId()+"/enabled").with(user("someone").roles(role)).contentType("application/json").content("{\"enabled\":false}")).andExpect(status().isForbidden());
  }
 }
}
