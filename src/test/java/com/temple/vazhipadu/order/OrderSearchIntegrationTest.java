package com.temple.vazhipadu.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temple.vazhipadu.common.dto.PagedResponse;
import com.temple.vazhipadu.offering.dto.CreateOfferingRequest;
import com.temple.vazhipadu.offering.dto.OfferingResponse;
import com.temple.vazhipadu.offering.service.OfferingService;
import com.temple.vazhipadu.order.dto.CreateOrderRequest;
import com.temple.vazhipadu.order.dto.OrderItemRequest;
import com.temple.vazhipadu.order.dto.OrderResponse;
import com.temple.vazhipadu.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OfferingService offeringService;

    @Autowired
    private OrderService orderService;

    private Long searchTempleId = 200L;
    private Long offeringId;

    @BeforeEach
    void setupOfferings() {
        OfferingResponse offering = offeringService.createOffering(
                new CreateOfferingRequest(searchTempleId, "Neyyvilakku", new BigDecimal("40.00"), true, null)
        );
        offeringId = offering.getId();
    }

    @Test
    @DisplayName("P2 Acceptance: Searching by phone number returns only that devotee's orders")
    void testSearchByPhoneNumberReturnsOnlyDevoteeOrders() throws Exception {
        String targetPhone = "9944001122";
        String otherPhone = "9988776655";

        // Create 3 orders for target phone
        for (int i = 1; i <= 3; i++) {
            orderService.createOrder(new CreateOrderRequest(
                    searchTempleId,
                    "Devotee Target " + i,
                    targetPhone,
                    "Rohini",
                    LocalDate.now().plusDays(i),
                    List.of(new OrderItemRequest(offeringId, 1))
            ));
        }

        // Create 2 orders for different phone
        for (int i = 1; i <= 2; i++) {
            orderService.createOrder(new CreateOrderRequest(
                    searchTempleId,
                    "Other Person " + i,
                    otherPhone,
                    "Ashwini",
                    LocalDate.now().plusDays(i),
                    List.of(new OrderItemRequest(offeringId, 1))
            ));
        }

        mockMvc.perform(get("/api/v1/orders")
                        .param("templeId", searchTempleId.toString())
                        .param("phoneNumber", targetPhone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].phoneNumber", everyItem(is(targetPhone))));
    }

    @Test
    @DisplayName("P2 Acceptance: Pagination page 0 and page 1 returns consecutive results with no repeats or gaps")
    void testPaginationReturnsNextSetWithNoRepeatsOrGaps() throws Exception {
        Long paginationTempleId = 205L;
        OfferingResponse pagOffering = offeringService.createOffering(
                new CreateOfferingRequest(paginationTempleId, "Ganapathi Homam", new BigDecimal("150.00"), true, null)
        );

        int totalOrdersToCreate = 7;
        List<Long> createdOrderIds = new ArrayList<>();
        for (int i = 1; i <= totalOrdersToCreate; i++) {
            OrderResponse created = orderService.createOrder(new CreateOrderRequest(
                    paginationTempleId,
                    "Devotee " + i,
                    "900000000" + i,
                    "Makayiram",
                    LocalDate.now().plusDays(1),
                    List.of(new OrderItemRequest(pagOffering.getId(), 1))
            ));
            createdOrderIds.add(created.getId());
        }

        int pageSize = 4;

        // Fetch Page 0
        MvcResult page0Result = mockMvc.perform(get("/api/v1/orders")
                        .param("templeId", paginationTempleId.toString())
                        .param("page", "0")
                        .param("size", String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageSize", is(4)))
                .andExpect(jsonPath("$.totalElements", is(totalOrdersToCreate)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.last", is(false)))
                .andReturn();

        // Fetch Page 1
        MvcResult page1Result = mockMvc.perform(get("/api/v1/orders")
                        .param("templeId", paginationTempleId.toString())
                        .param("page", "1")
                        .param("size", String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber", is(1)))
                .andExpect(jsonPath("$.pageSize", is(4)))
                .andExpect(jsonPath("$.totalElements", is(totalOrdersToCreate)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.last", is(true)))
                .andReturn();

        PagedResponse<OrderResponse> page0 = objectMapper.readValue(
                page0Result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(PagedResponse.class, OrderResponse.class)
        );
        PagedResponse<OrderResponse> page1 = objectMapper.readValue(
                page1Result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(PagedResponse.class, OrderResponse.class)
        );

        assertThat(page0.getContent()).hasSize(4);
        assertThat(page1.getContent()).hasSize(3);

        List<Long> page0Ids = page0.getContent().stream().map(OrderResponse::getId).toList();
        List<Long> page1Ids = page1.getContent().stream().map(OrderResponse::getId).toList();

        // Verify no overlap between page 0 and page 1
        Set<Long> combinedIds = new HashSet<>(page0Ids);
        for (Long id : page1Ids) {
            assertThat(combinedIds.add(id))
                    .withFailMessage("Found duplicate order id %d across page 0 and page 1", id)
                    .isTrue();
        }

        // Verify total combined equals all created orders (no gaps)
        assertThat(combinedIds).hasSize(totalOrdersToCreate);
        assertThat(combinedIds).containsAll(createdOrderIds);
    }
}
