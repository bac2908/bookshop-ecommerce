package vn.fs.backend.application.dto;

import java.time.LocalDate;

public class DailyPoint {
    private LocalDate date;
    private Double value;

    public DailyPoint(LocalDate date, Double value) {
        this.date = date; this.value = value;
    }
    public LocalDate getDate() { return date; }
    public Double getValue() { return value; }
}
