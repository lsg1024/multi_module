package com.msa.account.domain.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.domain.store.dto.StoreDto;
import com.msa.account.global.domain.dto.AdditionalOptionDto;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.GoldHarry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StoreController.class)
class StoreControllerTest {

    @Autowired
    MockMvc mockMvc;
    private StoreDto.StoreInfo originStoreInfo;
    private AddressDto.AddressInfo originAddressInfo;
    private CommonOptionDto.CommonOptionInfo originCommonOptionInfo;
    private AdditionalOptionDto.AdditionalOptionInfo originAdditionalOptionInfo;
    private GoldHarry mockGoldHarry;

    @BeforeEach
    void setUp() {
        originStoreInfo = StoreDto.StoreInfo.builder()
                .storeName("testName")
                .storeOwnerName("testOwner")
                .storePhoneNumber("01012341234")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("testNote")
                .build();

        originAddressInfo = AddressDto.AddressInfo.builder()
                .addressZipCode("12345")
                .addressBasic("서울특별시")
                .addressAdd("강남구")
                .build();

        originCommonOptionInfo = CommonOptionDto.CommonOptionInfo.builder()
                .level("ONE")
                .tradeType("WEIGHT")
                .goldHarryId("1")
                .goldHarryLoss("1.10")
                .build();

        originAdditionalOptionInfo = AdditionalOptionDto.AdditionalOptionInfo.builder()
                .additionalMaterialId("1")
                .additionalMaterialName("gold")
                .additionalApplyPastSales(true)
                .build();

        mockGoldHarry = GoldHarry.builder()
                .goldHarryLoss(new BigDecimal("1.10"))
                .build();

        ReflectionTestUtils.setField(mockGoldHarry, "goldHarryId", 1L);
    }

    @Test
    void store_createStoreApi_success() throws Exception {
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        setField(storeDto, "storeInfo", originStoreInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        mockMvc.perform(post("/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "test")
                        .content(new ObjectMapper().writeValueAsString(storeDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void store_createStoreApi_fail_storeName1() throws Exception {
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        StoreDto.StoreInfo storeInfo = StoreDto.StoreInfo.builder()
                .storeName(null)
                .storeOwnerName("testOwner")
                .storePhoneNumber("01012341234")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("testNote")
                .build();

        setField(storeDto, "storeInfo", storeInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        mockMvc.perform(post("/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "test")
                        .content(new ObjectMapper().writeValueAsString(storeDto)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("NO"))
                .andExpect(jsonPath("$.data['storeInfo.storeName']").value("필수 입력입니다."));
    }

    @Test
    void store_createStoreApi_fail_storeName2() throws Exception {
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        StoreDto.StoreInfo storeInfo = StoreDto.StoreInfo.builder()
                .storeName("")
                .storeOwnerName("testOwner")
                .storePhoneNumber("01012341234")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("testNote")
                .build();

        setField(storeDto, "storeInfo", storeInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        mockMvc.perform(post("/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "test")
                        .content(new ObjectMapper().writeValueAsString(storeDto)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("NO"))
                .andExpect(jsonPath("$.data['storeInfo.storeName']").value("필수 입력입니다."));
    }

    @Test
    void store_createStoreApi_fail_storeName3() throws Exception {
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        StoreDto.StoreInfo storeInfo = StoreDto.StoreInfo.builder()
                .storeName("!@#$%%$#")
                .storeOwnerName("testOwner")
                .storePhoneNumber("01012341234")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("testNote")
                .build();

        setField(storeDto, "storeInfo", storeInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        mockMvc.perform(post("/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "test")
                        .content(new ObjectMapper().writeValueAsString(storeDto)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("NO"))
                .andExpect(jsonPath("$.data['storeInfo.storeName']").value("영어, 한글, 숫자만 허용됩니다."));
    }

    @Test
    void store_createStoreApi_fail_phoneNumber1() throws Exception {
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        StoreDto.StoreInfo storeInfo = StoreDto.StoreInfo.builder()
                .storeName("testName")
                .storeOwnerName("testOwner")
                .storePhoneNumber("no")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("testNote")
                .build();

        setField(storeDto, "storeInfo", storeInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        mockMvc.perform(post("/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "test")
                        .content(new ObjectMapper().writeValueAsString(storeDto)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("NO"))
                .andExpect(jsonPath("$.data['storeInfo.storePhoneNumber']").value("숫자만 허용됩니다."));
    }

    @Test
    void store_createStoreApi_fail_phoneNumber2() throws Exception {
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        StoreDto.StoreInfo storeInfo = StoreDto.StoreInfo.builder()
                .storeName("testName")
                .storeOwnerName("testOwner")
                .storePhoneNumber("010-1234-1234")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("testNote")
                .build();

        setField(storeDto, "storeInfo", storeInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        mockMvc.perform(post("/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "test")
                        .content(new ObjectMapper().writeValueAsString(storeDto)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("NO"))
                .andExpect(jsonPath("$.data['storeInfo.storePhoneNumber']").value("숫자만 허용됩니다."));
    }

    @Test
    void store_createStoreApi_fail_note() throws Exception {
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        StoreDto.StoreInfo storeInfo = StoreDto.StoreInfo.builder()
                .storeName("testName")
                .storeOwnerName("testOwner")
                .storePhoneNumber("01012341234")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("@#$@%#@")
                .build();

        setField(storeDto, "storeInfo", storeInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        mockMvc.perform(post("/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "test")
                        .content(new ObjectMapper().writeValueAsString(storeDto)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("NO"))
                .andExpect(jsonPath("$.data['storeInfo.storeNote']").value("영어, 한글, 숫자만 허용됩니다."));
    }

}