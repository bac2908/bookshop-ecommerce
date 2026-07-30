package vn.fs.backend.application.dto;

public class DashboardSummary {
    private long newUsers7d;
    private long salesQty7d;
    private long orders7d;
    private double income7d;

    public DashboardSummary(long newUsers7d, long salesQty7d, long orders7d, double income7d) {
        this.newUsers7d = newUsers7d;
        this.salesQty7d = salesQty7d;
        this.orders7d = orders7d;
        this.income7d = income7d;
    }
    public long getNewUsers7d() { return newUsers7d; }
    public long getSalesQty7d() { return salesQty7d; }
    public long getOrders7d() { return orders7d; }
}
