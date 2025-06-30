package com.msa.userserver.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    ADMIN("ADMIN", "관리자"),
    USER("USER", "일반유저");

    private final String key;
    private final String title;
}
