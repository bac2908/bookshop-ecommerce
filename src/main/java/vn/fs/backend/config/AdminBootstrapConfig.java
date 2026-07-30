package vn.fs.backend.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.fs.backend.application.service.AdminBootstrapService;

@Slf4j
@Configuration
public class AdminBootstrapConfig {

    @Bean
    @ConditionalOnProperty(
            name = "app.bootstrap-admin.enabled",
            havingValue = "true")
    ApplicationRunner firstAdministratorBootstrap(
            AdminBootstrapService bootstrapService,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.name:Administrator}") String name,
            @Value("${app.bootstrap-admin.phone:}") String phone) {
        return arguments -> {
            boolean created = bootstrapService.provisionFirstAdministrator(
                    username,
                    email,
                    password,
                    name,
                    phone);
            if (created) {
                log.warn(
                        "Đã tạo quản trị viên đầu tiên '{}'. "
                                + "Hãy tắt ADMIN_BOOTSTRAP_ENABLED sau khi khởi tạo.",
                        username);
            }
        };
    }
}
