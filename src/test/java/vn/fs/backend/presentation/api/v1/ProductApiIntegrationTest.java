package vn.fs.backend.presentation.api.v1;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Testcontainers
class ProductApiIntegrationTest {

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
        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void protectedMutationRejectsMissingCsrfTokenWithJsonError() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(user("api-admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void invalidProductBodyReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(user("api-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("categoryId")));
    }

    @Test
    void adminCanCreateAndDeleteProductWithValidationAndCsrf() throws Exception {
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

        MvcResult created = mockMvc.perform(post("/api/v1/products")
                        .with(user("api-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Sản phẩm kiểm thử API"))
                .andReturn();

        JsonNode response = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        long productId = response.path("id").asLong();

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .with(user("api-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void openApiDocumentContainsVersionedProductPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs/bookshop-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Bookshop API"))
                .andExpect(jsonPath("$.paths['/api/v1/products']").exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse").exists());
    }
}
