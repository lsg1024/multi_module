package com.msa.account.global.domain.controller;

import com.msa.account.global.domain.dto.GoldHarryDto;
import com.msa.account.global.domain.service.GoldHarryService;
import com.msacommon.global.jwt.AccessToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class GoldHarryController {

    private final GoldHarryService goldHarryService;

    public GoldHarryController(GoldHarryService goldHarryService) {
        this.goldHarryService = goldHarryService;
    }

    @PatchMapping("/gold-harry/{id}")
    public ResponseEntity<Void> updateLoss(
            @AccessToken String accessToken,
            @PathVariable("id") Long id,
            @RequestBody GoldHarryDto.Update request) {
        goldHarryService.updateLoss(accessToken, id, request);
        return ResponseEntity.ok().build();
    }
}