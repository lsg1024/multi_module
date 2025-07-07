package com.msa.account.global.domain.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OptionLevel {

    ONE("1", 1),
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4);

    private final String key;
    private final int level;
}
