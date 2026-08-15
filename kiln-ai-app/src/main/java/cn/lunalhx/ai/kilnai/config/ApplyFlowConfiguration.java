package cn.lunalhx.ai.kilnai.config;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ApplyFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCollectionUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewDueTransitionUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle.BundleLoader;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle.SkillBundleSource;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the durable, idempotent Apply flow. The model ports fail closed with
 * a neutral unavailable result until operator-configured adapters are present;
 * scripted or real ports registered elsewhere take precedence.
 */
@Configuration
public class ApplyFlowConfiguration {

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
    ReviewTaskScheduler reviewTaskScheduler(ReviewTaskStore reviewStore) {
        return new ReviewTaskScheduler(reviewStore);
    }

    @Bean
    ReviewDueTransitionUseCase reviewDueTransitionUseCase(ReviewTaskStore reviewStore, Clock clock) {
        return new ReviewDueTransitionUseCase(reviewStore, clock);
    }

    @Bean
    ReviewCollectionUseCase reviewCollectionUseCase(ReviewTaskStore reviewStore, LearningFlowStore flowStore) {
        return new ReviewCollectionUseCase(reviewStore, flowStore);
    }

    @Bean
    IndependentSubmissionFlow independentSubmissionFlow(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            ReviewTaskScheduler reviewScheduler,
            Clock clock
    ) {
        return new IndependentSubmissionFlow(
                artifactStore, flowStore, assessmentPort, verificationPort, reviewScheduler, clock);
    }

    @Bean
    ReviewStartFlow reviewStartFlow(
            ApplyProfileExecutor executor,
            LearningFlowStore flowStore,
            ReviewTaskStore reviewStore,
            Clock clock
    ) {
        return new ReviewStartFlow(
                executor, flowStore, reviewStore, ReviewApplyFixture.reviewContext(), clock);
    }

    @Bean
    ReviewSubmissionFlow reviewSubmissionFlow(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            ReviewTaskScheduler reviewScheduler,
            Clock clock
    ) {
        return new ReviewSubmissionFlow(
                artifactStore, flowStore, assessmentPort, verificationPort, reviewScheduler, clock);
    }

    @Bean
    ApplyFlowUseCase applyFlowUseCase(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            ReviewSubmissionFlow reviewFlow,
            Clock clock
    ) {
        return new ApplyFlowUseCase(
                artifactStore, flowStore, diagnosticFlow, independentFlow, reviewFlow,
                DiagnosticApplyFixture.diagnosticContext(), clock);
    }
}
