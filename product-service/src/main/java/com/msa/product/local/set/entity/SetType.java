package com.msa.product.local.set.entity;

import com.msa.product.local.set.dto.SetTypeDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SET_TYPE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SetType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SET_TYPE_ID")
    private Long setTypeId;
    @Column(name = "SET_TYPE_NAME", unique = true)
    private String setTypeName;
    @Column(name = "SET_TYPE_NOTE")
    private String setTypeNote;

    @Builder
    public SetType(String setTypeName, String setTypeNote) {
        this.setTypeName = setTypeName;
        this.setTypeNote = setTypeNote;
    }

    public String getSetTypeName() {
        return setTypeName;
    }

    public String getSetTypeNote() {
        return setTypeNote;
    }

    public void updateSetType(SetTypeDto setTypeDto) {
        this.setTypeName = setTypeDto.getName();
        this.setTypeNote = setTypeDto.getNote();
    }
}
