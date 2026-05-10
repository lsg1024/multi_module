package com.msa.jewelry.shared.exception;

/**
 * 모놀리스 전체에서 사용하는 도메인 예외 base.
 *
 * <p>기존 MSA 시절 각 서비스의 {@code IllegalArgumentException} +
 * {@code FeignClientException} 으로 분산되어 있던 예외 처리를 통합.
 * GlobalExceptionHandler 가 이 base 를 catch 해 4xx 응답 매핑.
 */
public abstract class DomainException extends RuntimeException {

    private final int httpStatus;

    protected DomainException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
