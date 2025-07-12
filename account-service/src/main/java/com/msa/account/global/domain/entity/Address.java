package com.msa.account.global.domain.entity;

import com.msa.account.global.domain.dto.AddressDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "ADDRESS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE ADDRESS SET DELETED = TRUE WHERE ADDRESS_ID = ?")
public class Address {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADDRESS_ID")
    private Long addressId;
    @Column(name = "ADDRESS_ZIP_CODE")
    private String addressZipCode;
    @Column(name = "ADDRESS_BASIC")
    private String addressBasic;
    @Column(name = "ADDRESS_ADD")
    private String addressAdd;
    private boolean deleted = false;

    @Builder
    public Address(String addressZipCode, String addressBasic, String addressAdd) {
        this.addressZipCode = addressZipCode;
        this.addressBasic = addressBasic;
        this.addressAdd = addressAdd;
    }

    public void update(AddressDto.AddressInfo info) {
        this.addressZipCode = info.getAddressZipCode();
        this.addressBasic = info.getAddressBasic();
        this.addressAdd = info.getAddressAdd();
    }
}
