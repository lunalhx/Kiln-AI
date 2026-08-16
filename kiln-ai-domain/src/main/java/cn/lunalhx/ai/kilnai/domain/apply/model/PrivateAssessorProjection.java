package cn.lunalhx.ai.kilnai.domain.apply.model;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;

import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts.EquivalenceDeclaration;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts.RubricMapping;

import java.util.List;
import java.util.Objects;

public record PrivateAssessorProjection(
        CanonicalExpectedAnswer canonicalExpectedAnswer,
        List<RubricMapping> rubricMapping,
        List<SourceTraceEntry> sourceTrace,
        EquivalenceDeclaration equivalenceDeclaration,
        TaskFingerprint taskFingerprint,
        SolutionFingerprint solutionFingerprint,
        ExecutionTrace executionTrace
) {

    public PrivateAssessorProjection {
        Objects.requireNonNull(canonicalExpectedAnswer, "canonicalExpectedAnswer must not be null");
        Objects.requireNonNull(rubricMapping, "rubricMapping must not be null");
        Objects.requireNonNull(sourceTrace, "sourceTrace must not be null");
        Objects.requireNonNull(equivalenceDeclaration, "equivalenceDeclaration must not be null");
        Objects.requireNonNull(taskFingerprint, "taskFingerprint must not be null");
        Objects.requireNonNull(solutionFingerprint, "solutionFingerprint must not be null");
        Objects.requireNonNull(executionTrace, "executionTrace must not be null");
        rubricMapping = List.copyOf(rubricMapping);
        sourceTrace = List.copyOf(sourceTrace);
    }

    public record CanonicalExpectedAnswer(String expression, List<String> variables, String domain) {
        public CanonicalExpectedAnswer {
            Objects.requireNonNull(expression, "expression must not be null");
            Objects.requireNonNull(variables, "variables must not be null");
            Objects.requireNonNull(domain, "domain must not be null");
            variables = List.copyOf(variables);
        }
    }

    public record SourceTraceEntry(String sourceDocumentId, String sourceVersion, String passageId) {
        public SourceTraceEntry {
            Objects.requireNonNull(sourceDocumentId, "sourceDocumentId must not be null");
            Objects.requireNonNull(sourceVersion, "sourceVersion must not be null");
            Objects.requireNonNull(passageId, "passageId must not be null");
        }
    }

    public record TaskFingerprint(String derivedBy, String value) {
        public TaskFingerprint {
            Objects.requireNonNull(derivedBy, "derivedBy must not be null");
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    public record SolutionFingerprint(String derivedBy, String value) {
        public SolutionFingerprint {
            Objects.requireNonNull(derivedBy, "derivedBy must not be null");
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    public record ExecutionTrace(
            String profile,
            String taskBlueprint,
            List<String> skillStack,
            ModelExecution model
    ) {
        public ExecutionTrace {
            Objects.requireNonNull(profile, "profile must not be null");
            Objects.requireNonNull(taskBlueprint, "taskBlueprint must not be null");
            Objects.requireNonNull(skillStack, "skillStack must not be null");
            skillStack = List.copyOf(skillStack);
            Objects.requireNonNull(model, "model must not be null");
        }
    }
}
