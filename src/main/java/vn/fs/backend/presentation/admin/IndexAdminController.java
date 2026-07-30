package vn.fs.backend.presentation.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.fs.backend.application.dto.DailyPoint;
import vn.fs.backend.application.dto.DashboardSummary;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.application.service.DashboardService;

import java.security.Principal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class IndexAdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final DashboardService dashboardService;

    @ModelAttribute("user")
    public User user(Model model, Principal principal, User user) {
        if (principal != null) {
            model.addAttribute("user", new User());
            user = userRepository.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }
        return user;
    }

    @GetMapping("/home")
    public String index(Model model) {
        final int DAYS = 7;
        final LocalDate from = LocalDate.now().minusDays(DAYS - 1);

        // ---- Summary (7 ngày) ----
        DashboardSummary s = dashboardService.loadSummary(DAYS);

        // KPIs cho 3 vòng tròn
        Map<String, Number> kpis = new HashMap<>();
        kpis.put("newUsers",     s.getNewUsers7d());
        kpis.put("orders7d",     s.getOrders7d());
        // “subscribers” dùng cho SL bán 7 ngày để hiển thị đúng nhãn “SL bán (7 ngày)”
        kpis.put("subscribers",  s.getSalesQty7d());
        model.addAttribute("kpis", kpis);

        // ---- Income series (bar) ----
        List<DailyPoint> income = dashboardService.incomeSeries(DAYS);
        List<String> incomeLabels = income.stream().map(dp -> dp.getDate().toString()).collect(Collectors.toList());
        List<Double> incomeValues = income.stream().map(DailyPoint::getValue).collect(Collectors.toList());
        model.addAttribute("incomeLabels", incomeLabels);
        model.addAttribute("incomeValues", incomeValues);

        // ---- Orders count series (line “Daily Sales”) ----
        List<Object[]> rows = orderRepository.ordersCountByDateFrom(from);
        Map<LocalDate, Long> ordersMap = new HashMap<>();
        for (Object[] r : rows) {
            LocalDate d = ((java.sql.Date) r[0]).toLocalDate();
            long cnt = ((Number) r[1]).longValue();
            ordersMap.put(d, cnt);
        }
        List<String> ordersLabels = new ArrayList<>(DAYS);
        List<Long>   ordersValues = new ArrayList<>(DAYS);
        for (int i = 0; i < DAYS; i++) {
            LocalDate d = from.plusDays(i);
            ordersLabels.add(d.toString());
            ordersValues.add(ordersMap.getOrDefault(d, 0L));
        }
        model.addAttribute("ordersLabels", ordersLabels);
        model.addAttribute("ordersValues", ordersValues);

        // Sparkline mini “transactionsSmall”
        model.addAttribute("transactionsSmall", ordersValues);

        // ---- Tổng thu/chi hiển thị ----
        double totalIncome7d = incomeValues.stream().mapToDouble(Double::doubleValue).sum();
        String totalIncomeFormatted = formatCurrency(totalIncome7d);
        String totalSpendFormatted  = formatCurrency(0); // thay bằng số thực tế nếu có

        model.addAttribute("totalIncomeFormatted", totalIncomeFormatted);
        model.addAttribute("totalSpendFormatted", totalSpendFormatted);

        // Headline doanh số ngày gần nhất = SỐ ĐƠN (không format tiền)
        long lastDayOrders = ordersValues.isEmpty() ? 0 : ordersValues.get(ordersValues.size() - 1);
        model.addAttribute("dailySalesHeadline", String.valueOf(lastDayOrders));

        // Tỉ lệ tăng/giảm giao dịch so với ngày đầu
        long firstDayOrders = ordersValues.isEmpty() ? 0 : ordersValues.get(0);
        String delta = "+0%";
        if (firstDayOrders > 0) {
            double percent = ((double) (lastDayOrders - firstDayOrders) / firstDayOrders) * 100.0;
            delta = String.format("%+,.0f%%", percent);
        } else if (lastDayOrders > 0) {
            delta = "+100%";
        }
        model.addAttribute("transactionsDelta", delta);

        // Tổng giao dịch (tổng số đơn 7 ngày)
        long transactionsCount = ordersValues.stream().mapToLong(Long::longValue).sum();
        model.addAttribute("transactionsCount", transactionsCount);

        // Nhãn khoảng thời gian (Việt hoá)
        model.addAttribute("rangeLabel", "7 ngày gần nhất");

        return "admin/pages/dashboard";
    }

    private String formatCurrency(double vnd) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(vnd);
    }
}
