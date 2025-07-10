package com.msa.account.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "ADDITIONAL_OPTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE ADDITIONAL_OPTION SET DELETED = TRUE WHERE OPTION_ID = ?")
public class AdditionalOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OPTION_ID")
    private String optionId;
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
}
