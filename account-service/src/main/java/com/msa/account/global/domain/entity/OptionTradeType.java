package com.msa.account.global.domain.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OptionTradeType {

    WEIGHT("1", "중량"),
    PRICE("2", "시세");

    private final String key;
    private final String title;
}
