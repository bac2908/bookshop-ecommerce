package vn.fs.backend.presentation.api.v1.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import vn.fs.backend.application.dto.ReportGrouping;
import vn.fs.backend.application.dto.ReportRow;

@Schema(name = "AdminReportResponse")
public record AdminReportResponse(
        ReportGrouping groupBy,
        List<ReportRow> rows) {
}
