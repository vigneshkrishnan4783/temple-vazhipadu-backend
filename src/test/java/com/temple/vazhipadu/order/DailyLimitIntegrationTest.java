package com.temple.vazhipadu.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.vazhipadu.offering.dto.CreateOfferingRequest;
import com.temple.vazhipadu.offering.dto.OfferingResponse;
import com.temple.vazhipadu.offering.service.OfferingService;
import com.temple.vazhipadu.order.dto.CreateOrderRequest;
import com.temple.vazhipadu.order.dto.OrderItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DailyLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OfferingService offeringService;

    @Test
    @DisplayName("P3 Acceptance: With daily limit 5 and 4 booked, order for 2 is rejected and order for 1 succeeds")
    void testDailyLimitBoundaryConditions() throws Exception {
        Long templeId = 301L;
        LocalDate bookingDate = LocalDate.now().plusDays(10);

        // Create offering with daily limit of 5
        OfferingResponse pooja = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Udayasthamana Pooja", new BigDecimal("500.00"), true, 5)
        );

        // Book 4 slots
        CreateOrderRequest orderFor4 = new CreateOrderRequest(
                templeId, "Devotee 1", "9111111111", "Aswathi", bookingDate,
                List.of(new OrderItemRequest(pooja.getId(), 4))
        );
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderFor4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount", is(2000.00)));

        // Try to book 2 slots (4 + 2 = 6 > 5) -> Should be rejected
        CreateOrderRequest orderFor2 = new CreateOrderRequest(
                templeId, "Devotee 2", "9222222222", "Bharani", bookingDate,
                List.of(new OrderItemRequest(pooja.getId(), 2))
        );
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderFor2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is(String.format(
                        "Offering '%s' has exceeded its daily limit of 5 for date %s (already booked: 4, requested: 2)",
                        pooja.getName(), bookingDate
                ))));

        // Try to book 1 slot (4 + 1 = 5 <= 5) -> Should succeed
        CreateOrderRequest orderFor1 = new CreateOrderRequest(
                templeId, "Devotee 3", "9333333333", "Karthika", bookingDate,
                List.of(new OrderItemRequest(pooja.getId(), 1))
        );
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderFor1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount", is(500.00)));

        // Now with 5 booked, any further order for 1 should fail
        CreateOrderRequest orderAfterLimit = new CreateOrderRequest(
                templeId, "Devotee 4", "9444444444", "Rohini", bookingDate,
                List.of(new OrderItemRequest(pooja.getId(), 1))
        );
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderAfterLimit)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("P3 Acceptance: 20 simultaneous orders for 1 each against daily limit 5 -> Exactly 5 succeed")
    void testConcurrentOrdersRespectDailyLimit() throws Exception {
        Long templeId = 302L;
        LocalDate bookingDate = LocalDate.now().plusDays(15);
        int dailyLimit = 5;
        int totalRequests = 20;

        OfferingResponse pooja = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Special Concurrent Pooja", new BigDecimal("100.00"), true, dailyLimit)
        );

        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch readyLatch = new CountDownLatch(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= totalRequests; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for all threads to line up

                    CreateOrderRequest request = new CreateOrderRequest(
                            templeId,
                            "Devotee Thread " + index,
                            "98000000" + String.format("%02d", index),
                            "Makayiram",
                            bookingDate,
                            List.of(new OrderItemRequest(pooja.getId(), 1))
                    );

                    MvcResult result = mockMvc.perform(post("/api/v1/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    int statusCode = result.getResponse().getStatus();
                    statusCodes.add(statusCode);
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409 || statusCode == 400) {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all threads simultaneously!
        boolean completed = doneLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get())
                .withFailMessage("Expected exactly %d successful bookings, but got %d", dailyLimit, successCount.get())
                .isEqualTo(dailyLimit);

        assertThat(rejectedCount.get())
                .withFailMessage("Expected %d rejected bookings, but got %d", totalRequests - dailyLimit, rejectedCount.get())
                .isEqualTo(totalRequests - dailyLimit);

        assertThat(statusCodes).hasSize(totalRequests);
    }
}
