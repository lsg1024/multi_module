package com.msa.jewelry.local.assistant_stone.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ASSISTANCE_STONE")
@SQLDelete(sql = "UPDATE ASSISTANCE_STONE SET ASSISTANCE_STONE_DELETED = TRUE WHERE ASSISTANCE_STONE_ID = ?")
@Schema(description = "보조석 마스터 엔티티 — 메인 스톤 외 보조석(사이드 스톤)으로 사용되는 보석. 엔티티/컬럼은 영어 표기 'assistance'.")
public class AssistantStone {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASSISTANCE_STONE_ID")
    @Schema(description = "보조석 PK", example = "201")
    private Long assistanceStoneId;
    @Column(name = "ASSISTANCE_STONE_NAME")
    @Schema(description = "보조석 이름", example = "큐빅 0.05ct")
    private String assistanceStoneName;
    @Column(name = "ASSISTANCE_STONE_NOTE")
    @Schema(description = "보조석 비고", example = "큐빅 지르코니아")
    private String assistanceStoneNote;
    @Column(name = "ASSISTANCE_STONE_DELETED")
    @Schema(description = "소프트 삭제 플래그", example = "false")
    private boolean assistanceStoneDeleted = false;

    @Builder
    public AssistantStone(String assistanceStoneName, String assistanceStoneNote) {
        this.assistanceStoneName = assistanceStoneName;
        this.assistanceStoneNote = assistanceStoneNote;
    }

    public void updateAssistantStone(String assistantStoneName, String assistanceStoneNote) {
        this.assistanceStoneName = assistantStoneName;
        this.assistanceStoneNote = assistanceStoneNote;
    }
}
