package vn.fs.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.infrastructure.persistence.CartRepository;

import java.util.Date;

// Job dọn dẹp giỏ hàng:
// Đánh dấu EXPIRED cho giỏ quá hạn
// Xoá các giỏ quá hạn và rỗng
//
@Component
@RequiredArgsConstructor
public class CartCleanupJob {

    private final CartRepository cartRepository;

    // Chạy mỗi ngày lúc 03:00 sáng
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void run() {
        Date now = new Date();
        cartRepository.markExpired(now);
        cartRepository.deleteExpired(now);
    }
}
