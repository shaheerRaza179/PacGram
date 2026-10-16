package com.pacgram.admin.controller;

import com.pacgram.admin.dto.AdminAuditDto;
import com.pacgram.admin.dto.AdminBlockDto;
import com.pacgram.admin.dto.AdminDashboardDto;
import com.pacgram.admin.dto.AdminPageDto;
import com.pacgram.admin.dto.AdminPostDto;
import com.pacgram.admin.dto.AdminReportDto;
import com.pacgram.admin.dto.AdminUserDetailDto;
import com.pacgram.admin.dto.AdminUserDto;
import com.pacgram.admin.dto.UpdateReportRequest;
import com.pacgram.admin.dto.UpdateUserStatusRequest;
import com.pacgram.admin.service.AdminManagementService;
import com.pacgram.moderation.service.ReportService;
import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminManagementService adminService;
    private final ReportService reportService;

    public AdminController(AdminManagementService adminService, ReportService reportService) {
        this.adminService = adminService;
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminDashboardDto> overview() {
        return ApiResponse.success(adminService.overview());
    }

    @GetMapping("/users")
    public ApiResponse<AdminPageDto<AdminUserDto>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q) {
        return ApiResponse.success(adminService.users(page, size, q));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserDetailDto> userDetail(@PathVariable long userId) {
        return ApiResponse.success(adminService.userDetail(userId));
    }

    @GetMapping("/users/{userId}/posts")
    public ApiResponse<AdminPageDto<AdminPostDto>> userPosts(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.userPosts(userId, page, size));
    }

    @GetMapping("/users/{userId}/reports")
    public ApiResponse<AdminPageDto<AdminReportDto>> userReports(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.userReports(userId, page, size));
    }

    @GetMapping("/users/{userId}/blocks")
    public ApiResponse<AdminPageDto<AdminBlockDto>> userBlocks(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.userBlocks(userId, page, size));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<String> updateUserStatus(
            @AuthenticationPrincipal AccountPrincipal admin,
            @PathVariable long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        adminService.setUserStatus(admin.username(), userId, request.status());
        return ApiResponse.success("User status updated.");
    }

    @GetMapping("/reports")
    public ApiResponse<AdminPageDto<AdminReportDto>> reports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.reports(page, size, status));
    }

    @GetMapping("/reports/{reportId}/evidence/{evidenceId}")
    public ResponseEntity<Resource> reportEvidence(
            @AuthenticationPrincipal AccountPrincipal admin,
            @PathVariable long reportId,
            @PathVariable long evidenceId) {
        Resource resource = reportService.evidenceFile(reportId, evidenceId, admin);
        String contentType = reportService.evidence(reportId, admin).stream()
                .filter(item -> item.id().equals(evidenceId))
                .findFirst().orElseThrow().contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @PatchMapping("/reports/{reportId}")
    public ApiResponse<String> updateReport(
            @AuthenticationPrincipal AccountPrincipal admin,
            @PathVariable long reportId,
            @Valid @RequestBody UpdateReportRequest request) {
        adminService.updateReport(admin.username(), reportId, request);
        return ApiResponse.success("Report updated.");
    }

    @GetMapping("/audit")
    public ApiResponse<AdminPageDto<AdminAuditDto>> audit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.success(adminService.audit(page, size));
    }
}
