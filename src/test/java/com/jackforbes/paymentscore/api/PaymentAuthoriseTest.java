package com.jackforbes.paymentscore.api;

import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PaymentAuthoriseTest {

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
    void authorise_returns201_andInitialState() throws Exception {
        mvc.perform(post("/payments/authorise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1234,"currency":"GBP"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(1234))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.state").value("AUTHORISED"))
                .andExpect(jsonPath("$.capturedAmount").value(0))
                .andExpect(jsonPath("$.refundedAmount").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void get_returns200_forExistingPayment() throws Exception {
        String response = mvc.perform(post("/payments/authorise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1234,"currency":"GBP"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = JsonPath.read(response, "$.id");

        mvc.perform(get("/payments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.state").value("AUTHORISED"));
    }

    @Test
    void get_returns404_forNonExistingPayment() throws Exception {
        mvc.perform(get("/payments/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}