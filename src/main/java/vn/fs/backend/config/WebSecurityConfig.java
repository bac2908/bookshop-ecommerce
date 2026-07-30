package vn.fs.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import vn.fs.backend.security.ApiSecurityErrorHandler;
import vn.fs.backend.security.AuthenticationSuccessHandler;
import vn.fs.backend.security.CustomAuthenticationFailureHandler;
import vn.fs.backend.security.PreLoginLockFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private static final RequestMatcher API_REQUEST = request -> {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String applicationPath = requestUri.substring(contextPath.length());
        return applicationPath.startsWith("/api/");
    };
    private static final RequestMatcher NON_API_REQUEST =
            new NegatedRequestMatcher(API_REQUEST);

    @Autowired private AuthenticationSuccessHandler successHandler;
    @Autowired private CustomAuthenticationFailureHandler failureHandler;
    @Autowired private PreLoginLockFilter preLoginLockFilter;
    @Autowired private ApiSecurityErrorHandler apiSecurityErrorHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/storefront/**", "/admin-assets/**", "/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**",
                                "/api/v1/categories", "/api/v1/publishers").permitAll()
                        .requestMatchers("/api/v1/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/", "/home", "/login", "/register",
                                "/confirmOtpRegister", "/products", "/productDetail",
                                "/productByCategory", "/productByPublisher", "/searchProduct",
                                "/aboutUs", "/contact", "/loadImage", "/forgotPassword",
                                "/confirmOtpForgotPassword", "/changePassword", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/register", "/confirmOtpRegister",
                                "/forgotPassword", "/confirmOtpForgotPassword",
                                "/resendOtpForgotPassword", "/changePassword").permitAll()
                        .requestMatchers("/admin/reports", "/admin/reportCategory",
                                "/admin/reportYear", "/admin/reportMonth", "/admin/reportQuarter",
                                "/admin/reportOrderCustomer", "/admin/report-order-customer",
                                "/admin/report/customer").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/addToCart", "/remove/**", "/cart/**",
                                "/shoppingCart_checkout", "/checkout", "/pay/**",
                                "/checkout_success", "/checkout_paypal_success").authenticated()
                        .requestMatchers("/favorite/**", "/doFavorite", "/doUnFavorite",
                                "/profile/**", "/order/**").authenticated()
                        // An endpoint added later is private unless explicitly made public.
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/doLogin")
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout_success")
                        .permitAll())
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember"))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                apiSecurityErrorHandler,
                                API_REQUEST)
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                NON_API_REQUEST)
                        .defaultAccessDeniedHandlerFor(
                                apiSecurityErrorHandler,
                                API_REQUEST)
                        .defaultAccessDeniedHandlerFor(
                                new AccessDeniedHandlerImpl(),
                                NON_API_REQUEST))
                // CSRF remains enabled (Spring Security's secure default).
                .addFilterBefore(preLoginLockFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
