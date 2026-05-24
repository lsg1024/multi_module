package com.msa.jewelry.product.api;

/**
 * 다른 모듈이 색상(Color) 정보를 조회할 때 사용하는 공개 API.
 *
 * <p>기존 MSA 의 ColorClient(Feign wrapper) 동기 등가물.
 */
public interface ColorFinder {

    /**
     * 색상 ID 로 이름 조회.
     * @throws com.msa.jewelry.shared.exception.NotFoundException 미존재 시
     */
    String getColorName(Long colorId);

    /**
     * 색상 이름으로 ID 조회. 없으면 null.
     */
    Long findColorIdByName(String colorName);
}
