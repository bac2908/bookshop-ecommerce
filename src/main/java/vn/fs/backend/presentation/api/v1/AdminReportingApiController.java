package vn.fs.backend.presentation.api.v1;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.dto.DashboardSnapshot;
import vn.fs.backend.application.dto.ReportGrouping;
import vn.fs.backend.application.service.AdminReportService;
import vn.fs.backend.application.service.DashboardService;
import vn.fs.backend.presentation.api.v1.dto.AdminReportResponse;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin reporting", description = "Dashboard và báo cáo quản trị")
@SecurityRequirement(name = "sessionCookie")
public class AdminReportingApiController {

    private final DashboardService dashboardService;
    private final AdminReportService adminReportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Snapshot dashboard trong một khoảng ngày")
    public DashboardSnapshot dashboard(
            @RequestParam(defaultValue = "7")
            @Min(value = 1, message = "days phải lớn hơn hoặc bằng 1")
            @Max(value = 366, message = "days tối đa là 366")
            int days) {
        return dashboardService.snapshot(days);
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Báo cáo doanh số chỉ tính đơn COMPLETED")
    public AdminReportResponse report(
            @RequestParam(defaultValue = "PRODUCT")
            ReportGrouping groupBy) {
        return new AdminReportResponse(
                groupBy,
                adminReportService.report(groupBy));
    }
}
