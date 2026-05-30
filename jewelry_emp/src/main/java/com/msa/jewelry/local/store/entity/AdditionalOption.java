package com.msa.jewelry.local.store.entity;

import com.msa.jewelry.local.common_option.dto.AdditionalOptionDto;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "거래처 부가 옵션 엔티티 — 과거 판매분 적용 여부, 특수 재질 매핑 등")
public class AdditionalOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OPTION_ID")
    @Schema(description = "부가 옵션 PK", example = "1")
    private Long optionId;
    @Column(name = "OPTION_APPLY_PAST_SALES")
    @Schema(description = "과거 판매분 적용 여부", example = "false")
    private boolean optionApplyPastSales;
    @Column(name = "OPTION_MATERIAL_ID" )// product server mapping 필요
    @Schema(description = "재질 ID (product 모듈 매핑 필요)", example = "10")
    private String optionMaterialId;
    @Column(name = "OPTION_MATERIAL_NAME") // 싱글 조회용
    @Schema(description = "재질명 (싱글 조회 캐시용)", example = "18K")
    private String optionMaterialName;
    @Schema(description = "소프트 삭제 플래그", example = "false")
    private boolean deleted = false;

    @Builder
    public AdditionalOption(boolean optionApplyPastSales, String optionMaterialId, String optionMaterialName) {
        this.optionApplyPastSales = optionApplyPastSales;
        this.optionMaterialId = optionMaterialId;
        this.optionMaterialName = optionMaterialName;
    }

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
