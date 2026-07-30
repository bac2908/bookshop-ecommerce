package vn.fs.backend.application.service.paypal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

// Service tao PayPal Order v2 va capture thanh toan
@Slf4j
@Service
@RequiredArgsConstructor
public class PaypalOrdersV2Service {

    private final RestTemplate rest;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${paypal.mode:sandbox}")   private String mode;
    @Value("${paypal.client.app:}")    private String clientId;
    @Value("${paypal.client.secret:}") private String clientSecret;
    @Value("${paypal.brand:Book Shop}") private String brand;

    // Cache token nho
    private volatile String accessToken;
    private volatile long tokenExpireEpoch = 0L;

    private String apiBase() {
        return "live".equalsIgnoreCase(mode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    // Lay token client_credentials
    private synchronized String getAccessToken() throws Exception {
        long now = Instant.now().getEpochSecond();
        if (accessToken != null && now < (tokenExpireEpoch - 60)) {
            return accessToken;
        }
        String tokenUrl = apiBase() + "/v1/oauth2/token";

        HttpHeaders h = new HttpHeaders();
        h.setBasicAuth(clientId, clientSecret);
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        try {
            ResponseEntity<String> res = rest.exchange(
                    tokenUrl, HttpMethod.POST, new HttpEntity<>(form, h), String.class);

            JsonNode j = om.readTree(res.getBody());
            String token = j.path("access_token").asText(null);
            int expires = j.path("expires_in").asInt(3000);
            if (token == null) {
                log.error("PayPal OAuth khong co access_token. body={}", res.getBody());
                throw new IllegalStateException("Paypal OAuth that bai");
            }
            accessToken = token;
            tokenExpireEpoch = now + Math.max(60, expires);
            return accessToken;
        } catch (HttpStatusCodeException ex) {
            log.error("PayPal OAuth fail {} {}. body={}", ex.getStatusCode(), ex.getStatusText(), safeBody(ex));
            throw ex;
        }
    }

    private HttpHeaders authHeaders(String requestId) throws Exception {
        String token = getAccessToken();
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.set("Prefer", "return=representation");
        if (requestId != null) h.set("PayPal-Request-Id", requestId); // idempotency
        return h;
    }

    private static String two(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String safeBody(HttpStatusCodeException ex) {
        try { return ex.getResponseBodyAsString(); } catch (Exception ignore) { return ""; }
    }

    // ===== CREATE ORDER =====
    public static class CreateOut {
        public String orderId; public String approveUrl; public String currency; public String value;
    }

    public CreateOut createOrder(BigDecimal usdAmount, String currency, String returnUrl, String cancelUrl) throws Exception {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("intent", "CAPTURE");
        body.put("purchase_units", List.of(Map.of("amount", Map.of(
                "currency_code", currency, "value", two(usdAmount)
        ))));
        body.put("application_context", Map.of(
                "brand_name", brand,
                "return_url", returnUrl,
                "cancel_url", cancelUrl,
                "user_action", "PAY_NOW",
                "shipping_preference", "NO_SHIPPING"
        ));

        String url = apiBase() + "/v2/checkout/orders";
        try {
            HttpEntity<Map<String,Object>> req = new HttpEntity<>(body, authHeaders("CRT-"+UUID.randomUUID()));
            ResponseEntity<String> res = rest.postForEntity(url, req, String.class);

            JsonNode j = om.readTree(res.getBody());
            CreateOut out = new CreateOut();
            out.orderId = j.path("id").asText();
            out.currency = currency;
            out.value = two(usdAmount);
            for (JsonNode l : j.withArray("links")) {
                if ("approve".equalsIgnoreCase(l.path("rel").asText())) {
                    out.approveUrl = l.path("href").asText(); break;
                }
            }
            if (out.orderId == null || out.approveUrl == null) {
                log.error("Create order thieu id/approveUrl. body={}", res.getBody());
                throw new IllegalStateException("Create order thieu truong bat buoc");
            }
            return out;
        } catch (HttpStatusCodeException ex) {
            log.error("createOrder fail {} {}. body={}", ex.getStatusCode(), ex.getStatusText(), safeBody(ex));
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // thu refresh token 1 lan
                this.accessToken = null;
                HttpEntity<Map<String,Object>> req2 = new HttpEntity<>(body, authHeaders("CRT-"+UUID.randomUUID()));
                ResponseEntity<String> res2 = rest.postForEntity(url, req2, String.class);
                JsonNode j2 = om.readTree(res2.getBody());
                CreateOut out2 = new CreateOut();
                out2.orderId = j2.path("id").asText();
                for (JsonNode l : j2.withArray("links")) {
                    if ("approve".equalsIgnoreCase(l.path("rel").asText())) {
                        out2.approveUrl = l.path("href").asText(); break;
                    }
                }
                out2.currency = currency; out2.value = two(usdAmount);
                if (out2.orderId != null && out2.approveUrl != null) return out2;
            }
            throw ex;
        }
    }

    // ===== CAPTURE =====
    public static class CaptureOut {
        public String status; public String orderId; public String captureId; public String payerEmail;
    }

    public CaptureOut captureOrder(String orderId) throws Exception {
        String url = apiBase()+"/v2/checkout/orders/"+orderId+"/capture";
        try {
            HttpEntity<String> req = new HttpEntity<>("{}", authHeaders("CAP-"+orderId));
            ResponseEntity<String> res = rest.postForEntity(url, req, String.class);

            JsonNode j = om.readTree(res.getBody());
            CaptureOut out = new CaptureOut();
            out.orderId = j.path("id").asText();
            out.status  = j.path("status").asText();
            JsonNode cap = j.path("purchase_units").path(0).path("payments").path("captures").path(0);
            if (!cap.isMissingNode()) out.captureId = cap.path("id").asText();
            out.payerEmail = j.path("payer").path("email_address").asText(null);

            if (!"COMPLETED".equalsIgnoreCase(out.status))
                throw new IllegalStateException("Capture status: " + out.status);
            return out;
        } catch (HttpStatusCodeException ex) {
            log.error("captureOrder fail {} {}. body={}", ex.getStatusCode(), ex.getStatusText(), safeBody(ex));
            throw ex;
        }
    }
}
