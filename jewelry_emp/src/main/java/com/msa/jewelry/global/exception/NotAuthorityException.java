package com.msa.jewelry.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 권한 부족 예외 (HTTP 403 매핑).
// 과거 jakarta.ws.rs.ForbiddenException 를 상속했으나, JAX-RS 구현체(Jersey/RESTEasy)
// 가 classpath 에 없어 인스턴스화 시 RuntimeDelegate provider not found 로 터지는
// 버그가 있었음. Spring MVC 만 사용하므로 RuntimeException 으로 변경.
@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotAuthorityException extends RuntimeException {
    public NotAuthorityException(String msg) {
        super(msg);
    }
}
