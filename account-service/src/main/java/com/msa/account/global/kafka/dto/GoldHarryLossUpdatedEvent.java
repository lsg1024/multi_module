package com.msa.account.global.kafka.dto;

public record GoldHarryLossUpdatedEvent(String tenantId, Long goldHarryId, String newGoldHarryLoss) {}
