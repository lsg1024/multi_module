package com.msa.order.local.order.controller;

import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.service.OrdersService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdersController.class)
class OrdersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdersService ordersService;

    @Nested
    @DisplayName("GET /order - 주문 단건 조회")
    class GetOrder {

        @Test
        @DisplayName("성공")
        void getOrder_success() throws Exception {
            // given
            Long flowCode = 1L;
            OrderDto.ResponseDetail response = OrderDto.ResponseDetail.builder()
                    .flowCode("1")
                    .storeName("테스트매장")
                    .factoryName("테스트공장")
                    .productName("테스트상품")
                    .colorName("골드")
                    .materialName("14K")
                    .build();

            given(ordersService.getOrder(flowCode)).willReturn(response);

            // when & then
            mockMvc.perform(get("/order")
                            .param("flowCode", "1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.storeName").value("테스트매장"))
                    .andExpect(jsonPath("$.data.productName").value("테스트상품"));
        }
    }

    @Nested
    @DisplayName("PATCH /orders/status - 주문 상태 변경")
    class UpdateOrderStatus {

        @Test
        @DisplayName("성공")
        void updateOrderStatus_success() throws Exception {
            // given
            doNothing().when(ordersService).updateOrderStatus(any(), eq("1"), eq("RECEIPT"));

            // when & then
            mockMvc.perform(patch("/orders/status")
                            .param("id", "1")
                            .param("status", "RECEIPT")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(ordersService).updateOrderStatus(any(), eq("1"), eq("RECEIPT"));
        }
    }

    @Nested
    @DisplayName("DELETE /orders/delete - 주문 삭제")
    class DeleteOrder {

        @Test
        @DisplayName("성공")
        void deleteOrder_success() throws Exception {
            // given
            doNothing().when(ordersService).deletedOrders(any(), eq("1"));

            // when & then
            mockMvc.perform(delete("/orders/delete")
                            .param("id", "1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(ordersService).deletedOrders(any(), eq("1"));
        }
    }

    @Nested
    @DisplayName("GET /orders/status - 주문 상태 조회")
    class GetOrderStatus {

        @Test
        @DisplayName("성공")
        void getOrderStatus_success() throws Exception {
            // given
            List<String> statuses = List.of("접수", "접수실패", "대기");
            given(ordersService.getOrderStatusInfo("1")).willReturn(statuses);

            // when & then
            mockMvc.perform(get("/orders/status")
                            .param("id", "1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(3));
        }
    }
}
