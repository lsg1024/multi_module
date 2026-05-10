package com.msa.jewelry;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith 모듈 경계 검증 테스트.
 *
 * <p>이 테스트가 통과하면 다음이 보장된다:
 * <ul>
 *   <li>각 모듈이 다른 모듈의 {@code internal} 패키지를 직접 참조하지 않음</li>
 *   <li>순환 의존이 없음</li>
 *   <li>각 모듈의 package-info.java 에 선언된 {@code allowedDependencies}
 *       범위 내에서만 의존 관계가 형성됨</li>
 * </ul>
 *
 * <p>모놀리스로 통합한 뒤에도 이 테스트가 깨지지 않게 유지하면 추후 다시
 * 마이크로서비스로 분리할 때의 비용이 극적으로 낮아진다.
 *
 * <p>실패 예시:
 * <pre>
 * Module 'order' depends on non-exposed type
 *   com.msa.jewelry.account.internal.Store within module 'account'!
 * </pre>
 * → order 모듈이 account 모듈의 internal 클래스를 직접 사용하고 있음.
 *   해결: account.api.StoreView record 를 통해 노출하도록 변경.
 */
class ModularityTests {

    private final ApplicationModules modules =
            ApplicationModules.of(JewelryEmpApplication.class);

    @Test
    void verifyModularStructure() {
        modules.verify();
    }

    @Test
    void writeDocumentationSnippets() {
        // build/spring-modulith-docs/ 에 PlantUML / AsciiDoc 으로 모듈 다이어그램 생성.
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
