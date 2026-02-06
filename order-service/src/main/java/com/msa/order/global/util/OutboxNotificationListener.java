package com.msa.order.global.util;

import com.msa.common.global.aop.NoTrace;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.AuditorHolder;
import com.msa.order.local.outbox.redis.OutboxRelayService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PostgreSQL LISTEN/NOTIFY 기반 Outbox 이벤트 수신기
 * - DB 폴링 없이 즉시 이벤트 감지
 * - 전용 커넥션 1개만 사용 (HikariPool 미사용)
 */
@NoTrace
@Slf4j
@Component
public class OutboxNotificationListener {

    private final DataSource dataSource;
    private final OutboxRelayService outboxRelayService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Connection listenConnection;

    public OutboxNotificationListener(@Qualifier("defaultDataSource") DataSource dataSource,
                                       OutboxRelayService outboxRelayService) {
        this.dataSource = dataSource;
        this.outboxRelayService = outboxRelayService;
    }

    @PostConstruct
    public void start() {
        running.set(true);
        executor.submit(this::listenLoop);
        log.info("[OutboxListener] PostgreSQL LISTEN 시작");
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        executor.shutdownNow();
        closeConnection();
        log.info("[OutboxListener] PostgreSQL LISTEN 종료");
    }

    private void listenLoop() {
        while (running.get()) {
            try {
                ensureConnection();
                PGConnection pgConn = listenConnection.unwrap(PGConnection.class);

                // 500ms 대기 후 알림 확인 (CPU 부하 방지)
                PGNotification[] notifications = pgConn.getNotifications(500);

                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        handleNotification(notification.getParameter());
                    }
                }

            } catch (Exception e) {
                log.warn("[OutboxListener] LISTEN 오류, 3초 후 재연결: {}", e.getMessage());
                closeConnection();
                sleep(3000);
            }
        }
    }

    /**
     * 알림 수신: "tenant_id:event_id" 형식
     * 해당 테넌트의 PENDING 이벤트를 즉시 처리
     */
    private void handleNotification(String payload) {
        String[] parts = payload.split(":", 2);
        if (parts.length < 2) return;

        String tenantId = parts[0];

        try {
            TenantContext.setTenant(tenantId);
            AuditorHolder.setAuditor(tenantId);

            outboxRelayService.relayAllEventsForTenant();

        } catch (Exception e) {
            log.error("[OutboxListener] 이벤트 처리 실패. Tenant: {}, Error: {}",
                    tenantId, e.getMessage());
        } finally {
            TenantContext.clear();
            AuditorHolder.clear();
        }
    }

    /**
     * LISTEN 전용 커넥션 확보
     * - HikariPool이 아닌 직접 커넥션 (영구 점유)
     * - AutoCommit = true (LISTEN 요구사항)
     */
    private void ensureConnection() throws SQLException {
        if (listenConnection == null || listenConnection.isClosed()) {
            listenConnection = dataSource.getConnection();
            listenConnection.setAutoCommit(true);

            try (Statement stmt = listenConnection.createStatement()) {
                stmt.execute("LISTEN outbox_channel");
            }

            log.info("[OutboxListener] LISTEN 커넥션 연결 완료");
        }
    }

    private void closeConnection() {
        if (listenConnection != null) {
            try {
                listenConnection.close();
            } catch (SQLException ignored) {}
            listenConnection = null;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
