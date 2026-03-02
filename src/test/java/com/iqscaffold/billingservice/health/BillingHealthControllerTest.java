package com.iqscaffold.billingservice.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingHealthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void paymentsHealth_shouldReturnHealthStatus() throws Exception {
    mockMvc.perform(get("/api/v1/billing/payments/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.module").value("payments"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void subscriptionsHealth_shouldReturnHealthStatus() throws Exception {
    mockMvc.perform(get("/api/v1/billing/subscriptions/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.module").value("subscriptions"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void invoicesHealth_shouldReturnHealthStatus() throws Exception {
    mockMvc.perform(get("/api/v1/billing/invoices/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.module").value("invoices"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void gatewayHealth_shouldReturnHealthStatus() throws Exception {
    mockMvc.perform(get("/api/v1/billing/gateway/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.module").value("gateway"))
        .andExpect(jsonPath("$.timestamp").exists());
  }
}
