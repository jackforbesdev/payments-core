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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PaymentCaptureTest {

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
    void capture_fullAmount_transitionsToCaptured() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1234}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.state").value("CAPTURED"))
                .andExpect(jsonPath("$.capturedAmount").value(1234));
    }

    @Test
    void capture_partialAmount_transitionsToPartiallyCaptured() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PARTIALLY_CAPTURED"))
                .andExpect(jsonPath("$.capturedAmount").value(100));
    }

    @Test
    void capture_secondCapture_completesToCaptured() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PARTIALLY_CAPTURED"))
                .andExpect(jsonPath("$.capturedAmount").value(100));

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1134}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CAPTURED"))
                .andExpect(jsonPath("$.capturedAmount").value(1234));
    }

    @Test
    void capture_exceedsAuthorisedAmount_returns409ProblemDetail() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":2000}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void capture_inCapturedState_returns409ProblemDetail() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");

        mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1234}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CAPTURED"));

        mvc.perform(post("/payments/" + paymentId + "/capture")
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
    void capture_sameIdempotencyKey_sameRequest_isReplayedAndDoesNotDoubleCapture() throws Exception {
        String paymentId = authoriseAndExtractId(1234, "GBP");
        String idemKey = UUID.randomUUID().toString();

        String first = mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capturedAmount").value(100))
                .andReturn().getResponse().getContentAsString();

        String second = mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capturedAmount").value(100))
                .andReturn().getResponse().getContentAsString();

        // crude but effective: replay should be identical JSON (or at least same business fields)
        org.junit.jupiter.api.Assertions.assertEquals(first, second);
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