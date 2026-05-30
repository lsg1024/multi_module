package com.msa.jewelry.local.imagesearch.admin;

import com.msa.common.global.tenant.TenantContext;
import com.msa.jewelry.local.imagesearch.admin.ImageEmbeddingBackfillService.BackfillJob;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 백필(일괄 인덱싱) 관리 API.
 *
 * 운영자만 접근 가능하도록 SecurityConfig에서 ROLE_ADMIN 제한 권장.
 * 단일 인스턴스 가정 — 동시에 1개의 job만 진행됨.
 */
@RestController
@RequestMapping("/admin/embeddings/backfill")
@RequiredArgsConstructor
public class ImageEmbeddingBackfillController {

    private final ImageEmbeddingBackfillService service;

    @PostMapping("/start")
    public BackfillJob start(@RequestBody(required = false) StartRequest req) {
        StartRequest body = (req != null) ? req : new StartRequest(50, false);
        return service.start(TenantContext.getTenant(), body.chunkSize(), body.dryRun());
    }

    @GetMapping("/status")
    public BackfillJob status() {
        return service.status();
    }

    @PostMapping("/cancel")
    public BackfillJob cancel() {
        return service.cancel();
    }

    public record StartRequest(int chunkSize, boolean dryRun) {
        public StartRequest {
            if (chunkSize <= 0) chunkSize = 50;
        }
    }
}
