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
import java.util.concurrent.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PaymentConcurrencyTest {

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

    @Autowired MockMvc mvc;

    @Test
    void concurrent_captures_oneWins_otherGets409ConcurrentModification() throws Exception {
        String paymentId = authoriseAndExtractId(1000, "GBP");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<Integer> req1 = () -> doConcurrentCapture(paymentId, ready, start, 600);
        Callable<Integer> req2 = () -> doConcurrentCapture(paymentId, ready, start, 600);

        Future<Integer> f1 = pool.submit(req1);
        Future<Integer> f2 = pool.submit(req2);

        // wait until both threads are staged, then release them together
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        int s1 = f1.get(10, TimeUnit.SECONDS);
        int s2 = f2.get(10, TimeUnit.SECONDS);

        pool.shutdownNow();

        // Expect one success and one conflict.
        // We accept either ordering.
        org.junit.jupiter.api.Assertions.assertTrue(
                (s1 == 200 && s2 == 409) || (s1 == 409 && s2 == 200),
                "Expected one 200 and one 409, got " + s1 + " and " + s2
        );
    }

    private int doConcurrentCapture(String paymentId, CountDownLatch ready, CountDownLatch start, long amount) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);

        // Perform and return status
        return mvc.perform(post("/payments/" + paymentId + "/capture")
                        .header("X-Client-Id", "clientA")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%d}
                                """.formatted(amount)))
                .andReturn()
                .getResponse()
                .getStatus();
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
