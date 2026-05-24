package com.msa.jewelry.product.api;

/**
 * 다른 모듈이 보조석(AssistantStone) 정보를 조회할 때 사용하는 공개 API.
 */
public interface AssistantStoneFinder {

    /**
     * 보조석 ID 로 view 조회.
     * @throws com.msa.jewelry.shared.exception.NotFoundException 미존재 시
     */
    AssistantStoneView getAssistantStone(Long assistantStoneId);
}
