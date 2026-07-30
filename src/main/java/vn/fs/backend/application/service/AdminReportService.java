package vn.fs.backend.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.fs.backend.application.dto.ReportGrouping;
import vn.fs.backend.application.dto.ReportRow;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final OrderDetailRepository orderDetailRepository;

    public List<ReportRow> report(ReportGrouping grouping) {
        List<Object[]> rows = switch (grouping) {
            case PRODUCT -> orderDetailRepository.repo();
            case CATEGORY -> orderDetailRepository.repoWhereCategory();
            case YEAR -> orderDetailRepository.repoWhereYear();
            case MONTH -> orderDetailRepository.repoWhereMonth();
            case QUARTER -> orderDetailRepository.repoWhereQUARTER();
            case CUSTOMER -> orderDetailRepository.reportCustomer();
        };
        return rows.stream().map(this::toRow).toList();
    }

    private ReportRow toRow(Object[] row) {
        return new ReportRow(
                String.valueOf(row[0]),
                number(row[1]).longValue(),
                decimal(row[2]),
                decimal(row[3]),
                decimal(row[4]),
                decimal(row[5]));
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : 0;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
