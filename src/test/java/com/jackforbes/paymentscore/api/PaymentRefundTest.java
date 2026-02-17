package com.jackforbes.paymentscore.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PaymentRefundTest {

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
    void refund_fullCapturedAmount_transitionsToRefunded() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");
        capture(paymentId, 1234);

        mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1234}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.state").value("REFUNDED"))
                .andExpect(jsonPath("$.refundedAmount").value(1234));
    }

    @Test
    void refund_partialAmount_transitionsToPartiallyRefunded() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");
        capture(paymentId, 1234);

        mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.refundedAmount").value(100));
    }

    @Test
    void refund_secondRefund_completesToRefunded() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");
        capture(paymentId, 1234);

        mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.refundedAmount").value(100));

        mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1134}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REFUNDED"))
                .andExpect(jsonPath("$.refundedAmount").value(1234));
    }

    @Test
    void refund_exceedsCapturedAmount_returns409ProblemDetail() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");
        capture(paymentId, 100); // only captured 100

        mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":200}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void refund_beforeAnyCapture_returns409ProblemDetail() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");

        mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void refund_inRefundedState_returns409ProblemDetail() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");
        capture(paymentId, 1234);

        mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1234}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REFUNDED"));

        mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void refund_sameIdempotencyKey_sameRequest_isReplayedAndDoesNotDoubleRefund() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");
        capture(paymentId, 1234);

        String idemKey = UUID.randomUUID().toString();

        String first = mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundedAmount").value(100))
                .andReturn().getResponse().getContentAsString();

        String second = mvc.perform(post("/payments/" + paymentId + "/refund")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundedAmount").value(100))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(first, second);
    }

    // ---------- helpers ----------

    private void capture(String paymentId, long amount) throws Exception {
        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%d}
                                """.formatted(amount)))
                .andExpect(status().isOk());
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