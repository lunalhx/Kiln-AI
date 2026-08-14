package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Objects;

public record PrivateAssessorFacts(
        ProposedExpectedAnswer proposedExpectedAnswer,
        List<RubricMapping> rubricMapping,
        List<DraftSourceTraceEntry> sourceTrace,
        EquivalenceDeclaration equivalenceDeclaration
) {

    public PrivateAssessorFacts {
        Objects.requireNonNull(proposedExpectedAnswer, "proposedExpectedAnswer must not be null");
        Objects.requireNonNull(rubricMapping, "rubricMapping must not be null");
        Objects.requireNonNull(sourceTrace, "sourceTrace must not be null");
        Objects.requireNonNull(equivalenceDeclaration, "equivalenceDeclaration must not be null");
        rubricMapping = List.copyOf(rubricMapping);
        sourceTrace = List.copyOf(sourceTrace);
    }

    public record ProposedExpectedAnswer(String expression) {
        public ProposedExpectedAnswer {
            Objects.requireNonNull(expression, "expression must not be null");
        }
    }

    public record RubricMapping(String masteryCriterionId, List<String> evidenceChannels) {
        public RubricMapping {
            Objects.requireNonNull(masteryCriterionId, "masteryCriterionId must not be null");
            Objects.requireNonNull(evidenceChannels, "evidenceChannels must not be null");
            evidenceChannels = List.copyOf(evidenceChannels);
        }
    }

    public record DraftSourceTraceEntry(String sourceDocumentId, String passageId) {
        public DraftSourceTraceEntry {
            Objects.requireNonNull(sourceDocumentId, "sourceDocumentId must not be null");
            Objects.requireNonNull(passageId, "passageId must not be null");
        }
    }

    public record EquivalenceDeclaration(String kind, List<String> variables, String domain) {
        public EquivalenceDeclaration {
            Objects.requireNonNull(kind, "kind must not be null");
            Objects.requireNonNull(variables, "variables must not be null");
            Objects.requireNonNull(domain, "domain must not be null");
            variables = List.copyOf(variables);
        }
    }
}
