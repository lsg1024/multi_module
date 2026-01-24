package com.msa.order.local.order.util;

import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.Kind;
import com.msa.order.local.order.entity.order_enum.SourceType;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;

/**
 * StatusHistory 저장 로직을 통합하는 헬퍼 클래스
 * 중복 코드 제거 및 일관된 StatusHistory 관리를 위해 사용됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusHistoryHelper {

    private final StatusHistoryRepository statusHistoryRepository;

    /**
     * 초기 StatusHistory를 생성하고 저장합니다.
     * 주문 또는 재고 생성 시 사용됩니다.
     *
     * @param flowCode  플로우 코드
     * @param sourceType 소스 타입 (ORDER, STOCK 등)
     * @param phase     비즈니스 페이즈
     * @param nickname  생성자 닉네임
     * @return 저장된 StatusHistory
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StatusHistory saveCreate(Long flowCode, SourceType sourceType, BusinessPhase phase, String nickname) {
        StatusHistory statusHistory = StatusHistory.create(
                flowCode,
                sourceType,
                phase,
                Kind.CREATE,
                nickname
        );

        StatusHistory saved = statusHistoryRepository.save(statusHistory);
        log.debug("StatusHistory 생성: flowCode={}, phase={}, kind=CREATE", flowCode, phase);
        return saved;
    }

    /**
     * 상태 변경 StatusHistory를 생성하고 저장합니다.
     *
     * @param flowCode   플로우 코드
     * @param sourceType 소스 타입
     * @param fromPhase  변경 전 페이즈
     * @param toPhase    변경 후 페이즈
     * @param nickname   변경자 닉네임
     * @return 저장된 StatusHistory
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StatusHistory savePhaseChange(Long flowCode, SourceType sourceType,
                                        BusinessPhase fromPhase, BusinessPhase toPhase, String nickname) {
        StatusHistory statusHistory = StatusHistory.phaseChange(
                flowCode,
                sourceType,
                fromPhase,
                toPhase,
                nickname
        );

        StatusHistory saved = statusHistoryRepository.save(statusHistory);
        log.debug("StatusHistory 상태 변경: flowCode={}, {} -> {}", flowCode, fromPhase, toPhase);
        return saved;
    }

    /**
     * 마지막 StatusHistory를 조회하여 상태를 변경합니다.
     * 이전 상태를 자동으로 조회하여 새로운 상태로 전환합니다.
     *
     * @param flowCode 플로우 코드
     * @param toPhase  변경 후 페이즈
     * @param nickname 변경자 닉네임
     * @return 저장된 StatusHistory
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StatusHistory savePhaseChangeFromLast(Long flowCode, BusinessPhase toPhase, String nickname) {
        StatusHistory lastHistory = statusHistoryRepository
                .findTopByFlowCodeOrderByIdDesc(flowCode)
                .orElseThrow(() -> new IllegalArgumentException("이전 상태 이력: " + NOT_FOUND));

        return savePhaseChange(
                flowCode,
                lastHistory.getSourceType(),
                BusinessPhase.valueOf(lastHistory.getToValue()),
                toPhase,
                nickname
        );
    }
}
