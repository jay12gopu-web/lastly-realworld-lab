package com.lastly.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.*;
import java.util.List;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OrderApiIntegrationTest {
    @Container
    static final MySQLContainer<?> DB = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("lastly_test").withUsername("test").withPassword("test");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::getJdbcUrl);
        registry.add("spring.datasource.username", DB::getUsername);
        registry.add("spring.datasource.password", DB::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    private String newToken() throws Exception {
        String email="tester-"+UUID.randomUUID()+"@example.test";
        String pass="LongTestPassword_1234";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("email",email,"password",pass))))
            .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("email",email,"password",pass))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer "+json.readTree(body).get("accessToken").asText();
    }
    @Test void publicCatalogueAndPrivateOrder() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].sku",hasItem("LASTLY-MILK-001")));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1,\"quantity\":1,\"idempotencyKey\":\"no-auth\"}"))
            .andExpect(status().isUnauthorized());
        String token=newToken();
        mvc.perform(post("/api/orders").header("Authorization",token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1,\"quantity\":1,\"idempotencyKey\":\""+UUID.randomUUID()+"\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PLACED"));
    }
    @Test void otherCustomerCannotReadYourOrder() throws Exception {
        String own=newToken(), other=newToken();
        String response=mvc.perform(post("/api/orders").header("Authorization",own)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1,\"quantity\":1,\"idempotencyKey\":\""+UUID.randomUUID()+"\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String orderId=json.readTree(response).get("id").asText();
        mvc.perform(get("/api/orders/"+orderId).header("Authorization",own))
            .andExpect(status().isOk());
        mvc.perform(get("/api/orders/"+orderId).header("Authorization",other))
            .andExpect(status().isNotFound());
    }
    @Test void racingShoppersCannotOversellLastUnit() throws Exception {
        jdbc.update("UPDATE products SET stock_quantity = 1, version = version + 1 WHERE id = 3");
        String token=newToken();
        var start=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> submit=() -> {
                start.await();
                String body="{\"productId\":3,\"quantity\":1,\"idempotencyKey\":\""
                    +UUID.randomUUID()+"\"}";
                return mvc.perform(post("/api/orders").header("Authorization",token)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getStatus();
            };
            Future<Integer> a=pool.submit(submit),b=pool.submit(submit);
            start.countDown();
            List<Integer> statuses=List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertEquals(1,statuses.stream().filter(x->x==201).count());
            assertEquals(1,statuses.stream().filter(x->x==409).count());
            assertEquals(0,jdbc.queryForObject("SELECT stock_quantity FROM products WHERE id = 3",Integer.class));
        } finally { pool.shutdownNow(); }
    }
    @Test void invalidJwtIsRejected() throws Exception {
        mvc.perform(get("/api/orders/unknown")
            .header("Authorization", "Bearer not-a-real-jwt"))
            .andExpect(status().isUnauthorized());
    }
    @Test void invalidCredentialsAreRejected() throws Exception {
        String email="bad-login-"+UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of(
                "email",email,"password","StrongDemoPassword123"))))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of(
                "email",email,"password","WrongDemoPassword123"))))
            .andExpect(status().isUnauthorized());
    }
    @Test void shortPasswordRejectedAtRegistration() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of(
                "email","too-short@example.test","password","short"))))
            .andExpect(status().isBadRequest());
    }
    @Test void invalidQuantityGetsBadRequestWhenAuthenticated() throws Exception {
        mvc.perform(post("/api/orders").header("Authorization",newToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1,\"quantity\":0,\"idempotencyKey\":\"a\"}"))
            .andExpect(status().isBadRequest());
    }
}
