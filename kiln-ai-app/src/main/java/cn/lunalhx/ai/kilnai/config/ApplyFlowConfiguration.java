package cn.lunalhx.ai.kilnai.config;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ApplyFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle.BundleLoader;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle.SkillBundleSource;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Wires the durable, idempotent Apply flow. The model ports fail closed with
 * a neutral unavailable result until operator-configured adapters are present;
 * scripted or real ports registered elsewhere take precedence.
 */
@Configuration
public class ApplyFlowConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    ArtifactStore inMemoryArtifactStore(Clock clock) {
        return new InMemoryArtifactStore(clock);
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    LearningFlowStore inMemoryLearningFlowStore() {
        return new InMemoryLearningFlowStore();
    }

    @Bean
    @ConditionalOnMissingBean(ApplyGenerationPort.class)
    ApplyGenerationPort failClosedApplyGeneration() {
        return (compiledSystemPrompt, executionContextJson) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "apply generation adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnMissingBean(TaskVerifierPort.class)
    TaskVerifierPort failClosedApplyTaskVerifier() {
        return (taskPackage, context) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "apply task verifier adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnMissingBean(AssessmentPort.class)
    AssessmentPort failClosedApplyAssessment() {
        return context -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "apply assessment adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnMissingBean(ResponseVerificationPort.class)
    ResponseVerificationPort failClosedApplyResponseVerification() {
        return context -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "apply response verification adapter is not configured");
        };
    }

    @Bean
    BundleStack applyBundleStack() {
        BundleLoader loader = new BundleLoader();
        return new BundleStack(ApplyProfile.FIXED_STACK.stream()
                .map(loader::load)
                .map(SkillBundleSource::toBundle)
                .toList());
    }

    @Bean
    ApplyProfileExecutor applyProfileExecutor(
            BundleStack stack,
            ApplyGenerationPort generationPort,
            TaskVerifierPort verifierPort,
            ArtifactStore artifactStore
    ) {
        return new ApplyProfileExecutor(stack, generationPort, verifierPort, artifactStore);
    }

    @Bean
    DiagnosticFlow diagnosticFlow(
            ApplyProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            Clock clock
    ) {
        return new DiagnosticFlow(
                executor, artifactStore, flowStore, assessmentPort, verificationPort,
                DiagnosticApplyFixture.diagnosticContext(),
                IndependentApplyFixture.independentContext(),
                clock);
    }

    @Bean
    IndependentSubmissionFlow independentSubmissionFlow(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            Clock clock
    ) {
        return new IndependentSubmissionFlow(artifactStore, flowStore, assessmentPort, verificationPort, clock);
    }

    @Bean
    ApplyFlowUseCase applyFlowUseCase(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            Clock clock
    ) {
        return new ApplyFlowUseCase(
                artifactStore, flowStore, diagnosticFlow, independentFlow,
                DiagnosticApplyFixture.diagnosticContext(), clock);
    }
}
