package com.hansana.helpdesk.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class RoleTestController {

    @GetMapping("/user")
    public ResponseEntity<Map<String, String>> userAccess() {
        return ResponseEntity.ok(Map.of("message", "USER access granted"));
    }

    @GetMapping("/agent")
    public ResponseEntity<Map<String, String>> agentAccess() {
        return ResponseEntity.ok(Map.of("message", "SUPPORT_AGENT access granted"));
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, String>> adminAccess() {
        return ResponseEntity.ok(Map.of("message", "ADMIN access granted"));
    }
}
