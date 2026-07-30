package vn.fs.backend.presentation.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.fs.backend.application.dto.DailyPoint;
import vn.fs.backend.application.dto.DashboardSummary;
import vn.fs.backend.application.service.DashboardService;

import java.util.List;

@RestController
@RequestMapping("/admin/api/dashboard")
@RequiredArgsConstructor
public class AdminDashboardApi {

    private final DashboardService service;

    // GET /admin/api/dashboard/summary?days=7
    @GetMapping("/summary")
    public DashboardSummary summary(@RequestParam(defaultValue = "7") int days) {
        return service.loadSummary(days);
    }

    // GET /admin/api/dashboard/income?days=10
    @GetMapping("/income")
    public List<DailyPoint> income(@RequestParam(defaultValue = "10") int days) {
        return service.incomeSeries(days);
    }

    // GET /admin/api/dashboard/salesQty?days=12
    @GetMapping("/salesQty")
    public List<DailyPoint> salesQty(@RequestParam(defaultValue = "12") int days) {
        return service.salesQtySeries(days);
    }

    // (tuỳ chọn) New users để vẽ thêm line khác
    @GetMapping("/newUsers")
    public List<DailyPoint> newUsers(@RequestParam(defaultValue = "12") int days) {
        return service.newUsersSeries(days);
    }
}
