package com.msa.jewelry.account.api;

/**
 * 다른 모듈이 제조사를 참조할 때 사용하는 불변 view DTO.
 *
 * <p>{@code goldHarryLoss} 는 String — 원본 엔티티 그대로.
 */
public record FactoryView(
        Long factoryId,
        String factoryName,
        String grade,
        String goldHarryLoss
) {
}
