package vn.fs.entities;

import lombok.Getter; import lombok.Setter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Getter @Setter
@Entity @Table(name="order_payments")
public class OrderPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false)
    private Order order;

    @Column(nullable=false)
    private String provider; // "PAYPAL"

    private String method;

    @Column(name="external_order_id")
    private String externalOrderId; // PayPal order id

    @Column(name="external_capture_id")
    private String externalCaptureId; // PayPal capture id

    @Column(nullable=false, length=3)
    private String currency = "USD";

    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal amount; // USD

    @Column(precision=18, scale=6)
    private BigDecimal exchangeRate; // VND/USD

    private String status; // COMPLETED / REFUNDED ...

    private String payerEmail;

    private String message;

    @Temporal(TemporalType.TIMESTAMP)
    private Date paymentTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "refund_id")
    private String refundId;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount; // USD

    @Column(name = "refund_currency", length = 3)
    private String refundCurrency; // USD

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "refunded_at")
    private Date refundedAt;
}
