package vn.fs.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import vn.fs.backend.security.ApiSecurityErrorHandler;
import vn.fs.backend.security.AuthenticationSuccessHandler;
import vn.fs.backend.security.CustomAuthenticationFailureHandler;
import vn.fs.backend.security.ApiLogoutSuccessHandler;
import vn.fs.backend.security.PreLoginLockFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private static final RequestMatcher API_REQUEST = request -> {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String applicationPath = requestUri.substring(contextPath.length());
        return applicationPath.startsWith("/api/");
    };
    private static final RequestMatcher NON_API_REQUEST =
            new NegatedRequestMatcher(API_REQUEST);
    private static final RequestMatcher LOGOUT_REQUEST = request -> {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String applicationPath = requestUri.substring(contextPath.length());
        return "/api/v1/auth/logout".equals(applicationPath);
    };

    @Autowired private AuthenticationSuccessHandler successHandler;
    @Autowired private CustomAuthenticationFailureHandler failureHandler;
    @Autowired private PreLoginLockFilter preLoginLockFilter;
    @Autowired private ApiSecurityErrorHandler apiSecurityErrorHandler;
    @Autowired private ApiLogoutSuccessHandler apiLogoutSuccessHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public ChangeSessionIdAuthenticationStrategy sessionFixationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public CsrfAuthenticationStrategy csrfAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository) {
        return new CsrfAuthenticationStrategy(csrfTokenRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/storefront/**", "/admin-assets/**", "/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**",
                                "/api/v1/categories", "/api/v1/publishers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/admin/users/**", "/api/v1/admin/roles")
                                .hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/orders/*/refund").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/", "/home", "/login", "/register",
                                "/confirmOtpRegister", "/products", "/productDetail",
                                "/productByCategory", "/productByPublisher", "/searchProduct",
                                "/aboutUs", "/contact", "/loadImage", "/forgotPassword",
                                "/confirmOtpForgotPassword", "/changePassword", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/register", "/confirmOtpRegister",
                                "/resendOtpRegister",
                                "/forgotPassword", "/confirmOtpForgotPassword",
                                "/resendOtpForgotPassword", "/changePassword").permitAll()
                        .requestMatchers("/admin/reports", "/admin/reportCategory",
                                "/admin/reportYear", "/admin/reportMonth", "/admin/reportQuarter",
                                "/admin/reportOrderCustomer", "/admin/report-order-customer",
                                "/admin/report/customer").hasRole("ADMIN")
                        .requestMatchers("/admin/customers/edit/**").hasRole("ADMIN")
                        .requestMatchers("/admin/paypal/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/shoppingCart_checkout", "/checkout", "/pay/**",
                                "/checkout_success", "/checkout_paypal_success").authenticated()
                        .requestMatchers("/favorite/**",
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
                        .logoutRequestMatcher(LOGOUT_REQUEST)
                        .logoutSuccessHandler(apiLogoutSuccessHandler)
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
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository))
                // CSRF remains enabled (Spring Security's secure default).
                .addFilterBefore(preLoginLockFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
