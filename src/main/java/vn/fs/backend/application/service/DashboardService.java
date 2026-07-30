package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fs.backend.application.dto.DailyPoint;
import vn.fs.backend.application.dto.DashboardSnapshot;
import vn.fs.backend.application.dto.DashboardSummary;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final OrderDetailRepository orderDetailRepo;

    public DashboardSummary loadSummary(int days) {
        validateDays(days);
        LocalDate from = LocalDate.now().minusDays(days - 1);

        long newUsers   = userRepo.countNewUsersFrom(from);
        long orders     = orderRepo.countOrdersFrom(from);
        long salesQty   = orderDetailRepo.totalSoldQtyFrom(from);
        double income   = orderRepo.incomeByDateFrom(from).stream()
                .map(r -> ((Number) r[1]).doubleValue())
                .reduce(0d, Double::sum);

        return new DashboardSummary(newUsers, salesQty, orders, income);
    }

    public List<DailyPoint> incomeSeries(int days) {
        validateDays(days);
        LocalDate from = LocalDate.now().minusDays(days - 1);
        Map<LocalDate, Double> map = orderRepo.incomeByDateFrom(from).stream()
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r[0]).toLocalDate(),
                        r -> ((Number) r[1]).doubleValue()
                ));
        return fillMissingDays(from, days, map);
    }

    public List<DailyPoint> salesQtySeries(int days) {
        validateDays(days);
        LocalDate from = LocalDate.now().minusDays(days - 1);
        Map<LocalDate, Double> map = orderDetailRepo.soldQtyByDateFrom(from).stream()
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r[0]).toLocalDate(),
                        r -> ((Number) r[1]).doubleValue()
                ));
        return fillMissingDays(from, days, map);
    }

    public List<DailyPoint> newUsersSeries(int days) {
        validateDays(days);
        LocalDate from = LocalDate.now().minusDays(days - 1);
        Map<LocalDate, Double> map = userRepo.newUsersByDateFrom(from).stream() // cần có ở UserRepository
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r[0]).toLocalDate(),
                        r -> ((Number) r[1]).doubleValue()
                ));
        return fillMissingDays(from, days, map);
    }

    public List<DailyPoint> orderSeries(int days) {
        validateDays(days);
        LocalDate from = LocalDate.now().minusDays(days - 1);
        Map<LocalDate, Double> map = orderRepo.ordersCountByDateFrom(from).stream()
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r[0]).toLocalDate(),
                        r -> ((Number) r[1]).doubleValue()
                ));
        return fillMissingDays(from, days, map);
    }

    public DashboardSnapshot snapshot(int days) {
        validateDays(days);
        return new DashboardSnapshot(
                days,
                loadSummary(days),
                incomeSeries(days),
                orderSeries(days),
                salesQtySeries(days),
                newUsersSeries(days));
    }

    private List<DailyPoint> fillMissingDays(LocalDate from, int days, Map<LocalDate, Double> map) {
        List<DailyPoint> list = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            list.add(new DailyPoint(d, map.getOrDefault(d, 0d)));
        }
        return list;
    }

    private void validateDays(int days) {
        if (days < 1 || days > 366) {
            throw new IllegalArgumentException("days phải nằm trong khoảng 1 đến 366");
        }
    }
}
