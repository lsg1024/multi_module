package com.msa.jewelry.order.internal.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class DateDto {
    private OffsetDateTime deliveryDate;
}
