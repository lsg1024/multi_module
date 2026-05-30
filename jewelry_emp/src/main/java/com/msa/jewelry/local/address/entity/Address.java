package com.msa.jewelry.local.address.entity;

import com.msa.jewelry.local.address.dto.AddressDto;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "주소 엔티티 — 거래처/제조사의 우편번호 + 기본주소 + 상세주소")
public class Address {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADDRESS_ID")
    @Schema(description = "주소 PK", example = "100")
    private Long addressId;
    @Column(name = "ADDRESS_ZIP_CODE")
    @Schema(description = "우편번호", example = "06236")
    private String addressZipCode;
    @Column(name = "ADDRESS_BASIC")
    @Schema(description = "기본 주소", example = "서울특별시 강남구 테헤란로 123")
    private String addressBasic;
    @Column(name = "ADDRESS_ADD")
    @Schema(description = "상세 주소", example = "4층 401호")
    private String addressAdd;
    @Schema(description = "소프트 삭제 플래그", example = "false")
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
