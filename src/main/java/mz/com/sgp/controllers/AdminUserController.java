package mz.com.sgp.controllers;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import mz.com.sgp.services.AdminUserService;
import mz.com.sgp.data.dto.UserDTO;
@RestController
@RequestMapping("/auth/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service){this.service=service;}
    @GetMapping("/{id}") public UserDTO detail(@PathVariable Long id){return service.detail(id);}
    @PutMapping("/{id}") public UserDTO edit(@PathVariable Long id,@RequestBody AdminUserService.Edit request){return service.edit(id,request);}
    @PatchMapping("/{id}/enabled") public UserDTO enabled(@PathVariable Long id,@RequestBody AdminUserService.Enabled request){return service.enabled(id,request);}
}
