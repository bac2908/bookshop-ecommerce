package vn.fs.backend.application.service.impl;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ExchangeRateService {

    private final String API_URL = "https://open.er-api.com/v6/latest/USD";

    // Biến cache
    private double cachedRate = 24000.0;
    private LocalDateTime lastUpdated = LocalDateTime.MIN;

    public double getVNDExchangeRate() {

        if (lastUpdated.plusHours(24).isBefore(LocalDateTime.now())) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<Map> response = restTemplate.getForEntity(API_URL, Map.class);
                Map<String, Object> body = response.getBody();

                if (body != null && body.containsKey("conversion_rates")) {
                    Map<String, Object> rates = (Map<String, Object>) body.get("conversion_rates");
                    if (rates.containsKey("VND")) {
                        cachedRate = Double.parseDouble(rates.get("VND").toString());
                        lastUpdated = LocalDateTime.now();
                        System.out.println("Tỷ giá mới được cập nhật: " + cachedRate);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi lấy tỷ giá VND: " + e.getMessage());
            }
        } else {
            System.out.println("Dùng tỷ giá cached: " + cachedRate);
        }

        return cachedRate;
    }
}