package com.msa.product.global.kafka.dto;

public record ClassificationEvent(String tenantId, Long classificationId) {
}
