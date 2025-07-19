package com.msa.account.global.domain.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msa.account.global.domain.dto.GoldHarryDto;
import com.msa.account.global.domain.service.GoldHarryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gold-harry")
public class GoldHarryController {

    private final GoldHarryService goldHarryService;

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateLoss(@PathVariable Long id, @RequestBody GoldHarryDto.Update request) throws JsonProcessingException {
        goldHarryService.updateLoss(id, request);
        return ResponseEntity.ok().build();
    }
}