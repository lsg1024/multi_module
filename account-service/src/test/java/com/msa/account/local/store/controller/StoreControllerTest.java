package com.msa.account.local.store.controller;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.local.factory.service.ExcelService;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.account.local.store.service.StoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoreController.class)
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private ExcelService excelService;

    @MockitoBean
    private JobLauncher jobLauncher;

    @MockitoBean
    private Job storeImportJob;

    @Nested
    @DisplayName("GET /store/{id} - 판매처 단건 조회")
    class GetStore {

        @Test
        @DisplayName("성공")
        void getStoreInfo_success() throws Exception {
            // given
            String storeId = "1";
            AccountDto.AccountSingleResponse response = new AccountDto.AccountSingleResponse(
                    "1", "테스트매장", "홍길동", "01012345678", null, null, null, "테스트 메모",
                    null, null, null, null, "1", "WEIGHT", "ONE", "1", "1.10"
            );

            given(storeService.getStoreInfo(storeId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/store/{id}", storeId)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accountName").value("테스트매장"))
                    .andExpect(jsonPath("$.data.accountOwnerName").value("홍길동"));

            verify(storeService).getStoreInfo(storeId);
        }
    }

    @Nested
    @DisplayName("GET /stores - 판매처 목록 조회")
    class GetStores {

        @Test
        @DisplayName("성공")
        void getStoreList_success() throws Exception {
            // when & then
            mockMvc.perform(get("/stores")
                            .param("page", "0")
                            .param("size", "12")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("성공 - 검색어 포함")
        void getStoreList_withSearch_success() throws Exception {
            // when & then
            mockMvc.perform(get("/stores")
                            .param("search", "테스트")
                            .param("page", "0")
                            .param("size", "12")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /stores/grade - 판매처 등급 조회")
    class GetStoreGrade {

        @Test
        @DisplayName("성공")
        void getStoreGrade_success() throws Exception {
            // given
            String storeId = "1";
            given(storeService.getStoreGrade(storeId)).willReturn("1급");

            // when & then
            mockMvc.perform(get("/stores/grade")
                            .param("id", storeId)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("1급"));

            verify(storeService).getStoreGrade(storeId);
        }
    }

    @Nested
    @DisplayName("DELETE /stores/{id} - 판매처 삭제")
    class DeleteStore {

        @Test
        @DisplayName("성공")
        void deleteStore_success() throws Exception {
            // given
            String storeId = "1";

            // when & then
            mockMvc.perform(delete("/stores/{id}", storeId)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /stores/harry/{id}/{harry} - 판매처 해리 업데이트")
    class UpdateHarry {

        @Test
        @DisplayName("성공")
        void updateHarry_success() throws Exception {
            // given
            String storeId = "1";
            String harryId = "2";

            // when & then
            mockMvc.perform(patch("/stores/harry/{id}/{harry}", storeId, harryId)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("수정 완료"));
        }
    }

    @Nested
    @DisplayName("PATCH /stores/grade/{id}/{grade} - 판매처 등급 업데이트")
    class UpdateGrade {

        @Test
        @DisplayName("성공")
        void updateGrade_success() throws Exception {
            // given
            String storeId = "1";
            String grade = "TWO";

            // when & then
            mockMvc.perform(patch("/stores/grade/{id}/{grade}", storeId, grade)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("수정 완료"));
        }
    }

    @Nested
    @DisplayName("GET /api/store/{id} - API 판매처 조회")
    class GetApiStore {

        @Test
        @DisplayName("성공")
        void getApiStoreInfo_success() throws Exception {
            // given
            Long storeId = 1L;
            StoreDto.ApiStoreInfo response = new StoreDto.ApiStoreInfo(1L, "테스트매장", "1급", "1.10", false);

            given(storeService.getStoreInfo(storeId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/store/{id}", storeId)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.storeName").value("테스트매장"))
                    .andExpect(jsonPath("$.data.grade").value("1급"));
        }
    }

    @Nested
    @DisplayName("GET /stores/receivable - 판매처 미수금액 조회")
    class GetStoreReceivable {

        @Test
        @DisplayName("성공")
        void getStoreReceivable_success() throws Exception {
            // when & then
            mockMvc.perform(get("/stores/receivable")
                            .param("page", "0")
                            .param("size", "12")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /stores/receivable/{id} - 판매처 미수금액 상세조회")
    class GetStoreReceivableDetail {

        @Test
        @DisplayName("성공")
        void getStoreReceivableDetail_success() throws Exception {
            // given
            String storeId = "1";
            AccountDto.AccountResponse response = AccountDto.AccountResponse.builder()
                    .accountId(1L)
                    .accountName("테스트매장")
                    .goldWeight("10.5")
                    .moneyAmount("100000")
                    .build();

            given(storeService.getStoreReceivableDetail(storeId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/stores/receivable/{id}", storeId)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accountName").value("테스트매장"));
        }
    }
}
