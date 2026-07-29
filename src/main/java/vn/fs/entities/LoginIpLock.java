package vn.fs.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_ip_lock", indexes = {
        @Index(name = "idx_ip", columnList = "ip")
})
public class LoginIpLock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 45, nullable = false)
    private String ip;

    @Column(nullable = false)
    private int failedAttempt = 0;

    private LocalDateTime lockedUntil;
    private LocalDateTime lastFailedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public int getFailedAttempt() { return failedAttempt; }
    public void setFailedAttempt(int failedAttempt) { this.failedAttempt = failedAttempt; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public LocalDateTime getLastFailedAt() { return lastFailedAt; }
    public void setLastFailedAt(LocalDateTime lastFailedAt) { this.lastFailedAt = lastFailedAt; }
}
