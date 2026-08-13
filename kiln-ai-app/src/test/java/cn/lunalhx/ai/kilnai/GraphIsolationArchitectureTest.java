package cn.lunalhx.ai.kilnai;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cn.lunalhx.ai.kilnai", importOptions = ImportOption.DoNotIncludeTests.class)
class GraphIsolationArchitectureTest {

    @ArchTest
    static final ArchRule graphTypesStayInAdapter = noClasses()
            .that().resideOutsideOfPackage("cn.lunalhx.ai.kilnai.graph.saa..")
            .should().dependOnClassesThat().resideInAnyPackage("com.alibaba.cloud.ai.graph..");

    @ArchTest
    static final ArchRule applicationDoesNotUseMvcOrMyBatis = noClasses()
            .that().resideInAnyPackage("cn.lunalhx.ai.kilnai.application..", "cn.lunalhx.ai.kilnai.api..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "org.apache.ibatis..",
                    "com.alibaba.cloud.ai.graph.."
            );
}
