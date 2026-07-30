package vn.fs.backend.application.dto;

import java.util.List;

public record DashboardSnapshot(
        int days,
        DashboardSummary summary,
        List<DailyPoint> income,
        List<DailyPoint> orders,
        List<DailyPoint> salesQuantity,
        List<DailyPoint> newUsers) {
}
