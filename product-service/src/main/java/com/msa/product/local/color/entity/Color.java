package com.msa.product.local.color.entity;

import com.msa.product.local.color.dto.ColorDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "COLOR")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Color {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COLOR_ID")
    private Long colorId;
    @Column(name = "COLOR_NAME")
    private String colorName;
    @Column(name = "COLOR_NOTE")
    private String colorNote;
    @Column(name = "DEFAULT_ID")
    private boolean defaultId;

    @Builder
    public Color(Long colorId, String colorName, String colorNote) {
        this.colorId = colorId;
        this.colorName = colorName;
        this.colorNote = colorNote;
    }

    public void updateColor(ColorDto colorDto) {
        this.colorName = colorDto.getName();
        this.colorNote = colorDto.getNote();
    }
    public boolean isDeletable() {
        return !defaultId;
    }
}
