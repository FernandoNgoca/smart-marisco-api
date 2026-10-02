package mz.com.sgp.controllers;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import mz.com.sgp.services.DashboardOverviewService;
@RestController
@RequestMapping("/api/dashboard/v1")
@PreAuthorize("hasRole('MANAGER') and !hasRole('ADMIN')")
public class DashboardOverviewController {
    private final DashboardOverviewService service;
    public DashboardOverviewController(DashboardOverviewService service) { this.service=service; }
    @GetMapping("/overview")
    public DashboardOverviewService.Overview overview(
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) { return service.overview(from,to); }
}
