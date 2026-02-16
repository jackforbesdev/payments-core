package com.jackforbes.paymentscore.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PaymentErrorHandlingTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("payments")
            .withUsername("payments")
            .withPassword("payments");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void get_returns404_withProblemDetail_whenPaymentMissing() throws Exception {
        mvc.perform(get("/payments/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/payments/00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void capture_returns409_whenIdempotencyKeyReusedWithDifferentRequest() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");

        String idemKey = java.util.UUID.randomUUID().toString();

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":200}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void capture_returns409_whenInvalidTransition() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", "k1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1234}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", "k2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void authorise_returns400_whenValidationFails() throws Exception {

        mvc.perform(post("/payments/authorise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1234}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private String authoriseAndExtractId(long amount, String currency) throws Exception {
        String response = mvc.perform(post("/payments/authorise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%d,"currency":"%s"}
                                """.formatted(amount, currency)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}

