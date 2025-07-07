package com.msa.account.domain.store.entity;

import com.msa.account.global.domain.entity.Address;
import com.msa.account.global.domain.entity.CommonOption;
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
@Table(name = "STORE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE STORE SET STORE_DELETED = TRUE WHERE STORE_ID = ?")
public class Store extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STORE_ID")
    private Long storeId;
    @Column(name = "STORE_NAME", nullable = false, length = 30)
    private String storeName;
    @Column(name = "STORE_OWNER_NAME", nullable = false, length = 30)
    private String storeOwnerName;
    @Column(name = "STORE_PHONE_NUMBER", length = 13)
    private String storePhoneNumber;
    @Column(name = "STORE_CONTACT_NUMBER_1", length = 13)
    private String storeContactNumber1;
    @Column(name = "STORE_CONTACT_NUMBER_2", length = 13)
    private String storeContactNumber2;
    @Column(name = "STORE_TRADE_PLACE", length = 50)
    private String storeTradePlace;
    @Column(name = "STORE_FAX_NUMBER", length = 16)
    private String storeFaxNumber;
    @Column(name = "STORE_NOTE")
    private String storeNote;
    @Column(name = "STORE_DELETED", nullable = false)
    private boolean storeDeleted = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADDRESS_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Address address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMMON_OPTIONS_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommonOption commonOption;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPTION_ID")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AdditionalOption additionalOption;

    @Builder
    public Store(String storeName, String storePhoneNumber, String storeContactNumber1, String storeContactNumber2, String storeTradePlace, String storeFaxNumber, String storeNote) {
        this.storeName = storeName;
        this.storePhoneNumber = storePhoneNumber;
        this.storeContactNumber1 = storeContactNumber1;
        this.storeContactNumber2 = storeContactNumber2;
        this.storeTradePlace = storeTradePlace;
        this.storeFaxNumber = storeFaxNumber;
        this.storeNote = storeNote;
    }
}
