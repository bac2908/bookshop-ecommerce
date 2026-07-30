package vn.fs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GreenyShopApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayBaselineMigrationIsApplied() {
		Integer appliedMigrations = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success = true",
				Integer.class);

		assertEquals(1, appliedMigrations);
	}

	@Test
	void homePageIsPublicAndRenders() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk());
	}

	@Test
	void invalidImagePathReturnsSafeFallback() throws Exception {
		mockMvc.perform(get("/loadImage").param("imageName", "../secret.txt"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/gif"))
				.andExpect(result -> assertTrue(
						result.getResponse().getContentAsByteArray().length > 0));
	}

	@Test
	@WithMockUser(username = "admin-export-test", roles = "ADMIN")
	void adminExcelExportUsesXlsxMediaType() throws Exception {
		mockMvc.perform(get("/admin/export"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string(
						"Content-Disposition", "attachment; filename=orders.xlsx"))
				.andExpect(result -> assertTrue(
						result.getResponse().getContentAsByteArray().length > 0));
	}

	@Test
	void profileRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/profile"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	void invalidLoginUsesCustomFailureHandler() throws Exception {
		mockMvc.perform(post("/doLogin")
						.with(csrf())
						.param("username", "missing-user")
						.param("password", "invalid-password"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	@WithMockUser(username = "security-test", roles = "USER")
	void stateChangingRequestWithoutCsrfIsRejected() throws Exception {
		mockMvc.perform(post("/order/cancel/1"))
				.andExpect(status().isForbidden());
	}

	@Test
	void protectedPostWithCsrfStillRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/order/cancel/1").with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}
}
