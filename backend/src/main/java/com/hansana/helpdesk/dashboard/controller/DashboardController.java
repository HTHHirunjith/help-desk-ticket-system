package com.hansana.helpdesk.dashboard.controller;

import com.hansana.helpdesk.auth.security.UserPrincipal;
import com.hansana.helpdesk.dashboard.dto.AdminDashboardResponse;
import com.hansana.helpdesk.dashboard.dto.AgentDashboardResponse;
import com.hansana.helpdesk.dashboard.dto.UserDashboardResponse;
import com.hansana.helpdesk.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/user")
    public ResponseEntity<UserDashboardResponse> getUserDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserDashboardResponse response = dashboardService.getUserDashboard(principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agent")
    public ResponseEntity<AgentDashboardResponse> getAgentDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentDashboardResponse response = dashboardService.getAgentDashboard(principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        AdminDashboardResponse response = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(response);
    }
}
