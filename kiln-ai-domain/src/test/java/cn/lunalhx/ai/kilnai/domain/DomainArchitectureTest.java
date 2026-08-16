package cn.lunalhx.ai.kilnai.domain;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cn.lunalhx.ai.kilnai.domain", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnFrameworks = noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "org.apache.ibatis..",
                    "jakarta.persistence..",
                    "jakarta.servlet..",
                    "com.alibaba.cloud.ai.graph..",
                    "org.springframework.ai.."
            );

    @ArchTest
    static final ArchRule profilesAssessmentAndPedagogyAgentMustNotWriteLearningState = noClasses()
            .that().resideInAnyPackage(
                    "..domain.apply.profile..",
                    "..domain.learning.pedagogy..")
            .or().haveSimpleNameContaining("Assessment")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain.learning.graph..",
                    "..domain.apply.port.LearningFlowStore",
                    "..domain.apply.port.ReviewTaskStore"
            );
}
