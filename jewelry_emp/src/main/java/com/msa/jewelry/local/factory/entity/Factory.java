package com.msa.jewelry.local.factory.entity;

import com.msa.jewelry.global.dto.AccountDto;
import com.msa.jewelry.local.address.dto.AddressDto;
import com.msa.jewelry.local.common_option.dto.CommonOptionDto;
import com.msa.jewelry.local.address.entity.Address;
import com.msa.jewelry.local.common_option.entity.CommonOption;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
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

@Getter
@Entity
@Table(name = "FACTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE FACTORY SET FACTORY_DELETED = TRUE WHERE FACTORY_ID = ?")
@Schema(description = "제조사(공장) 엔티티 — 제조 위탁처 마스터. 미수금 잔고, 주소, 공통 옵션을 보유")
public class Factory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACTORY_ID")
    @Schema(description = "제조사 PK", example = "5")
    private Long factoryId;

    @Column(name = "FACTORY_NAME", unique = true, nullable = false, length = 30)
    @Schema(description = "제조사명 (유니크)", example = "한빛제조사")
    private String factoryName;
    @Column(name = "FACTORY_OWNER_NAME", length = 30)
    @Schema(description = "제조사 대표자명", example = "홍길동")
    private String factoryOwnerName;
    @Column(name = "FACTORY_PHONE_NUMBER", length = 13)
    @Schema(description = "제조사 대표 전화번호", example = "010-1234-5678")
    private String factoryPhoneNumber;
    @Column(name = "FACTORY_CONTACT_NUMBER_1", length = 13)
    @Schema(description = "제조사 연락처 1", example = "02-123-4567")
    private String factoryContactNumber1;
    @Column(name = "FACTORY_CONTACT_NUMBER_2", length = 13)
    @Schema(description = "제조사 연락처 2", example = "02-987-6543")
    private String factoryContactNumber2;
    @Column(name = "FACTORY_FAX_NUMBER", length = 16)
    @Schema(description = "제조사 팩스 번호", example = "02-123-4568")
    private String factoryFaxNumber;
    @Column(name = "FACTORY_NOTE")
    @Schema(description = "제조사 비고", example = "주요 협력사")
    private String factoryNote;
    @Column(name = "FACTORY_DELETED", nullable = false)
    @Schema(description = "소프트 삭제 플래그", example = "false")
    private boolean factoryDeleted = false;

    @Column(name = "CURRENT_GOLD_BALANCE", nullable = false, precision = 10, scale = 3)
    @Schema(description = "현재 금 미수 잔액(g)", example = "12.345")
    private BigDecimal currentGoldBalance = BigDecimal.ZERO;
    @Column(name = "CURRENT_MONEY_BALANCE", nullable = false)
    @Schema(description = "현재 현금 미수 잔액(원)", example = "1500000")
    private Long currentMoneyBalance = 0L;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ADDRESS_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "제조사 주소 (1:1)")
    private Address address;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "COMMON_OPTION_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "제조사 공통 옵션 (등급/거래유형/금시세 정책, 1:1)")
    private CommonOption commonOption;

    @Builder
    public Factory(String factoryName, String factoryOwnerName, String factoryPhoneNumber, String factoryContactNumber1, String factoryContactNumber2, String factoryFaxNumber, String factoryNote, boolean factoryDeleted, Address address, CommonOption commonOption) {
        this.factoryName = factoryName;
        this.factoryOwnerName = factoryOwnerName;
        this.factoryPhoneNumber = factoryPhoneNumber;
        this.factoryContactNumber1 = factoryContactNumber1;
        this.factoryContactNumber2 = factoryContactNumber2;
        this.factoryFaxNumber = factoryFaxNumber;
        this.factoryNote = factoryNote;
        this.factoryDeleted = factoryDeleted;
        this.address = address;
        this.commonOption = commonOption;
    }
    public void updateFactoryInfo(AccountDto.AccountInfo factoryInfo) {
        if (factoryInfo == null) {
            return;
        }
        if (factoryInfo.getAccountName() != null && !factoryInfo.getAccountName().isEmpty()) {
            this.factoryName = factoryInfo.getAccountName();
        }
        if (factoryInfo.getAccountOwnerName() != null) {
            this.factoryOwnerName = factoryInfo.getAccountOwnerName();
        }
        if (factoryInfo.getAccountPhoneNumber() != null) {
            this.factoryPhoneNumber = factoryInfo.getAccountPhoneNumber();
        }
        if (factoryInfo.getAccountContactNumber1() != null) {
            this.factoryContactNumber1 = factoryInfo.getAccountContactNumber1();
        }
        if (factoryInfo.getAccountContactNumber2() != null) {
            this.factoryContactNumber2 = factoryInfo.getAccountContactNumber2();
        }
        if (factoryInfo.getAccountFaxNumber() != null) {
            this.factoryFaxNumber = factoryInfo.getAccountFaxNumber();
        }
        if (factoryInfo.getAccountNote() != null) {
            this.factoryNote = factoryInfo.getAccountNote();
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

    public void updateCommonOption(CommonOptionDto.CommonOptionInfo optionInfo, GoldHarry goldHarry) {
        this.commonOption.updateTradeTypeAndOptionLevel(optionInfo);
        this.commonOption.updateGoldHarry(goldHarry);
    }

    public void updateBalance(BigDecimal goldAmount, Long moneyAmount) {
        this.currentGoldBalance = this.currentGoldBalance.add(goldAmount);
        this.currentMoneyBalance += moneyAmount;
    }

    public boolean isNameChanged(String factoryName) {
        return !this.factoryName.equals(factoryName);
    }
}
