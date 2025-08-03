package com.msa.account.local.store.kafka;

import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.CommonOptionRepository;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.domain.service.GoldHarryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"goldHarryLoss.update"})
public class GoldHarryLossKafkaE2ETest {
    @Autowired
    GoldHarryService goldHarryService;
    @Autowired
    CommonOptionRepository commonOptionRepository;
    @Autowired
    GoldHarryRepository goldHarryRepository;

    private GoldHarry goldHarry1;
    private GoldHarry goldHarry2;

    @BeforeEach
    void setup() {
        // 테스트 데이터 초기화
//        goldHarry1 = goldHarryRepository.save(new GoldHarry(new BigDecimal("1.11")));
//        goldHarry2 = goldHarryRepository.save(new GoldHarry(new BigDecimal("2.22")));
//
//        CommonOption option1 = CommonOption.builder()
//                .goldHarry(goldHarry1)
//                .goldHarryLoss("1.11")
//                .optionLevel(null)
//                .optionTradeType(null)
//                .build();
//
//        CommonOption option2 = CommonOption.builder()
//                .goldHarry(goldHarry1)
//                .goldHarryLoss("1.11")
//                .optionLevel(null)
//                .optionTradeType(null)
//                .build();
//
//        commonOptionRepository.saveAll(List.of(option1, option2));
    }

//    @Test
//    void goldHarryLoss_카프카_배치_E2E_성공() throws Exception {
//        //given & when: 서비스 메서드를 호출해서 값 변경 & Kafka 메시지 발행
//        goldHarryService.updateLoss(accessToken, 1L, new GoldHarryDto.Update("9.99"));
//
//        // 약간의 시간 대기 (배치, 카프카 컨슈머 비동기 실행 대기)
//        Thread.sleep(2000);
//
//        // then: CommonOption DB 값이 모두 9.99로 바뀌었는지 검증
//        List<CommonOption> updated = commonOptionRepository.findAll();
//        for (CommonOption commonOption : updated) {
//            Long goldHarryId = commonOption.getGoldHarry() != null ? commonOption.getGoldHarry().getGoldHarryId() : null;
//            log.info("commonOption goldHarryId={} goldHarryLoss={}", goldHarryId, commonOption.getGoldHarryLoss());
//        }
//
//        GoldHarry goldHarry = goldHarryRepository.findById(1L)
//                .orElseThrow(() -> new NotFoundException("not"));
//        commonOptionRepository.findAll();
//        System.out.println("goldHarryLoss = " + goldHarry.getGoldHarryLoss());
//    }

}