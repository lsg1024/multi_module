package com.msa.product.global.exception;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValue {
    Class<? extends Enum<?>> enumClass();

    String message() default "허용되지 않은 값입니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
