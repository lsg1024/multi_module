package com.msa.product.local.stone.assistantStone.entity;

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
public class AssistantStone {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASSISTANCE_STONE_ID")
    private Long assistanceStoneId;
    @Column(name = "ASSISTANCE_STONE_NAME")
    private String assistanceStoneName;
    @Column(name = "ASSISTANCE_STONE_NOTE")
    private String assistanceStoneNote;
    @Column(name = "ASSISTANCE_STONE_DELETED")
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
