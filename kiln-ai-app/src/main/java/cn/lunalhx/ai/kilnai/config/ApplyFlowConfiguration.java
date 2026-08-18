package cn.lunalhx.ai.kilnai.config;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ExplainApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.PracticeApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.TeachBackApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.HintFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExplainGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.HintGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackAssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackTaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassifierPort;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningFlowCommandUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningStateGraph;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPort;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCollectionUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCancellationUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewDueTransitionUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle.BundleLoader;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle.SkillBundleSource;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    OperatorModelProfilePort failClosedOperatorModelProfile() {
        return () -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE,
                    "operator model profile is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    ApplyGenerationPort failClosedApplyGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "apply generation adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    HintGenerationPort failClosedHintGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "hint generation adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    TeachBackGenerationPort failClosedTeachBackGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "teach-back generation adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    TeachBackTaskVerifierPort failClosedTeachBackTaskVerifier() {
        return (profile, taskPackage, context) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "teach-back task verifier adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    TeachBackAssessmentPort failClosedTeachBackAssessment() {
        return (profile, context) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "teach-back assessment adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    TaskVerifierPort failClosedApplyTaskVerifier() {
        return (profile, taskPackage, context) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "apply task verifier adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    AssessmentPort failClosedApplyAssessment() {
        return (profile, context) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "apply assessment adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    ResponseVerificationPort failClosedApplyResponseVerification() {
        return (profile, context) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "apply response verification adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    ExplainGenerationPort failClosedExplainGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "explain generation adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    PedagogyPort failClosedPedagogy() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "pedagogy adapter is not configured");
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "false", matchIfMissing = true)
    ClarificationClassifierPort failClosedClarificationClassifier() {
        return (profile, message, taskText) -> {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "clarification classifier adapter is not configured");
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
    BundleStack explainBundleStack() {
        BundleLoader loader = new BundleLoader();
        return new BundleStack(ExplainProfile.FIXED_STACK.stream()
                .map(loader::load)
                .map(SkillBundleSource::toBundle)
                .toList());
    }

    @Bean
    BundleStack teachBackBundleStack() {
        BundleLoader loader = new BundleLoader();
        return new BundleStack(TeachBackProfile.FIXED_STACK.stream()
                .map(loader::load)
                .map(SkillBundleSource::toBundle)
                .toList());
    }

    @Bean
    ApplyProfileExecutor applyProfileExecutor(
            @Qualifier("applyBundleStack") BundleStack stack,
            ApplyGenerationPort generationPort,
            TaskVerifierPort verifierPort,
            ArtifactStore artifactStore
    ) {
        return new ApplyProfileExecutor(stack, generationPort, verifierPort, artifactStore);
    }

    @Bean
    ExplainProfileExecutor explainProfileExecutor(
            @Qualifier("explainBundleStack") BundleStack stack,
            ExplainGenerationPort generationPort
    ) {
        return new ExplainProfileExecutor(stack, generationPort);
    }

    @Bean
    ExplainFlow explainFlow(
            ExplainProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore
    ) {
        return new ExplainFlow(executor, artifactStore, flowStore, ExplainApplyFixture.explainContext());
    }

    @Bean
    HintFlow hintFlow(
            HintGenerationPort generationPort,
            ArtifactStore artifactStore
    ) {
        return new HintFlow(generationPort, artifactStore,
                PracticeApplyFixture.practiceContext().conceptSourcePack());
    }

    @Bean
    PracticeSubmissionFlow practiceSubmissionFlow(
            ApplyProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            Clock clock
    ) {
        return new PracticeSubmissionFlow(
                executor, artifactStore, flowStore, assessmentPort, verificationPort,
                PracticeApplyFixture.practiceContext(), IndependentApplyFixture.independentContext(), clock);
    }

    @Bean
    TeachBackProfileExecutor teachBackProfileExecutor(
            @Qualifier("teachBackBundleStack") BundleStack stack,
            TeachBackGenerationPort generationPort,
            TeachBackTaskVerifierPort verifierPort,
            ArtifactStore artifactStore
    ) {
        return new TeachBackProfileExecutor(stack, generationPort, verifierPort, artifactStore);
    }

    @Bean
    TeachBackFlow teachBackFlow(
            TeachBackProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            TeachBackAssessmentPort assessmentPort,
            Clock clock
    ) {
        return new TeachBackFlow(
                executor, artifactStore, flowStore, assessmentPort,
                TeachBackApplyFixture.teachBackContext(), clock);
    }

    @Bean
    LearningStateGraph learningStateGraph(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            ReviewTaskStore reviewStore,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            PracticeSubmissionFlow practiceFlow,
            ReviewSubmissionFlow reviewFlow,
            ExplainFlow explainFlow,
            HintFlow hintFlow,
            TeachBackFlow teachBackFlow,
            PedagogyPort pedagogyPort,
            ClarificationClassifierPort clarificationClassifier,
            Clock clock
    ) {
        return new LearningStateGraph(
                artifactStore, flowStore, reviewStore, diagnosticFlow, independentFlow, practiceFlow,
                reviewFlow, explainFlow, hintFlow, teachBackFlow, pedagogyPort, clarificationClassifier, clock);
    }

    @Bean
    LearningFlowCommandUseCase learningFlowCommandUseCase(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            LearningStateGraph graph,
            OperatorModelProfilePort modelProfilePort,
            Clock clock
    ) {
        return new LearningFlowCommandUseCase(
                flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), modelProfilePort);
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
    ReviewCancellationUseCase reviewCancellationUseCase(
            ReviewTaskStore reviewStore, LearningFlowStore flowStore, Clock clock) {
        return new ReviewCancellationUseCase(reviewStore, flowStore, clock);
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
            ApplyProfileExecutor executor,
            ReviewTaskStore reviewStore,
            Clock clock
    ) {
        return new ReviewSubmissionFlow(
                artifactStore, flowStore, assessmentPort, verificationPort, reviewScheduler,
                executor, reviewStore, ReviewApplyFixture.reviewContext(), clock);
    }
}
