package com.msa.jewelry.local.imagesearch.admin;

import com.msa.common.global.tenant.TenantContext;
import com.msa.jewelry.local.imagesearch.client.ImageSearchClient;
import com.msa.jewelry.local.imagesearch.dto.ImageSearchDtos.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 9000장 일괄 인덱싱(백필) 진행 추적기.
 *
 * 상태는 in-memory(AtomicReference) — 단일 인스턴스 운영 가정.
 * 다중 인스턴스 운영 시 Redis로 이관 필요.
 *
 * 실제 ProductImage 일괄 조회는 BackfillRunner 인터페이스로 분리하여
 * product 도메인의 Repository를 주입받는 형태로 구현한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageEmbeddingBackfillService {

    private final ImageSearchClient client;
    private final BackfillRunner runner;

    private final AtomicReference<BackfillJob> currentJob = new AtomicReference<>();

    public BackfillJob start(String tenantId, int chunkSize, boolean dryRun) {
        BackfillJob existing = currentJob.get();
        if (existing != null && existing.getStatus() == BackfillStatus.RUNNING) {
            throw new IllegalStateException("Backfill already running — id=" + existing.getId());
        }

        BackfillJob job = new BackfillJob(UUID.randomUUID().toString(), tenantId, dryRun);
        currentJob.set(job);

        // 별도 스레드 (간단히 Thread; 운영에서는 ThreadPoolTaskExecutor 권장)
        new Thread(() -> execute(job, chunkSize), "img-embed-backfill-" + job.getId()).start();
        return job;
    }

    public BackfillJob status() {
        BackfillJob job = currentJob.get();
        if (job == null) {
            throw new IllegalStateException("No backfill job has been started");
        }
        return job;
    }

    public BackfillJob cancel() {
        BackfillJob job = currentJob.get();
        if (job == null || job.getStatus() != BackfillStatus.RUNNING) {
            throw new IllegalStateException("No running backfill to cancel");
        }
        job.setStatus(BackfillStatus.CANCELLED);
        return job;
    }

    // ============================================================
    private void execute(BackfillJob job, int chunkSize) {
        // 백그라운드 스레드는 ThreadLocal(TenantContext)이 비어 있다.
        // Hibernate schema-per-tenant 라우팅에 사용되므로 명시적으로 설정/정리해야 한다.
        TenantContext.setTenant(job.getTenantId());
        try {
            job.setStartedAt(Instant.now());
            int total = runner.countAll(job.getTenantId());
            job.setTotal(total);
            log.info("Backfill started — tenant={}, total={}", job.getTenantId(), total);

            int page = 0;
            while (true) {
                if (job.getStatus() == BackfillStatus.CANCELLED) {
                    log.warn("Backfill cancelled at page {}", page);
                    return;
                }

                List<EmbedItem> items = runner.loadPage(job.getTenantId(), page, chunkSize);
                if (items.isEmpty()) {
                    break;
                }

                if (!job.isDryRun()) {
                    EmbedResponse resp = client.embed(new EmbedRequest(job.getTenantId(), items));
                    job.addDone(resp.succeeded() == null ? 0 : resp.succeeded().size());
                    if (resp.failed() != null) {
                        for (EmbedFailure f : resp.failed()) {
                            job.addFailure(f.productImageId(), f.reason());
                        }
                    }
                } else {
                    job.addDone(items.size());
                }
                page++;
            }

            job.setStatus(BackfillStatus.COMPLETED);
            job.setFinishedAt(Instant.now());
            log.info("Backfill completed — tenant={}, done={}, failed={}",
                    job.getTenantId(), job.getDone(), job.getFailures().size());
        } catch (Exception e) {
            log.error("Backfill failed", e);
            job.setStatus(BackfillStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(Instant.now());
        } finally {
            TenantContext.clear();
        }
    }

    // ============================================================
    // 페이지 로더 인터페이스 — 실제 구현은 ProductImageRepository 활용
    // ============================================================
    public interface BackfillRunner {
        int countAll(String tenantId);

        /** 0-based 페이지 단위로 (productId, productImageId, imagePath) 묶음 반환 */
        List<EmbedItem> loadPage(String tenantId, int page, int size);
    }

    // ============================================================
    // 진행 상태 모델
    // ============================================================
    public enum BackfillStatus {
        RUNNING, COMPLETED, FAILED, CANCELLED
    }

    @Getter
    public static class BackfillJob {
        private final String id;
        private final String tenantId;
        private final boolean dryRun;
        private volatile BackfillStatus status = BackfillStatus.RUNNING;
        private volatile int total = 0;
        private volatile int done = 0;
        private volatile Instant startedAt;
        private volatile Instant finishedAt;
        private volatile String errorMessage;
        private final List<FailedItem> failures = Collections.synchronizedList(new ArrayList<>());

        public BackfillJob(String id, String tenantId, boolean dryRun) {
            this.id = id;
            this.tenantId = tenantId;
            this.dryRun = dryRun;
        }

        synchronized void addDone(int n) { this.done += n; }
        synchronized void addFailure(long productImageId, String reason) {
            failures.add(new FailedItem(productImageId, reason));
        }

        public void setStatus(BackfillStatus s) { this.status = s; }
        public void setStartedAt(Instant t)    { this.startedAt = t; }
        public void setFinishedAt(Instant t)   { this.finishedAt = t; }
        public void setTotal(int t)            { this.total = t; }
        public void setErrorMessage(String m)  { this.errorMessage = m; }

        public record FailedItem(long productImageId, String reason) {}
    }
}
