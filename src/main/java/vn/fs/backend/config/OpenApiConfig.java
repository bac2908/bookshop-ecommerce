package vn.fs.backend.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookshopOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bookshop API")
                        .version("v1")
                        .description("REST API cho danh mục và quản trị sản phẩm của Bookshop.")
                        .contact(new Contact().name("Bookshop Backend Team")))
                .components(new Components()
                        .addSecuritySchemes(
                                "sessionCookie",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("JSESSIONID")
                                        .description("Cookie phiên sau khi đăng nhập tại /login"))
                        .addSecuritySchemes(
                                "csrfToken",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-CSRF-TOKEN")
                                        .description("CSRF token bắt buộc cho POST, PUT và DELETE")));
    }

    @Bean
    public GroupedOpenApi publicApiV1() {
        return GroupedOpenApi.builder()
                .group("bookshop-v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
