package vn.fs.backend.presentation.api.v1;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Map;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.fs.backend.application.service.OrderConfirmationMailService;
import vn.fs.backend.application.service.SendMailService;
import vn.fs.backend.application.service.impl.ExchangeRateService;
import vn.fs.backend.application.service.paypal.PaypalOrdersV2Service;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Testcontainers
class BookshopApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.4.10"))
            .withDatabaseName("bookshop_integration")
            .withUsername("bookshop")
            .withPassword("bookshop_test")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockitoBean
    private PaypalOrdersV2Service paypalOrdersV2Service;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @MockitoBean
    private OrderConfirmationMailService orderConfirmationMailService;

    @MockitoBean
    private SendMailService sendMailService;

    @BeforeEach
    void seedApiUser() {
        reset(
                paypalOrdersV2Service,
                exchangeRateService,
                orderConfirmationMailService,
                sendMailService);
        when(exchangeRateService.getVNDExchangeRate()).thenReturn(25_000.0);

        Long customerId = ensureApiUser(
                "api_customer",
                "api.customer@example.test",
                "API Customer");
        Long otherCustomerId = ensureApiUser(
                "api_other",
                "api.other@example.test",
                "API Other Customer");
        Long adminId = ensureApiUser(
                "api_admin",
                "api.admin@example.test",
                "API Administrator");
        Long staffId = ensureApiUser(
                "api_staff",
                "api.staff@example.test",
                "API Staff");

        setUserRoles(customerId, "ROLE_USER");
        setUserRoles(otherCustomerId, "ROLE_USER");
        setUserRoles(adminId, "ROLE_ADMIN");
        setUserRoles(staffId, "ROLE_STAFF");

        cleanCustomerState(customerId);
        cleanCustomerState(otherCustomerId);
        cleanCustomerState(adminId);
        cleanCustomerState(staffId);
        jdbcTemplate.update(
                "delete from categories where category_name like 'API Phase4%'");
        jdbcTemplate.update(
                "delete from nxb where name like 'API Phase4%'");
        jdbcTemplate.update(
                "update categories set status = 1 where category_id <= 6");
        jdbcTemplate.update(
                "update nxb set status = 1 where id <= 6");
        jdbcTemplate.update(
                "update products set status = 1 where product_id <= 12");
        jdbcTemplate.update(
                "update products set quantity = 100 where product_id = 1");
    }

    private Long ensureApiUser(String username, String email, String name) {
        Integer existing = jdbcTemplate.queryForObject(
                "select count(*) from user where username = ?",
                Integer.class,
                username);
        if (existing == null || existing == 0) {
            jdbcTemplate.update(
                    """
                    insert into user (
                        avatar, email, failed_attempt, last_login_at, locked_until,
                        name, password, phone, register_date, status, username
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    null,
                    email,
                    0,
                    null,
                    null,
                    name,
                    passwordEncoder.encode("ValidPassword123!"),
                    "0900000000",
                    Date.valueOf(LocalDate.of(2026, 1, 1)),
                    true,
                    username);
        }

        Long userId = jdbcTemplate.queryForObject(
                "select user_id from user where username = ?",
                Long.class,
                username);
        Integer roleCount = jdbcTemplate.queryForObject(
                "select count(*) from users_roles where user_id = ?",
                Integer.class,
                userId);
        if (roleCount == null || roleCount == 0) {
            Long roleId = jdbcTemplate.queryForObject(
                    "select id from role where name = 'ROLE_USER'",
                    Long.class);
            jdbcTemplate.update(
                    "insert into users_roles (user_id, role_id) values (?, ?)",
                    userId,
                    roleId);
        }
        return userId;
    }

    private void cleanCustomerState(Long userId) {
        jdbcTemplate.update("delete from favorites where user_id = ?", userId);
        jdbcTemplate.update(
                """
                delete from comments
                where order_detail_id in (
                    select order_detail_id
                    from order_details
                    where order_id in (select order_id from orders where user_id = ?)
                )
                """,
                userId);
        jdbcTemplate.update(
                "delete from order_payments where order_id in (select order_id from orders where user_id = ?)",
                userId);
        jdbcTemplate.update(
                "delete from order_details where order_id in (select order_id from orders where user_id = ?)",
                userId);
        jdbcTemplate.update("delete from orders where user_id = ?", userId);
        jdbcTemplate.update(
                "delete from cart_items where cart_id in (select cart_id from carts where user_id = ?)",
                userId);
        jdbcTemplate.update("delete from carts where user_id = ?", userId);
        jdbcTemplate.update(
                "update user set failed_attempt = 0, locked_until = null where user_id = ?",
                userId);
    }

    private void setUserRoles(Long userId, String... roleNames) {
        jdbcTemplate.update("delete from users_roles where user_id = ?", userId);
        for (String roleName : roleNames) {
            Long roleId = jdbcTemplate.queryForObject(
                    "select id from role where name = ?",
                    Long.class,
                    roleName);
            jdbcTemplate.update(
                    "insert into users_roles (user_id, role_id) values (?, ?)",
                    userId,
                    roleId);
        }
        jdbcTemplate.update(
                "update user set status = 1 where user_id = ?",
                userId);
    }

    @Test
    void productCatalogUsesRealMySqlAndReturnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "productName")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content[0].imageUrl").isNotEmpty());
    }

    @Test
    void categoryAndPublisherCatalogsArePublic() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));

        mockMvc.perform(get("/api/v1/publishers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void missingProductUsesStableErrorContract() throws Exception {
        mockMvc.perform(get("/api/v1/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/products/999999"));
    }

    @Test
    void invalidPaginationUsesValidationErrorContract() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void protectedMutationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void csrfEndpointIsPublicAndReturnsHeaderContract() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void realCsrfLoginSessionMeAndLogoutFlowWorks() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String anonymousSessionId = session.getId();
        JsonNode csrfBeforeLogin = objectMapper.readTree(
                csrfResult.getResponse().getContentAsByteArray());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .header(
                                csrfBeforeLogin.path("headerName").asText(),
                                csrfBeforeLogin.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "login", "api_customer",
                                "password", "ValidPassword123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("api_customer"))
                .andExpect(jsonPath("$.email").value("api.customer@example.test"))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")))
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotEquals(anonymousSessionId, authenticatedSession.getId());

        mockMvc.perform(get("/api/v1/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("api_customer"));

        MvcResult refreshedCsrfResult = mockMvc.perform(get("/api/v1/auth/csrf")
                        .session(authenticatedSession))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode csrfAfterLogin = objectMapper.readTree(
                refreshedCsrfResult.getResponse().getContentAsByteArray());
        assertNotEquals(
                csrfBeforeLogin.path("token").asText(),
                csrfAfterLogin.path("token").asText());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(authenticatedSession)
                        .header(
                                csrfAfterLogin.path("headerName").asText(),
                                csrfAfterLogin.path("token").asText()))
                .andExpect(status().isNoContent());

        assertTrue(authenticatedSession.isInvalid());
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void apiLoginRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "login", "api_customer",
                                "password", "ValidPassword123!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void invalidCredentialsUseStableErrorAndIncrementFailureCounter() throws Exception {
        jdbcTemplate.update(
                "update user set failed_attempt = 0, locked_until = null where username = ?",
                "api_customer");

        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        JsonNode csrfToken = objectMapper.readTree(
                csrfResult.getResponse().getContentAsByteArray());

        mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .header(
                                csrfToken.path("headerName").asText(),
                                csrfToken.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "login", "api_customer",
                                "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        Integer failedAttempts = jdbcTemplate.queryForObject(
                "select failed_attempt from user where username = ?",
                Integer.class,
                "api_customer");
        assertTrue(failedAttempts != null && failedAttempts >= 1);
    }

    @Test
    void forgotPasswordRequiresVerifiedOtpAndCannotReuseResetSession()
            throws Exception {
        String email = "api.customer@example.test";
        String replacementPassword = "RecoveredPassword123!";
        String reusedPassword = "ReusedPassword123!";
        String originalHash = jdbcTemplate.queryForObject(
                "select password from user where email = ?",
                String.class,
                email);

        try {
            MvcResult unverifiedRequest = mockMvc.perform(post("/forgotPassword")
                            .with(csrf())
                            .param("email", email))
                    .andExpect(status().isOk())
                    .andExpect(view().name(
                            "storefront/pages/confirm-otp-forgot-password"))
                    .andReturn();
            MockHttpSession unverifiedSession = (MockHttpSession) unverifiedRequest
                    .getRequest()
                    .getSession(false);
            assertNotNull(unverifiedSession);

            mockMvc.perform(post("/changePassword")
                            .session(unverifiedSession)
                            .with(csrf())
                            .param("email", email)
                            .param("newPassword", replacementPassword)
                            .param("confirmPassword", replacementPassword))
                    .andExpect(status().isOk())
                    .andExpect(view().name("storefront/pages/forgot-password"))
                    .andExpect(model().attributeExists("error"));
            assertEquals(originalHash, passwordHash(email));

            MvcResult verifiedRequest = mockMvc.perform(post("/forgotPassword")
                            .with(csrf())
                            .param("email", email))
                    .andExpect(status().isOk())
                    .andReturn();
            MockHttpSession verifiedSession = (MockHttpSession) verifiedRequest
                    .getRequest()
                    .getSession(false);
            assertNotNull(verifiedSession);
            String otp = (String) verifiedSession.getAttribute("FP_OTP");
            assertNotNull(otp);

            mockMvc.perform(post("/confirmOtpForgotPassword")
                            .session(verifiedSession)
                            .with(csrf())
                            .param("email", email)
                            .param("otp", otp))
                    .andExpect(status().isOk())
                    .andExpect(view().name("storefront/pages/change-password"));
            assertEquals(Boolean.TRUE, verifiedSession.getAttribute("FP_VERIFIED"));
            assertNull(verifiedSession.getAttribute("FP_OTP"));

            mockMvc.perform(post("/changePassword")
                            .session(verifiedSession)
                            .with(csrf())
                            .param("email", email)
                            .param("newPassword", replacementPassword)
                            .param("confirmPassword", replacementPassword))
                    .andExpect(status().isOk())
                    .andExpect(view().name("storefront/pages/change-password"))
                    .andExpect(model().attribute("showLoginLink", true));
            assertTrue(passwordEncoder.matches(
                    replacementPassword,
                    passwordHash(email)));
            assertNull(verifiedSession.getAttribute("FP_VERIFIED"));

            mockMvc.perform(post("/changePassword")
                            .session(verifiedSession)
                            .with(csrf())
                            .param("email", email)
                            .param("newPassword", reusedPassword)
                            .param("confirmPassword", reusedPassword))
                    .andExpect(status().isOk())
                    .andExpect(view().name("storefront/pages/forgot-password"))
                    .andExpect(model().attributeExists("error"));
            assertTrue(passwordEncoder.matches(
                    replacementPassword,
                    passwordHash(email)));
        } finally {
            jdbcTemplate.update(
                    "update user set password = ? where email = ?",
                    originalHash,
                    email);
        }
    }

    @Test
    void protectedMutationRejectsMissingCsrfTokenWithJsonError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .with(user("api-admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void cartAndFavoritesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void cartSupportsAddSetQuantityRemoveAndClear() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", 1,
                                "quantity", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distinctItems").value(1))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].imageUrl").isNotEmpty());

        mockMvc.perform(put("/api/v1/cart/items/1")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("quantity", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        mockMvc.perform(delete("/api/v1/cart/items/1")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalQuantity").value(0));

        mockMvc.perform(delete("/api/v1/cart/items")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void cartRejectsMissingProductExcessStockAndMissingCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", 999999,
                                "quantity", 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", 1,
                                "quantity", 101))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(user("api_customer").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", 1,
                                "quantity", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void codCheckoutReservesInventoryEnforcesOwnershipAndCancelsIdempotently()
            throws Exception {
        int stockBefore = productStock(1L);
        addProductToCart("api_customer", 1L, 2);

        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("COD")))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.containsString("/api/v1/orders/")))
                .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"))
                .andExpect(jsonPath("$.paymentMethod").value("COD"))
                .andReturn();

        long orderId = objectMapper.readTree(
                        checkout.getResponse().getContentAsByteArray())
                .path("orderId")
                .asLong();
        assertEquals(stockBefore - 2, productStock(1L));
        assertEquals(0, customerCartItemCount("api_customer"));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .with(user("api_customer").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.cancellable").value(true));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .with(user("api_other").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/orders/{orderId}/cancellation", orderId)
                        .with(user("api_other").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/orders/{orderId}/cancellation", orderId)
                        .with(user("api_customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellable").value(false));
        assertEquals(stockBefore, productStock(1L));

        mockMvc.perform(put("/api/v1/orders/{orderId}/cancellation", orderId)
                        .with(user("api_customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(stockBefore, productStock(1L));
    }

    @Test
    void checkoutRollsBackOrderAndKeepsCartWhenStockChanges() throws Exception {
        addProductToCart("api_customer", 1L, 2);
        jdbcTemplate.update(
                "update products set quantity = 1 where product_id = 1");

        mockMvc.perform(post("/api/v1/checkout")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("COD")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        Integer orders = jdbcTemplate.queryForObject(
                """
                select count(*)
                from orders o
                join user u on u.user_id = o.user_id
                where u.username = ?
                """,
                Integer.class,
                "api_customer");
        assertEquals(0, orders);
        assertEquals(1, customerCartItemCount("api_customer"));
        assertEquals(1, productStock(1L));
    }

    @Test
    void checkoutRequiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("COD")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/v1/checkout")
                        .with(user("api_customer").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("COD")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void paypalReturnGetDoesNotMutateAndCaptureIsOwnedAndIdempotent()
            throws Exception {
        PaypalOrdersV2Service.CreateOut created = paypalCreateOut(
                "PAYPAL-ORDER-CAPTURE");
        when(paypalOrdersV2Service.createOrder(
                any(BigDecimal.class),
                anyString(),
                anyString(),
                anyString()))
                .thenReturn(created);

        PaypalOrdersV2Service.CaptureOut captured =
                new PaypalOrdersV2Service.CaptureOut();
        captured.orderId = created.orderId;
        captured.captureId = "PAYPAL-CAPTURE-1";
        captured.status = "COMPLETED";
        captured.payerEmail = "payer@example.test";
        when(paypalOrdersV2Service.captureOrder(created.orderId))
                .thenReturn(captured);

        int stockBefore = productStock(1L);
        addProductToCart("api_customer", 1L, 1);
        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("PAYPAL")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$.approvalUrl").value(created.approveUrl))
                .andExpect(jsonPath("$.externalPaymentId").value(created.orderId))
                .andReturn();
        long orderId = objectMapper.readTree(
                        checkout.getResponse().getContentAsByteArray())
                .path("orderId")
                .asLong();
        assertEquals(stockBefore - 1, productStock(1L));
        assertEquals(0, customerCartItemCount("api_customer"));

        mockMvc.perform(get("/pay/success")
                        .param("token", created.orderId)
                        .with(user("api_customer").roles("USER")))
                .andExpect(status().isOk());
        verify(paypalOrdersV2Service, times(0)).captureOrder(anyString());
        assertEquals("PENDING", paymentStatus(created.orderId));

        mockMvc.perform(post("/api/v1/checkout/paypal/capture")
                        .with(user("api_other").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paypalActionBody(created.orderId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/checkout/paypal/capture")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paypalActionBody(created.orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/v1/checkout/paypal/capture")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paypalActionBody(created.orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(paypalOrdersV2Service, times(1)).captureOrder(created.orderId);
        assertEquals("COMPLETED", paymentStatus(created.orderId));
        assertEquals(stockBefore - 1, productStock(1L));
    }

    @Test
    void cancellingPendingPaypalCheckoutRestoresInventoryExactlyOnce()
            throws Exception {
        PaypalOrdersV2Service.CreateOut created = paypalCreateOut(
                "PAYPAL-ORDER-CANCEL");
        when(paypalOrdersV2Service.createOrder(
                any(BigDecimal.class),
                anyString(),
                anyString(),
                anyString()))
                .thenReturn(created);

        int stockBefore = productStock(1L);
        addProductToCart("api_customer", 1L, 3);
        mockMvc.perform(post("/api/v1/checkout")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("PAYPAL")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"));
        assertEquals(stockBefore - 3, productStock(1L));

        mockMvc.perform(post("/api/v1/checkout/paypal/cancel")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paypalActionBody(created.orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(stockBefore, productStock(1L));
        assertEquals("CANCELLED", paymentStatus(created.orderId));

        mockMvc.perform(post("/api/v1/checkout/paypal/cancel")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paypalActionBody(created.orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(stockBefore, productStock(1L));
    }

    @Test
    void favoritesAreIdempotentAndUniquePerUserAndProduct() throws Exception {
        mockMvc.perform(put("/api/v1/favorites/1")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true))
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(put("/api/v1/favorites/1")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(get("/api/v1/favorites")
                        .with(user("api_customer").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(delete("/api/v1/favorites/1")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(false))
                .andExpect(jsonPath("$.count").value(0));

        Integer persistedRows = jdbcTemplate.queryForObject(
                """
                select count(*)
                from favorites f
                join user u on u.user_id = f.user_id
                where u.username = ? and f.product_id = ?
                """,
                Integer.class,
                "api_customer",
                1);
        assertTrue(persistedRows != null && persistedRows == 0);
    }

    @Test
    void favoriteRejectsUnavailableProduct() throws Exception {
        mockMvc.perform(put("/api/v1/favorites/999999")
                        .with(user("api_customer").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));
    }

    @Test
    void invalidProductBodyReturnsFieldErrors() throws Exception {
        MockMultipartFile data = new MockMultipartFile(
                "data",
                "data.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of()));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "book.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/v1/admin/products")
                        .file(data)
                        .file(file)
                        .with(user("api-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("categoryId")));
    }

    @Test
    void adminCanCreateAdjustStockAndHideProductWithValidationAndCsrf() throws Exception {
        Map<String, Object> request = Map.ofEntries(
                Map.entry("name", "Sản phẩm kiểm thử API"),
                Map.entry("price", 79000),
                Map.entry("quantity", 25),
                Map.entry("discount", 5),
                Map.entry("description", "Dữ liệu chỉ tồn tại trong MySQL Testcontainers."),
                Map.entry("enteredDate", "2026-01-01"),
                Map.entry("active", true),
                Map.entry("categoryId", 1),
                Map.entry("publisherId", 1),
                Map.entry("imageName", "api-integration-book.jpg"));

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "data.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "api-integration-book.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] {1, 2, 3});

        MvcResult created = mockMvc.perform(multipart("/api/v1/admin/products")
                        .file(data)
                        .file(file)
                        .with(user("api-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Sản phẩm kiểm thử API"))
                .andReturn();

        JsonNode response = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        long productId = response.path("id").asLong();

        mockMvc.perform(put("/api/v1/admin/products/{id}/stock", productId)
                        .with(user("api-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("quantity", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(30));

        mockMvc.perform(put("/api/v1/admin/products/{id}/visibility", productId)
                        .with(user("api-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void adminOrderApiEnforcesTransitionsAndRefundRole() throws Exception {
        long orderId = insertOrder(userId("api_customer"), 0, 125_000);

        mockMvc.perform(put("/api/v1/admin/orders/{id}/status", orderId)
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(put("/api/v1/admin/orders/{id}/status", orderId)
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "status", "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(2));

        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund", orderId)
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void phaseFiveTemplatesRenderApiOnlyMutationForms() throws Exception {
        addProductToCart("api_customer", 1L, 1);

        MvcResult checkoutPage = mockMvc.perform(get("/checkout")
                        .with(user("api_customer").roles("USER")))
                .andExpect(status().isOk())
                .andReturn();
        String checkoutHtml = checkoutPage.getResponse().getContentAsString();
        assertTrue(checkoutHtml.contains("data-api-checkout"));
        assertTrue(checkoutHtml.contains("data-api-cart-action=\"increment\""));
        assertTrue(!checkoutHtml.contains("action=\"/checkout\""));
        assertTrue(!checkoutHtml.contains("action=\"/cart/inc"));

        MvcResult productsPage = mockMvc.perform(get("/admin/products")
                        .with(user("api_staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andReturn();
        String productsHtml = productsPage.getResponse().getContentAsString();
        assertTrue(productsHtml.contains("data-admin-product-create"));
        assertTrue(productsHtml.contains("data-admin-product-stock"));
        assertTrue(!productsHtml.contains("action=\"/admin/addProduct\""));
        assertTrue(productsHtml.contains("jquery-3.6.0.min.js"));
    }

    @Test
    void adminUserApiEnforcesRoleAndProtectsSelfAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user("api_customer").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user("api_staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(user("api_admin").roles("ADMIN"))
                        .param("keyword", "api_")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].password").doesNotExist())
                .andExpect(jsonPath(
                        "$.content[?(@.username == 'api_admin')].currentUser")
                        .value(hasItem(true)));

        Long adminId = userId("api_admin");
        Long adminRoleId = roleId("ROLE_ADMIN");
        mockMvc.perform(put("/api/v1/admin/users/{id}/access", adminId)
                        .with(user("api_admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "enabled", false,
                                "roleIds", java.util.List.of(adminRoleId)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_DISABLE_FORBIDDEN"));
    }

    @Test
    void adminCanUpdateAnotherUsersRolesAndStatusWithCsrf() throws Exception {
        Long targetId = userId("api_other");
        Long staffRoleId = roleId("ROLE_STAFF");

        mockMvc.perform(put("/api/v1/admin/users/{id}/access", targetId)
                        .with(user("api_admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "enabled", false,
                                "roleIds", java.util.List.of(staffRoleId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_STAFF")));

        Boolean enabled = jdbcTemplate.queryForObject(
                "select status from user where user_id = ?",
                Boolean.class,
                targetId);
        String role = jdbcTemplate.queryForObject(
                """
                select r.name
                from users_roles ur
                join role r on r.id = ur.role_id
                where ur.user_id = ?
                """,
                String.class,
                targetId);
        assertEquals(false, enabled);
        assertEquals("ROLE_STAFF", role);

        mockMvc.perform(put("/api/v1/admin/users/{id}/access", targetId)
                        .with(user("api_admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "enabled", true,
                                "roleIds", java.util.List.of(staffRoleId)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void staffCanManageCategoriesThroughMultipartApi() throws Exception {
        MockMultipartFile createData = jsonPart(
                "data",
                Map.of("name", "API Phase4 Category", "active", true));

        MvcResult created = mockMvc.perform(multipart("/api/v1/admin/categories")
                        .file(createData)
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API Phase4 Category"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        long categoryId = objectMapper.readTree(
                        created.getResponse().getContentAsByteArray())
                .path("id")
                .asLong();

        mockMvc.perform(multipart("/api/v1/admin/categories")
                        .file(jsonPart(
                                "data",
                                Map.of("name", "API Phase4 Category", "active", true)))
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_EXISTS"));

        mockMvc.perform(multipart(
                                "/api/v1/admin/categories/{id}",
                                categoryId)
                        .file(jsonPart(
                                "data",
                                Map.of("name", "API Phase4 Category Updated", "active", true)))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("API Phase4 Category Updated"));

        mockMvc.perform(put(
                                "/api/v1/admin/categories/{id}/visibility",
                                categoryId)
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Boolean active = jdbcTemplate.queryForObject(
                "select status from categories where category_id = ?",
                Boolean.class,
                categoryId);
        assertEquals(false, active);
    }

    @Test
    void staffCanManagePublishersAndDuplicateNamesAreRejected() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/admin/publishers")
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "API Phase4 Publisher",
                                "active", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API Phase4 Publisher"))
                .andReturn();
        long publisherId = objectMapper.readTree(
                        created.getResponse().getContentAsByteArray())
                .path("id")
                .asLong();

        mockMvc.perform(post("/api/v1/admin/publishers")
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "API Phase4 Publisher",
                                "active", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PUBLISHER_NAME_EXISTS"));

        mockMvc.perform(put(
                                "/api/v1/admin/publishers/{id}/visibility",
                                publisherId)
                        .with(user("api_staff").roles("STAFF"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void dashboardAndReportsExcludeCancelledOrders() throws Exception {
        Long customerId = userId("api_customer");
        long completedOrderId = insertOrder(customerId, 2, 100.0);
        jdbcTemplate.update(
                """
                insert into order_details (price, quantity, order_id, product_id)
                values (?, ?, ?, ?)
                """,
                50.0,
                2,
                completedOrderId,
                1L);
        long cancelledOrderId = insertOrder(customerId, 3, 900.0);
        jdbcTemplate.update(
                """
                insert into order_details (price, quantity, order_id, product_id)
                values (?, ?, ?, ?)
                """,
                100.0,
                9,
                cancelledOrderId,
                1L);

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user("api_staff").roles("STAFF"))
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.orders7d").value(1))
                .andExpect(jsonPath("$.summary.salesQty7d").value(2))
                .andExpect(jsonPath("$.summary.income7d").value(100.0))
                .andExpect(jsonPath("$.orders", hasSize(7)));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(user("api_admin").roles("ADMIN"))
                        .param("groupBy", "PRODUCT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("PRODUCT"))
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].quantity").value(2))
                .andExpect(jsonPath("$.rows[0].revenue").value(100.0));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .with(user("api_staff").roles("STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user("api_staff").roles("STAFF"))
                        .param("days", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/admin/home")
                        .with(user("api_staff").roles("STAFF")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/reports")
                        .with(user("api_admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    private void addProductToCart(String username, long productId, int quantity)
            throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .with(user(username).roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "productId", productId,
                                "quantity", quantity))))
                .andExpect(status().isOk());
    }

    private MockMultipartFile jsonPart(String name, Object value)
            throws Exception {
        return new MockMultipartFile(
                name,
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(value));
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject(
                "select user_id from user where username = ?",
                Long.class,
                username);
    }

    private String passwordHash(String email) {
        return jdbcTemplate.queryForObject(
                "select password from user where email = ?",
                String.class,
                email);
    }

    private Long roleId(String roleName) {
        return jdbcTemplate.queryForObject(
                "select id from role where name = ?",
                Long.class,
                roleName);
    }

    private long insertOrder(Long userId, int statusCode, double amount) {
        jdbcTemplate.update(
                """
                insert into orders (
                    address, amount, order_date, paypal_capture_id,
                    phone, status, user_id, note
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "123 API Report Street",
                amount,
                Date.valueOf(LocalDate.now()),
                null,
                "0900000000",
                statusCode,
                userId,
                "Phase 4 reporting test");
        Long orderId = jdbcTemplate.queryForObject(
                "select max(order_id) from orders where user_id = ?",
                Long.class,
                userId);
        return orderId == null ? 0L : orderId;
    }

    private byte[] checkoutBody(String paymentMethod) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of(
                "address", "123 Đường Kiểm Thử, Thành phố Hồ Chí Minh",
                "phone", "0900000000",
                "note", "API integration checkout",
                "paymentMethod", paymentMethod));
    }

    private byte[] paypalActionBody(String externalOrderId) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of(
                "externalOrderId", externalOrderId));
    }

    private PaypalOrdersV2Service.CreateOut paypalCreateOut(String orderId) {
        PaypalOrdersV2Service.CreateOut created =
                new PaypalOrdersV2Service.CreateOut();
        created.orderId = orderId;
        created.approveUrl = "https://sandbox.paypal.test/approve/" + orderId;
        created.currency = "USD";
        created.value = "3.06";
        return created;
    }

    private int productStock(long productId) {
        Integer quantity = jdbcTemplate.queryForObject(
                "select quantity from products where product_id = ?",
                Integer.class,
                productId);
        return quantity == null ? 0 : quantity;
    }

    private int customerCartItemCount(String username) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from cart_items ci
                join carts c on c.cart_id = ci.cart_id
                join user u on u.user_id = c.user_id
                where u.username = ?
                """,
                Integer.class,
                username);
        return count == null ? 0 : count;
    }

    private String paymentStatus(String externalOrderId) {
        return jdbcTemplate.queryForObject(
                """
                select status
                from order_payments
                where provider = 'PAYPAL' and external_order_id = ?
                """,
                String.class,
                externalOrderId);
    }

    @Test
    void openApiDocumentContainsVersionedProductPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs/bookshop-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Bookshop API"))
                .andExpect(jsonPath("$.paths['/api/v1/products']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/csrf']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cart']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cart/items']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/favorites']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/checkout']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/checkout/paypal/capture']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders/{orderId}']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/orders/{orderId}/cancellation']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/admin/users/{userId}/access']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/admin/categories']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/admin/publishers']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/admin/dashboard']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/admin/reports']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/admin/products']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/admin/products/{productId}/stock']")
                        .exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/admin/orders/{orderId}/status']")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse").exists());
    }
}
