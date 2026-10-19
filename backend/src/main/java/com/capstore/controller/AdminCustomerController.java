package com.capstore.controller;

import com.capstore.dto.AdminDtos.*;
import com.capstore.dto.PageResponse;
import com.capstore.entity.Role;
import com.capstore.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCustomerController {
    private final AdminService admin;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() { return admin.dashboard(); }

    @GetMapping("/customers")
    public PageResponse<CustomerResponse> customers(@RequestParam(required = false) Role role,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return admin.customers(role, page, size);
    }

    @PutMapping("/customers/{id}/approve-business")
    public CustomerResponse approve(@PathVariable Long id) { return admin.approveBusiness(id); }

    @PutMapping("/customers/{id}/enabled")
    public CustomerResponse enabled(@PathVariable Long id, @RequestParam boolean value) { return admin.setEnabled(id, value); }
}
