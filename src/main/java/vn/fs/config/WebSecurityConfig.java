package vn.fs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.fs.security.CustomAuthenticationFailureHandler;
import vn.fs.security.PreLoginLockFilter;
import vn.fs.service.UserDetailService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired private UserDetailService userDetailService;
    @Autowired private SuccessHandler successHandler;
    @Autowired private CustomAuthenticationFailureHandler failureHandler;
    @Autowired private PreLoginLockFilter preLoginLockFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/images/**", "/vendor/**", "/fonts/**",
                        "/assets/**", "/uploads/**").permitAll()

                .antMatchers(HttpMethod.GET, "/", "/home", "/login", "/register",
                        "/confirmOtpRegister", "/products", "/productDetail",
                        "/productByCategory", "/productByPublisher", "/searchProduct",
                        "/aboutUs", "/contact", "/loadImage", "/forgotPassword",
                        "/confirmOtpForgotPassword", "/changePassword", "/error").permitAll()

                .antMatchers(HttpMethod.POST, "/register", "/confirmOtpRegister",
                        "/forgotPassword", "/confirmOtpForgotPassword",
                        "/resendOtpForgotPassword", "/changePassword").permitAll()

                .antMatchers("/admin/reports", "/admin/reportCategory",
                        "/admin/reportYear", "/admin/reportMonth", "/admin/reportQuarter",
                        "/admin/reportOrderCustomer", "/admin/report-order-customer",
                        "/admin/report/customer").hasRole("ADMIN")

                .antMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")

                .antMatchers("/addToCart", "/remove/**", "/cart/**",
                        "/shoppingCart_checkout", "/checkout", "/pay/**",
                        "/checkout_success", "/checkout_paypal_success").authenticated()

                .antMatchers("/favorite/**", "/doFavorite", "/doUnFavorite",
                        "/profile/**", "/order/**").authenticated()

                // An endpoint added later is private unless explicitly made public.
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginProcessingUrl("/doLogin")
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
                .and()
                .logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout_success")
                .permitAll()
                .and()
                .rememberMe()
                .rememberMeParameter("remember");

        // CSRF remains enabled (Spring Security's secure default).
        http.addFilterBefore(preLoginLockFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
