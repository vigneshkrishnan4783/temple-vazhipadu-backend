package com.temple.vazhipadu.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.vazhipadu.common.util.TimeUtils;
import com.temple.vazhipadu.offering.dto.CreateOfferingRequest;
import com.temple.vazhipadu.offering.dto.OfferingResponse;
import com.temple.vazhipadu.offering.service.OfferingService;
import com.temple.vazhipadu.order.dto.CreateOrderRequest;
import com.temple.vazhipadu.order.dto.OrderItemRequest;
import com.temple.vazhipadu.order.dto.OrderResponse;
import com.temple.vazhipadu.order.entity.Order;
import com.temple.vazhipadu.order.repository.OrderRepository;
import com.temple.vazhipadu.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderCancellationAndReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OfferingService offeringService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("P4 Acceptance: Cancelling an order frees its daily limit, and cancelled order is not counted in daily report")
    void testCancellationFreesDailyLimitAndExcludesFromReport() throws Exception {
        Long templeId = 401L;
        LocalDate bookingDate = LocalDate.now().plusDays(20);

        // Daily limit of 2
        OfferingResponse offering = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Thilahavanam", new BigDecimal("150.00"), true, 2)
        );

        // Book order 1 for 1 slot
        OrderResponse order1 = orderService.createOrder(new CreateOrderRequest(
                templeId, "Devotee 1", "9555555551", "Aswathi", bookingDate,
                List.of(new OrderItemRequest(offering.getId(), 1))
        ));

        // Book order 2 for 1 slot -> Quota is now full (2/2)
        OrderResponse order2 = orderService.createOrder(new CreateOrderRequest(
                templeId, "Devotee 2", "9555555552", "Bharani", bookingDate,
                List.of(new OrderItemRequest(offering.getId(), 1))
        ));

        // Try to book order 3 -> Fails because limit of 2 is reached
        CreateOrderRequest order3Request = new CreateOrderRequest(
                templeId, "Devotee 3", "9555555553", "Karthika", bookingDate,
                List.of(new OrderItemRequest(offering.getId(), 1))
        );
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order3Request)))
                .andExpect(status().isConflict());

        // Cancel order 2 via API
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", order2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Now order 3 should SUCCEED because limit slot is freed!
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order3Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Check Daily Collection Report for today:
        // Cancelled order 2 must NOT be counted; only order 1 and order 3 are counted!
        LocalDate todayIst = TimeUtils.toIstLocalDate(java.time.Instant.now());
        mockMvc.perform(get("/api/v1/reports/daily-collection")
                        .param("templeId", templeId.toString())
                        .param("date", todayIst.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templeId", is(templeId.intValue())))
                .andExpect(jsonPath("$.totalOrders", is(2)))
                .andExpect(jsonPath("$.totalAmount", is(300.00))) // 2 * 150.00 = 300.00
                .andExpect(jsonPath("$.offerings[0].quantity", is(2)))
                .andExpect(jsonPath("$.offerings[0].totalAmount", is(300.00)));
    }

    @Test
    @DisplayName("P4 Acceptance: An order placed at 00:15 IST on a date is counted on that date")
    void testOrderPlacedAt0015IstCountedOnThatDate() throws Exception {
        Long templeId = 402L;
        LocalDate targetDate = LocalDate.of(2026, 11, 10);

        OfferingResponse offering = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Palpayasam", new BigDecimal("75.00"), true, null)
        );

        OrderResponse createdOrder = orderService.createOrder(new CreateOrderRequest(
                templeId, "Midnight Devotee", "9666666666", "Rohini", targetDate.plusDays(1),
                List.of(new OrderItemRequest(offering.getId(), 2))
        ));

        // Simulate order placed at exactly 00:15 IST on 2026-11-10 (which is 2026-11-09 18:45:00 UTC)
        ZonedDateTime orderTimeInIst = ZonedDateTime.of(2026, 11, 10, 0, 15, 0, 0, TimeUtils.IST_ZONE);
        Order entity = orderRepository.findById(createdOrder.getId()).orElseThrow();
        entity.setCreatedAt(orderTimeInIst.toInstant());
        orderRepository.saveAndFlush(entity);

        // Fetch report for 2026-11-10
        mockMvc.perform(get("/api/v1/reports/daily-collection")
                        .param("templeId", templeId.toString())
                        .param("date", "2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate", is("2026-11-10")))
                .andExpect(jsonPath("$.totalOrders", is(1)))
                .andExpect(jsonPath("$.totalAmount", is(150.00)))
                .andExpect(jsonPath("$.offerings", hasSize(1)))
                .andExpect(jsonPath("$.offerings[0].offeringName", is("Palpayasam")))
                .andExpect(jsonPath("$.offerings[0].quantity", is(2)))
                .andExpect(jsonPath("$.offerings[0].totalAmount", is(150.00)));

        // Ensure it is NOT counted in 2026-11-09 report
        mockMvc.perform(get("/api/v1/reports/daily-collection")
                        .param("templeId", templeId.toString())
                        .param("date", "2026-11-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders", is(0)))
                .andExpect(jsonPath("$.totalAmount", is(0.00)));
    }

    @Test
    @DisplayName("P4 Acceptance: Report total exactly equals sum of offerings totals to the paisa")
    void testReportTotalEqualsSumOfOfferingsTotals() throws Exception {
        Long templeId = 403L;
        LocalDate reportDate = LocalDate.now();

        OfferingResponse off1 = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Malar Para", new BigDecimal("33.33"), true, null)
        );
        OfferingResponse off2 = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Nellu Para", new BigDecimal("66.67"), true, null)
        );

        // Order 1: 3 * 33.33 = 99.99
        orderService.createOrder(new CreateOrderRequest(
                templeId, "Devotee A", "9777777771", "Aswathi", reportDate.plusDays(1),
                List.of(new OrderItemRequest(off1.getId(), 3))
        ));

        // Order 2: 1 * 66.67 = 66.67
        orderService.createOrder(new CreateOrderRequest(
                templeId, "Devotee B", "9777777772", "Bharani", reportDate.plusDays(1),
                List.of(new OrderItemRequest(off2.getId(), 1))
        ));

        String responseJson = mockMvc.perform(get("/api/v1/reports/daily-collection")
                        .param("templeId", templeId.toString())
                        .param("date", reportDate.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        BigDecimal expectedTotal = new BigDecimal("99.99").add(new BigDecimal("66.67")); // 166.66
        double totalInJson = objectMapper.readTree(responseJson).get("totalAmount").asDouble();
        assertThat(new BigDecimal(String.format("%.2f", totalInJson)))
                .isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("P4 Requirement: Cancelling an already cancelled order is rejected")
    void testCancelAlreadyCancelledOrderIsRejected() throws Exception {
        Long templeId = 404L;
        OfferingResponse off = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Simple Offering", new BigDecimal("50.00"), true, null)
        );

        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                templeId, "Devotee C", "9888888888", "Rohini", LocalDate.now().plusDays(2),
                List.of(new OrderItemRequest(off.getId(), 1))
        ));

        // Cancel first time
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", order.getId()))
                .andExpect(status().isOk());

        // Cancel second time -> Expect 400 Bad Request
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", order.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Order is already cancelled")));
    }
}
