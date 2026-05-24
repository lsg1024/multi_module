package com.msa.jewelry.account.internal.store.domain.entity;

import com.msa.jewelry.account.internal.global.domain.dto.AccountDto;
import com.msa.jewelry.account.internal.global.domain.dto.AdditionalOptionDto;
import com.msa.jewelry.account.internal.global.domain.dto.AddressDto;
import com.msa.jewelry.account.internal.global.domain.dto.CommonOptionDto;
import com.msa.jewelry.account.internal.global.domain.entity.Address;
import com.msa.jewelry.account.internal.global.domain.entity.CommonOption;
import com.msa.jewelry.account.internal.global.domain.entity.GoldHarry;
import com.msa.common.global.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * 매장(판매처) 엔티티.
 *
 * *금 거래의 판매 주체인 매장 정보를 관리한다. 주요 속성:
 *
 *   - 현재 금({@code currentGoldBalance}) 및 돈({@code currentMoneyBalance}) 잔액
 *   - 주소({@link com.msa.jewelry.account.internal.global.domain.entity.Address}) — 1:1 관계, CASCADE
 *   - 거래 옵션({@link com.msa.jewelry.account.internal.global.domain.entity.CommonOption}) — 해리/거래유형/등급 포함
 *   - 부가 옵션({@link AdditionalOption}) — 과거 매출 적용 여부 등 추가 설정
 * 
 *
 * *소프트 삭제 방식을 사용한다 ({@code storeDeleted = true}).
 * {@code storeDefault = true}인 레코드는 시스템 기본 매장을 나타낸다.
 *
 * *의존 엔티티: {@link com.msa.jewelry.account.internal.global.domain.entity.CommonOption},
 * {@link com.msa.jewelry.account.internal.global.domain.entity.Address}, {@link AdditionalOption}
 */
