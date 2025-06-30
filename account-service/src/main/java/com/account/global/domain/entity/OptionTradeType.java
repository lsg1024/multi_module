package com.account.global.domain.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OptionTradeType {

    WEIGHT("WEIGHT", "중량"),
    PRICE("PRICE", "시세");

    private final String key;
    private final String title;
}
