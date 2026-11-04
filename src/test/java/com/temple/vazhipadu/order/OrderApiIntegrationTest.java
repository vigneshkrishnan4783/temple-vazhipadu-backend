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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OfferingService offeringService;

    @Test
    @DisplayName("P1 Acceptance: 2 Pushpanjali @ ₹30 and 1 Archana @ ₹50 has a total of ₹110")
    void testCreateOrderWithMultipleOfferingsCalculatesTotalCorrectly() throws Exception {
        Long templeId = 101L;

        // Create Pushpanjali @ ₹30
        OfferingResponse pushpanjali = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Pushpanjali", new BigDecimal("30.00"), true, null)
        );

        // Create Archana @ ₹50
        OfferingResponse archana = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Archana", new BigDecimal("50.00"), true, null)
        );

        CreateOrderRequest request = new CreateOrderRequest(
                templeId,
                "Anand Kumar",
                "9876543210",
                "Rohini",
                LocalDate.now().plusDays(1),
                List.of(
                        new OrderItemRequest(pushpanjali.getId(), 2),
                        new OrderItemRequest(archana.getId(), 1)
                )
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templeId", is(101)))
                .andExpect(jsonPath("$.devoteeName", is("Anand Kumar")))
                .andExpect(jsonPath("$.phoneNumber", is("9876543210")))
                .andExpect(jsonPath("$.nakshatra", is("Rohini")))
                .andExpect(jsonPath("$.totalAmount", is(110.00)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].offeringName", is("Pushpanjali")))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.items[0].subtotal", is(60.00)))
                .andExpect(jsonPath("$.items[1].offeringName", is("Archana")))
                .andExpect(jsonPath("$.items[1].quantity", is(1)))
                .andExpect(jsonPath("$.items[1].subtotal", is(50.00)));
    }

    @Test
    @DisplayName("P1 Acceptance: An order for an inactive offering is rejected with 400 Bad Request")
    void testOrderForInactiveOfferingIsRejected() throws Exception {
        Long templeId = 101L;

        OfferingResponse inactiveOffering = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Special Abhishekam", new BigDecimal("250.00"), false, null)
        );

        CreateOrderRequest request = new CreateOrderRequest(
                templeId,
                "Meenakshi",
                "9123456789",
                "Ashwini",
                LocalDate.now().plusDays(2),
                List.of(
                        new OrderItemRequest(inactiveOffering.getId(), 1)
                )
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Offering 'Special Abhishekam' is currently inactive and cannot be booked")));
    }

    @Test
    @DisplayName("P1 Requirement: Get order by ID returns full details")
    void testGetOrderById() throws Exception {
        Long templeId = 102L;

        OfferingResponse deeparadhana = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Deeparadhana", new BigDecimal("20.00"), true, null)
        );

        CreateOrderRequest request = new CreateOrderRequest(
                templeId,
                "Suresh",
                "9447001122",
                "Bharani",
                LocalDate.now().plusDays(3),
                List.of(
                        new OrderItemRequest(deeparadhana.getId(), 3)
                )
        );

        String createResponseJson = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(createResponseJson).get("id").asLong();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.totalAmount", is(60.00)))
                .andExpect(jsonPath("$.devoteeName", is("Suresh")));
    }

    @Test
    @DisplayName("P1 Requirement: Invalid input returns unified JSON error response")
    void testInvalidInputReturnsUnifiedError() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                null, // Missing templeId
                "", // Blank devoteeName
                "123", // Invalid phone
                "", // Blank nakshatra
                null, // Null bookingDate
                List.of() // Empty items
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed for request body")));
    }

    @Test
    @DisplayName("P1 Review Fix: Duplicate offerings in a single order request are rejected")
    void testOrderWithDuplicateOfferingsIsRejected() throws Exception {
        Long templeId = 101L;

        OfferingResponse pushpanjali = offeringService.createOffering(
                new CreateOfferingRequest(templeId, "Pushpanjali Duplicate Test", new BigDecimal("30.00"), true, null)
        );

        CreateOrderRequest request = new CreateOrderRequest(
                templeId,
                "Karthik",
                "9888877777",
                "Bharani",
                LocalDate.now().plusDays(1),
                List.of(
                        new OrderItemRequest(pushpanjali.getId(), 1),
                        new OrderItemRequest(pushpanjali.getId(), 2)
                )
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Duplicate offering id " + pushpanjali.getId() + " in order. Please consolidate quantities.")));
    }
}
