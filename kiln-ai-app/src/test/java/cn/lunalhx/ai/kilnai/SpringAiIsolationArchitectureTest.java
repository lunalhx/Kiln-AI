package cn.lunalhx.ai.kilnai;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cn.lunalhx.ai.kilnai", importOptions = ImportOption.DoNotIncludeTests.class)
class SpringAiIsolationArchitectureTest {

    @ArchTest
    static final ArchRule springAiTypesStayInModelAdapter = noClasses()
            .that().resideOutsideOfPackage("cn.lunalhx.ai.kilnai.infrastructure.adapter.model..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.ai..");

    @ArchTest
    static final ArchRule domainApiAndTriggerStayClearOfSpringAi = noClasses()
            .that().resideInAnyPackage(
                    "cn.lunalhx.ai.kilnai.domain..",
                    "cn.lunalhx.ai.kilnai.api..",
                    "cn.lunalhx.ai.kilnai.trigger.."
            )
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.ai..");

    @ArchTest
    static final ArchRule persistenceDoesNotImportSpringAi = noClasses()
            .that().resideInAnyPackage("cn.lunalhx.ai.kilnai.infrastructure.adapter.repository..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.ai..");

    @ArchTest
    static final ArchRule schedulersNeverTouchApplyOrModel = noClasses()
            .that().resideInAnyPackage("cn.lunalhx.ai.kilnai.trigger.schedule..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "cn.lunalhx.ai.kilnai.domain.apply..",
                    "cn.lunalhx.ai.kilnai.infrastructure.adapter.model..",
                    "org.springframework.ai.."
            );
}
