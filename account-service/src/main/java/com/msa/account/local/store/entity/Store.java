package com.msa.account.local.store.entity;

import com.msa.account.local.store.dto.StoreDto;
import com.msa.account.global.domain.dto.AdditionalOptionDto;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.Address;
import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.common.global.domain.BaseEntity;
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
@Table(name = "STORE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE STORE SET STORE_DELETED = TRUE WHERE STORE_ID = ?")
public class Store extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STORE_ID")
    private Long storeId;
    @Column(name = "STORE_NAME", unique = true, nullable = false, length = 30)
    private String storeName;
    @Column(name = "STORE_OWNER_NAME", nullable = false, length = 30)
    private String storeOwnerName;
    @Column(name = "STORE_PHONE_NUMBER", length = 13)
    private String storePhoneNumber;
    @Column(name = "STORE_CONTACT_NUMBER_1", length = 13)
    private String storeContactNumber1;
    @Column(name = "STORE_CONTACT_NUMBER_2", length = 13)
    private String storeContactNumber2;
    @Column(name = "STORE_FAX_NUMBER", length = 16)
    private String storeFaxNumber;
    @Column(name = "STORE_NOTE")
    private String storeNote;
    @Column(name = "STORE_DELETED", nullable = false)
    private boolean storeDeleted = false;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ADDRESS_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Address address;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "COMMON_OPTION_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommonOption commonOption;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "OPTION_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    public void updateStoreInfo(StoreDto.StoreInfo updateInfo) {
        this.storeName = updateInfo.getStoreName();
        this.storeOwnerName = updateInfo.getStoreOwnerName();
        this.storePhoneNumber = updateInfo.getStorePhoneNumber();
        this.storeContactNumber1 = updateInfo.getStoreContactNumber1();
        this.storeContactNumber2 = updateInfo.getStoreContactNumber2();
        this.storeFaxNumber = updateInfo.getStoreFaxNumber();
        this.storeNote = updateInfo.getStoreNote();
    }

    public void updateCommonOption(CommonOptionDto.CommonOptionInfo optionInfo, GoldHarry goldHarry) {
        this.commonOption.updateTradeTypeAndOptionLevel(optionInfo);
        this.commonOption.updateGoldHarry(goldHarry);
    }

    public void updateAddressInfo(AddressDto.AddressInfo addressInfo) {
        this.address.update(addressInfo);
    }

    public void updateAdditionalOption(AdditionalOptionDto.AdditionalOptionInfo optionInfo) {
        this.additionalOption.update(optionInfo);
    }

    public boolean isNameChanged(String storeName) {
        return !this.storeName.equals(storeName);
    }
}
