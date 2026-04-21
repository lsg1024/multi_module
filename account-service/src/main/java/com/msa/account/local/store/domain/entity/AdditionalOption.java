package com.msa.account.local.store.domain.entity;

import com.msa.account.global.domain.dto.AdditionalOptionDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Getter
@Table(name = "ADDITIONAL_OPTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE ADDITIONAL_OPTION SET DELETED = TRUE WHERE OPTION_ID = ?")
public class AdditionalOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OPTION_ID")
    private Long optionId;
    @Column(name = "OPTION_APPLY_PAST_SALES")
    private boolean optionApplyPastSales;
    @Column(name = "OPTION_MATERIAL_ID" )// product server mapping 필요
    private String optionMaterialId;
    @Column(name = "OPTION_MATERIAL_NAME") // 싱글 조회용
    private String optionMaterialName;
    private boolean deleted = false;

    @Builder
    public AdditionalOption(boolean optionApplyPastSales, String optionMaterialId, String optionMaterialName) {
        this.optionApplyPastSales = optionApplyPastSales;
        this.optionMaterialId = optionMaterialId;
        this.optionMaterialName = optionMaterialName;
    }

    /**
     * 부가 옵션을 부분 업데이트한다.
     * optionMaterialId / optionMaterialName 이 null 이면 기존 값 유지
     * (payload 누락 시 DB 값이 null 로 덮어써지는 현상 방지).
     * boolean 필드는 원시 타입이므로 항상 갱신된다.
     */
    public void update(AdditionalOptionDto.AdditionalOptionInfo info) {
        if (info == null) {
            return;
        }
        this.optionApplyPastSales = info.isAdditionalApplyPastSales();
        if (info.getAdditionalMaterialId() != null) {
            this.optionMaterialId = info.getAdditionalMaterialId();
        }
        if (info.getAdditionalMaterialName() != null) {
            this.optionMaterialName = info.getAdditionalMaterialName();
        }
    }
}
