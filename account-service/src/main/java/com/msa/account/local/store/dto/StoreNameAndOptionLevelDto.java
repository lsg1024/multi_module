package com.msa.account.local.store.dto;

import com.msa.account.global.domain.entity.OptionLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoreNameAndOptionLevelDto {
    private String storeName;
    private String grade;

    public StoreNameAndOptionLevelDto(String storeName, OptionLevel optionLevel) {
        this.storeName = storeName;
        this.grade = optionLevel.getLevel();;
    }

}
