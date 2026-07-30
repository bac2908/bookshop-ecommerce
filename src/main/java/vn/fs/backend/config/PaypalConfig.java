package vn.fs.backend.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// Cau hinh RestTemplate don gian
@Configuration
public class PaypalConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Co the set timeout tai day neu can
        return builder.build();
    }
}
