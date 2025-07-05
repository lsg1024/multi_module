package com.msa.account.global.domain.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OptionLevel {

    ONE("FIRST", 1),
    TWO("TWO", 2),
    THREE("THREE", 3),
    FOUR("FOUR", 4);

    private final String key;
    private final int level;
}
