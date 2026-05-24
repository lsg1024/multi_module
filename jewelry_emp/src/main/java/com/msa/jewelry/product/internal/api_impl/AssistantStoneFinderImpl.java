package com.msa.jewelry.product.internal.api_impl;

import com.msa.jewelry.product.api.AssistantStoneFinder;
import com.msa.jewelry.product.api.AssistantStoneView;
import com.msa.jewelry.product.internal.stone.assistantStone.entity.AssistantStone;
import com.msa.jewelry.product.internal.stone.assistantStone.repository.AssistantStoneRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AssistantStoneFinder} 의 같은 JVM 동기 구현체.
 *
 * <p>엔티티 필드명은 "assistance" 표기(assistanceStoneId 등)이지만 외부 API view 와
 * 호출자 측 명명 일관성을 위해 "assistant" 로 노출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssistantStoneFinderImpl implements AssistantStoneFinder {

    private final AssistantStoneRepository assistantStoneRepository;

    @Override
    public AssistantStoneView getAssistantStone(Long assistantStoneId) {
        AssistantStone entity = assistantStoneRepository.findById(assistantStoneId)
                .orElseThrow(() -> new NotFoundException("보조석 미존재: assistantStoneId=" + assistantStoneId));
        return new AssistantStoneView(
                entity.getAssistanceStoneId(),
                entity.getAssistanceStoneName(),
                entity.getAssistanceStoneNote()
        );
    }
}
