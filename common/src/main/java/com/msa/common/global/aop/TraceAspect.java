package com.msa.common.global.aop;

import com.msa.common.global.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class TraceAspect {
    @Pointcut("execution(* com.msa..*(..))")
    private void allMsa() {}

    @Pointcut("allMsa() && !@within(com.msa.common.global.aop.NoTrace) && !@annotation(com.msa.common.global.aop.NoTrace)")
    private void traceLogTarget() {}

    @Around("traceLogTarget()")
    public Object doTrace(ProceedingJoinPoint joinPoint) throws Throwable {
        String tenant = TenantContext.getTenant();
        log.info("[TRACE START: " + tenant + "] method: {}", joinPoint.getSignature());
        try {
            Object result = joinPoint.proceed();
            log.info("[TRACE END: " + tenant + "] method: {}", joinPoint.getSignature());
            return result;
        } catch (Throwable e) {
            log.error("[TRACE EXCEPTION: " + tenant + "] method: {}, exception: {}", joinPoint.getSignature(), e.getMessage());
            throw e;
        }
    }

}
