package vn.fs.backend.application.service.paypal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import vn.fs.backend.application.dto.RefundResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaypalRefundService {

    private final RestTemplate rest;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${paypal.mode:sandbox}")   private String mode;
    @Value("${paypal.client.app:}")    private String clientId;
    @Value("${paypal.client.secret:}") private String clientSecret;

    // cache token nhe
    private volatile String accessToken;
    private volatile long tokenExpireEpoch = 0L;

    private String apiBase() {
        return "live".equalsIgnoreCase(mode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    // lay OAuth token: content-type x-www-form-urlencoded CHI dung o day
    private synchronized String getAccessToken() throws Exception {
        long now = Instant.now().getEpochSecond();
        if (accessToken != null && now < (tokenExpireEpoch - 60)) return accessToken;

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
                log.error("OAuth khong co access_token. body={}", res.getBody());
                throw new IllegalStateException("Paypal OAuth fail");
            }
            accessToken = token;
            tokenExpireEpoch = now + Math.max(60, expires);
            return accessToken;
        } catch (HttpStatusCodeException ex) {
            log.error("OAuth fail {} {} body={}", ex.getStatusCode(), ex.getStatusText(), safeBody(ex));
            throw ex;
        }
    }

    private HttpHeaders authJsonHeaders(String requestId) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(getAccessToken());
        h.setContentType(MediaType.APPLICATION_JSON);      // <<=== QUAN TRONG
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.set("Prefer", "return=representation");
        if (requestId != null) h.set("PayPal-Request-Id", requestId); // idempotency
        return h;
    }

    private static String safeBody(HttpStatusCodeException ex) {
        try { return ex.getResponseBodyAsString(); } catch (Exception ignore) { return ""; }
    }

    // refund capture: amount == null => full refund
    public RefundResult refundCapture(String captureId,
                                      BigDecimal amount,
                                      String currency,
                                      String note) throws Exception {
        String url = apiBase() + "/v2/payments/captures/" + captureId + "/refund";

        // tao body JSON
        ObjectNode body = om.createObjectNode();
        if (amount != null) {
            String cur = (currency == null || currency.isBlank()) ? "USD" : currency.toUpperCase(Locale.ROOT);
            ObjectNode amt = body.putObject("amount");
            amt.put("value", amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
            amt.put("currency_code", cur);
        }
        if (note != null && !note.isBlank()) {
            body.put("note_to_payer", note);
        }

        // PayPal yeu cau JSON, dung headers JSON
        HttpEntity<String> req = new HttpEntity<>(
                body.size() == 0 ? "{}" : om.writeValueAsString(body),
                authJsonHeaders("RFD-" + UUID.randomUUID())
        );

        try {
            ResponseEntity<String> res = rest.postForEntity(url, req, String.class);
            JsonNode j = om.readTree(res.getBody());
            String refundId = j.path("id").asText(null);
            String status   = j.path("status").asText(null);
            if (refundId == null) {
                log.error("Refund response khong co id. body={}", res.getBody());
                throw new IllegalStateException("Refund that bai: khong nhan duoc refund id");
            }
            return new RefundResult(refundId, status);
        } catch (HttpStatusCodeException ex) {
            log.error("Refund fail {} {} body={}", ex.getStatusCode(), ex.getStatusText(), safeBody(ex));
            throw ex;
        }
    }
}
