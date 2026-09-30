package mz.com.sgp.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import mz.com.sgp.data.dto.DashboardSummaryDTO;
import mz.com.sgp.services.DashboardService;

@RestController
@RequestMapping("api/dashboard/v1")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('MANAGER') and !hasRole('ADMIN')")
    public DashboardSummaryDTO summary() { return dashboard.summary(); }
}
