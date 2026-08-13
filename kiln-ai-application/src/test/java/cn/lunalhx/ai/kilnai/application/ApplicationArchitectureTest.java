package cn.lunalhx.ai.kilnai.application;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cn.lunalhx.ai.kilnai.application", importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationArchitectureTest {

    @ArchTest
    static final ArchRule applicationMustNotDependOnAdapters = noClasses()
            .that().resideInAnyPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.boot..",
                    "org.apache.ibatis..",
                    "com.alibaba.cloud.ai.graph..",
                    "org.springframework.ai.."
            );
}
