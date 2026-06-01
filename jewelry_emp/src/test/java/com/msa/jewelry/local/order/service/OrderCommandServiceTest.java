package com.msa.jewelry.local.order.service;

import com.msa.jewelry.local.assistant_stone.service.AssistantStoneService;
import com.msa.jewelry.local.order.dto.OrderDto;
import com.msa.jewelry.local.order.repository.OrdersRepository;
import com.msa.jewelry.local.priority.repository.PriorityRepository;
import com.msa.jewelry.local.stone.service.StoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * OrderCommandService 단위 테스트.
 * <p>
 * 모놀리스 통합으로 OrderCommandService 와 OrderProcessingService 가 한 클래스로 합쳐졌고,
 * 핵심 happy-path 흐름은 OrdersServiceIntegrationTest 의 @SpringBootTest 가 커버한다.
 * 여기서는 외부 의존(DB/날짜 파싱) 없이도 검증 가능한 입력 가드를 단위 테스트로 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderCommandService 단위 테스트")
class OrderCommandServiceTest {

    @Mock OrdersRepository ordersRepository;
    @Mock PriorityRepository priorityRepository;
    @Mock AssistantStoneService assistantStoneService;
    @Mock StoneService stoneService;

    @InjectMocks OrderCommandService orderCommandService;

    @Test
    @DisplayName("priorityName 으로 우선순위를 못 찾으면 IllegalArgumentException — 주문은 저장되지 않는다")
    void 우선순위_없음_예외() {
        OrderDto.Request orderDto = mock(OrderDto.Request.class);
        // ID 게터들은 stub 안 해도 SafeParse 가 null-safe 로 처리하므로 우선순위 조회까지 도달한다.
        given(orderDto.getPriorityName()).willReturn("없는등급");
        given(priorityRepository.findByPriorityName("없는등급")).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderCommandService.createOrder("WAIT", orderDto))
                .isInstanceOf(IllegalArgumentException.class);

        // 우선순위 검증 단계에서 끊기므로 어떤 주문도 저장되면 안 된다.
        verify(ordersRepository, never()).save(any());
    }
}
