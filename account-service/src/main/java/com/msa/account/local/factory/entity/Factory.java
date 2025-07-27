package com.msa.account.local.factory.entity;

import com.msa.account.local.factory.dto.FactoryDto;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.Address;
import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msacommon.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLDelete;

@Getter
@Entity
@Table(name = "FACTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE FACTORY SET FACTORY_DELETED = TRUE WHERE FACTORY_ID = ?")
public class Factory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACTORY_ID")
    private Long factoryId;

    @Column(name = "FACTORY_NAME", unique = true, nullable = false, length = 30)
    private String factoryName;
    @Column(name = "FACTORY_OWNER_NAME", nullable = false, length = 30)
    private String factoryOwnerName;
    @Column(name = "FACTORY_PHONE_NUMBER", length = 13)
    private String factoryPhoneNumber;
    @Column(name = "FACTORY_CONTACT_NUMBER_1", length = 13)
    private String factoryContactNumber1;
    @Column(name = "FACTORY_CONTACT_NUMBER_2", length = 13)
    private String factoryContactNumber2;
    @Column(name = "FACTORY_FAX_NUMBER", length = 16)
    private String factoryFaxNumber;
    @Column(name = "FACTORY_NOTE")
    private String factoryNote;
    @Column(name = "FACTORY_DELETED", nullable = false)
    private boolean factoryDeleted = false;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ADDRESS_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Address address;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "COMMON_OPTION_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
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
    public void updateFactoryInfo(FactoryDto.FactoryInfo factoryInfo) {
        this.factoryName = factoryInfo.getFactoryName();
        this.factoryOwnerName = factoryInfo.getFactoryOwnerName();
        this.factoryContactNumber1 = factoryInfo.getFactoryContactNumber1();
        this.factoryContactNumber2 = factoryInfo.getFactoryContactNumber2();
        this.factoryFaxNumber = factoryInfo.getFactoryFaxNumber();
        this.factoryNote = factoryInfo.getFactoryNote();
    }
    public void updateAddressInfo(AddressDto.AddressInfo addressInfo) {
        this.address.update(addressInfo);
    }

    public void updateCommonOption(CommonOptionDto.CommonOptionInfo optionInfo, GoldHarry goldHarry) {
        this.commonOption.updateTradeTypeAndOptionLevel(optionInfo);
        this.commonOption.updateGoldHarry(goldHarry);
    }

    public boolean isNameChanged(String factoryName) {
        return !this.factoryName.equals(factoryName);
    }
}
