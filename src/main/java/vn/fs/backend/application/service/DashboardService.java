package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fs.backend.application.dto.DailyPoint;
import vn.fs.backend.application.dto.DashboardSummary;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final OrderDetailRepository orderDetailRepo;

    public DashboardSummary loadSummary(int days) {

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
        LocalDate from = LocalDate.now().minusDays(days - 1);
        Map<LocalDate, Double> map = orderRepo.incomeByDateFrom(from).stream()
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r[0]).toLocalDate(),
                        r -> ((Number) r[1]).doubleValue()
                ));
        return fillMissingDays(from, days, map);
    }

    public List<DailyPoint> salesQtySeries(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1);
        Map<LocalDate, Double> map = orderDetailRepo.soldQtyByDateFrom(from).stream()
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r[0]).toLocalDate(),
                        r -> ((Number) r[1]).doubleValue()
                ));
        return fillMissingDays(from, days, map);
    }

    public List<DailyPoint> newUsersSeries(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1);
        Map<LocalDate, Double> map = userRepo.newUsersByDateFrom(from).stream() // cần có ở UserRepository
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r[0]).toLocalDate(),
                        r -> ((Number) r[1]).doubleValue()
                ));
        return fillMissingDays(from, days, map);
    }

    private List<DailyPoint> fillMissingDays(LocalDate from, int days, Map<LocalDate, Double> map) {
        List<DailyPoint> list = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            list.add(new DailyPoint(d, map.getOrDefault(d, 0d)));
        }
        return list;
    }
}
