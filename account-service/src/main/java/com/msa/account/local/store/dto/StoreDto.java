package com.msa.account.local.store.dto;

import com.msa.account.global.domain.dto.AdditionalOptionDto;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.local.store.entity.Store;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.account.global.domain.dto.util.ExchangeEnumUtil.getLevelTypeTitle;
import static com.msa.account.global.domain.dto.util.ExchangeEnumUtil.getTradeTypeTitle;

public class StoreDto {

    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";

    //목록 조회
    @Getter
    @NoArgsConstructor
    public static class StoreResponse {
        private Long storeId;
        private String storeName;
        private String storeOwnerName;
        private String storePhoneNumber;
        private String storeContactNumber1;
        private String storeContactNumber2;
        private String storeFaxNumber;
        private String storeNote;
        private String address;
        private String tradeType;
        private String level;
        private String goldHarryLoss;

        @QueryProjection
        public StoreResponse(Long storeId, String storeName, String storeOwnerName, String storePhoneNumber, String storeContactNumber1, String storeContactNumber2, String storeFaxNumber, String storeNote, String address, String tradeType, String level, String goldHarryLoss) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.storeOwnerName = storeOwnerName;
            this.storePhoneNumber = storePhoneNumber;
            this.storeContactNumber1 = storeContactNumber1;
            this.storeContactNumber2 = storeContactNumber2;
            this.storeFaxNumber = storeFaxNumber;
            this.storeNote = storeNote;
            this.address = address;
            this.tradeType = getTradeTypeTitle(tradeType);;
            this.level = getLevelTypeTitle(level);;
            this.goldHarryLoss = goldHarryLoss;
        }
    }

    //개별 조회
    @Getter
    @NoArgsConstructor
    public static class StoreSingleResponse {
        private String storeId;
        private String storeName;
        private String storeOwnerName;
        private String storePhoneNumber;
        private String storeContactNumber1;
        private String storeContactNumber2;
        private String storeFaxNumber;
        private String storeNote;

        private String addressId;
        private String addressZipCode;
        private String addressBasic;
        private String addressAdd;

        private String commonOptionId;
        private String tradeType;
        private String level;
        private String goldHarryId;
        private String goldHarryLoss;

        private String additionalOptionId;
        private boolean additionalApplyPastSales;
        private String additionalMaterialId;
        private String additionalMaterialName;

        @QueryProjection
        public StoreSingleResponse(String storeId, String storeName, String storeOwnerName, String storePhoneNumber, String storeContactNumber1, String storeContactNumber2, String storeFaxNumber, String storeNote, String addressId, String addressZipCode, String addressBasic, String addressAdd, String commonOptionId, String tradeType, String level, String goldHarryId, String goldHarryLoss, String additionalOptionId, boolean additionalApplyPastSales, String additionalMaterialId, String additionalMaterialName) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.storeOwnerName = storeOwnerName;
            this.storePhoneNumber = storePhoneNumber;
            this.storeContactNumber1 = storeContactNumber1;
            this.storeContactNumber2 = storeContactNumber2;
            this.storeFaxNumber = storeFaxNumber;
            this.storeNote = storeNote;
            this.addressId = addressId;
            this.addressZipCode = addressZipCode;
            this.addressBasic = addressBasic;
            this.addressAdd = addressAdd;
            this.commonOptionId = commonOptionId;
            this.tradeType = getTradeTypeTitle(tradeType);
            this.level = getLevelTypeTitle(level);;
            this.goldHarryId = goldHarryId;
            this.goldHarryLoss = goldHarryLoss;
            this.additionalOptionId = additionalOptionId;
            this.additionalApplyPastSales = additionalApplyPastSales;
            this.additionalMaterialId = additionalMaterialId;
            this.additionalMaterialName = additionalMaterialName;
        }
    }

    //상점 생성
    @Getter
    @NoArgsConstructor
    public static class StoreRequest {

        @Valid
        @NotNull(message = "상점 정보는 필수입니다.")
        private StoreInfo storeInfo;

        @NotNull(message = "기본 옵션 정보는 필수입니다.")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        private AdditionalOptionDto.AdditionalOptionInfo additionalOptionInfo;
        @Valid
        private AddressDto.AddressInfo addressInfo;

        public Store toEntity(GoldHarry goldHarry) {
            return Store.builder()
                    .storeName(storeInfo.getStoreName())
                    .storeOwnerName(storeInfo.getStoreOwnerName())
                    .storePhoneNumber(storeInfo.getStorePhoneNumber())
                    .storeContactNumber1(storeInfo.getStoreContactNumber1())
                    .storeContactNumber2(storeInfo.getStoreContactNumber2())
                    .storeFaxNumber(storeInfo.getStoreFaxNumber())
                    .storeNote(storeInfo.getStoreNote())
                    .address(addressInfo.toEntity())
                    .commonOption(commonOptionInfo.toEntity(goldHarry))
                    .additionalOption(additionalOptionInfo.toEntity())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StoreUpdate {
        @Valid
        @NotNull(message = "상점 정보는 필수입니다.")
        private StoreInfo storeInfo;
        @NotNull(message = "기본 옵션 정보는 필수입니다.")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        private AdditionalOptionDto.AdditionalOptionInfo additionalOptionInfo;
        @Valid
        private AddressDto.AddressInfo addressInfo;
    }

    @Getter
    @NoArgsConstructor
    public static class StoreInfo {

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String storeName;

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String storeOwnerName;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String storePhoneNumber;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String storeContactNumber1;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String storeContactNumber2;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String storeFaxNumber;

        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String storeNote;

        @Builder
        @QueryProjection
        public StoreInfo(String storeName, String storeOwnerName, String storePhoneNumber, String storeContactNumber1, String storeContactNumber2, String storeFaxNumber, String storeNote) {
            this.storeName = storeName;
            this.storeOwnerName = storeOwnerName;
            this.storePhoneNumber = storePhoneNumber;
            this.storeContactNumber1 = storeContactNumber1;
            this.storeContactNumber2 = storeContactNumber2;
            this.storeFaxNumber = storeFaxNumber;
            this.storeNote = storeNote;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StoreCondition {
        private String storeName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiStoreInfo {
        private Long storeId;
        private String storeName;
        private String grade;
    }

}
