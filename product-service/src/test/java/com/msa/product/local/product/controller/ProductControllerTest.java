package com.msa.product.local.product.controller;

import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Nested
    @DisplayName("GET /products/{id} - 상품 단건 조회")
    class GetProduct {

        @Test
        @DisplayName("성공")
        void getProduct_success() throws Exception {
            // given
            Long productId = 1L;
            ProductDto.Detail response = ProductDto.Detail.builder()
                    .productId("1")
                    .factoryId(1L)
                    .factoryName("테스트공장")
                    .productName("테스트상품")
                    .standardWeight("10.5")
                    .build();

            given(productService.getProduct(productId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/products/{id}", productId)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.productName").value("테스트상품"))
                    .andExpect(jsonPath("$.data.factoryName").value("테스트공장"));

            verify(productService).getProduct(productId);
        }
    }

    @Nested
    @DisplayName("GET /products - 상품 목록 조회")
    class GetProducts {

        @Test
        @DisplayName("성공")
        void getProducts_success() throws Exception {
            // when & then
            mockMvc.perform(get("/products")
                            .param("page", "0")
                            .param("size", "10")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}