@Getter
@Entity
@Table(name = "STORE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE STORE SET STORE_DELETED = TRUE WHERE STORE_ID = ?")
@Schema(description = "거래처(매장) 엔티티 — 소매상 마스터. 미수금 잔고, 주소, 거래 옵션을 보유")
public class Store extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STORE_ID")
    @Schema(description = "거래처 PK", example = "10")
    private Long storeId;
    @Column(name = "STORE_NAME", unique = true, nullable = false, length = 30)
    @Schema(description = "거래처명 (유니크)", example = "강남금은방")
    private String storeName;
    @Column(name = "STORE_OWNER_NAME", length = 30)
    @Schema(description = "거래처 대표자명", example = "홍길동")
    private String storeOwnerName;
    @Column(name = "STORE_PHONE_NUMBER", length = 13)
    @Schema(description = "거래처 대표 전화번호", example = "010-1234-5678")
    private String storePhoneNumber;
    @Column(name = "STORE_CONTACT_NUMBER_1", length = 13)
    @Schema(description = "거래처 연락처 1", example = "02-123-4567")
    private String storeContactNumber1;
    @Column(name = "STORE_CONTACT_NUMBER_2", length = 13)
    @Schema(description = "거래처 연락처 2", example = "02-987-6543")
    private String storeContactNumber2;
    @Column(name = "STORE_FAX_NUMBER", length = 16)
    @Schema(description = "거래처 팩스 번호", example = "02-123-4568")
    private String storeFaxNumber;
    @Column(name = "STORE_NOTE")
    @Schema(description = "거래처 비고", example = "VIP 거래처")
    private String storeNote;
    /** 시스템 기본 매장 여부. 기본 매장은 삭제 및 변경이 제한될 수 있다. */
    @Column(name = "STORE_DEFAULT", nullable = false)
    @Schema(description = "시스템 기본 매장 여부 — 삭제/변경 제한 대상", example = "false")
    private boolean storeDefault = false;
    @Column(name = "STORE_DELETED", nullable = false)
    @Schema(description = "소프트 삭제 플래그", example = "false")
    private boolean storeDeleted = false;

    /** 현재 보유 금 잔액 (단위: 돈, 소수점 3자리). Kafka 이벤트 처리 시 delta 합산으로 갱신된다. */
    @Column(name = "CURRENT_GOLD_BALANCE", nullable = false, precision = 10, scale = 3)
    @Schema(description = "현재 금 미수 잔액(돈, 소수점 3자리)", example = "12.345")
    private BigDecimal currentGoldBalance = BigDecimal.ZERO;
    /** 현재 보유 돈(현금) 잔액 (단위: 원). Kafka 이벤트 처리 시 delta 합산으로 갱신된다. */
    @Column(name = "CURRENT_MONEY_BALANCE", nullable = false)
    @Schema(description = "현재 현금 미수 잔액(원)", example = "1500000")
    private Long currentMoneyBalance = 0L;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ADDRESS_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "거래처 주소 (1:1)")
    private Address address;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "COMMON_OPTION_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "거래처 공통 옵션 (등급/거래유형/금시세 정책, 1:1)")
    private CommonOption commonOption;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "OPTION_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "거래처 부가 옵션 (1:1)")
    private AdditionalOption additionalOption;

    @Builder
    public Store(String storeName, String storeOwnerName, String storePhoneNumber, String storeContactNumber1, String storeContactNumber2, String storeFaxNumber, String storeNote, Address address, CommonOption commonOption, AdditionalOption additionalOption) {
        this.storeName = storeName;
        this.storeOwnerName = storeOwnerName;
        this.storePhoneNumber = storePhoneNumber;
        this.storeContactNumber1 = storeContactNumber1;
        this.storeContactNumber2 = storeContactNumber2;
        this.storeFaxNumber = storeFaxNumber;
        this.storeNote = storeNote;
        this.address = address;
        this.commonOption = commonOption;
        this.additionalOption = additionalOption;
    }

    /**
     * 매장 정보를 부분 업데이트한다.
     * null/빈 문자열로 전달된 필드는 기존 DB 값을 유지한다 (payload 누락 시 데이터 유실 방지).
     */
    public void updateStoreInfo(AccountDto.AccountInfo updateInfo) {
        if (updateInfo == null) {
            return;
        }
        if (updateInfo.getAccountName() != null && !updateInfo.getAccountName().isEmpty()) {
            this.storeName = updateInfo.getAccountName();
        }
        if (updateInfo.getAccountOwnerName() != null) {
            this.storeOwnerName = updateInfo.getAccountOwnerName();
        }
        if (updateInfo.getAccountPhoneNumber() != null) {
            this.storePhoneNumber = updateInfo.getAccountPhoneNumber();
        }
        if (updateInfo.getAccountContactNumber1() != null) {
            this.storeContactNumber1 = updateInfo.getAccountContactNumber1();
        }
        if (updateInfo.getAccountContactNumber2() != null) {
            this.storeContactNumber2 = updateInfo.getAccountContactNumber2();
        }
        if (updateInfo.getAccountFaxNumber() != null) {
            this.storeFaxNumber = updateInfo.getAccountFaxNumber();
        }
        if (updateInfo.getAccountNote() != null) {
            this.storeNote = updateInfo.getAccountNote();
        }
    }

    public void updateCommonOption(CommonOptionDto.CommonOptionInfo optionInfo, GoldHarry goldHarry) {
        this.commonOption.updateTradeTypeAndOptionLevel(optionInfo);
        this.commonOption.updateGoldHarry(goldHarry);
    }

    public void updateAdditionalOption(AdditionalOptionDto.AdditionalOptionInfo optionInfo) {
        if (optionInfo == null) {
            return;
        }

        if (this.additionalOption == null) {
            this.additionalOption = AdditionalOption.builder()
                    .optionApplyPastSales(optionInfo.isAdditionalApplyPastSales())
                    .optionMaterialId(optionInfo.getAdditionalMaterialId())
                    .optionMaterialName(optionInfo.getAdditionalMaterialName())
                    .build();
        } else {
            this.additionalOption.update(optionInfo);
        }
    }

    public void updateAddressInfo(AddressDto.AddressInfo addressInfo) {
        if (addressInfo == null) {
            return;
        }

        if (this.address == null) {
            this.address = Address.builder()
                    .addressAdd(addressInfo.getAddressAdd())
                    .addressBasic(addressInfo.getAddressBasic())
                    .addressZipCode(addressInfo.getAddressZipCode())
                    .build();
        } else {
            this.address.update(addressInfo);
        }
    }

    /**
     * 금 및 돈 잔액에 delta 값을 합산하여 현재 잔액을 갱신한다.
     *
     * *양수 delta는 잔액 증가(입금/매입), 음수 delta는 잔액 감소(출금/매출)를 의미한다.
     * Kafka 이벤트 처리 흐름에서 {@link com.msa.jewelry.account.internal.global.kafka.service.KafkaService}가 호출한다.
     *
     * @param goldAmount  금 잔액 변동분 (단위: 돈, 양수=증가, 음수=감소)
     * @param moneyAmount 돈 잔액 변동분 (단위: 원, 양수=증가, 음수=감소)
     */
    public void updateBalance(BigDecimal goldAmount, Long moneyAmount) {
        this.currentGoldBalance = this.currentGoldBalance.add(goldAmount);
        this.currentMoneyBalance += moneyAmount;
    }

    public boolean isNameChanged(String storeName) {
        return !this.storeName.equals(storeName);
    }
}
