package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ScriptedAssessmentModel implements AssessmentPort {

    private final List<Optional<ResponseAssessment>> replies;
    private final List<ResponseAssessmentContext> contexts = new ArrayList<>();

    public ScriptedAssessmentModel(List<ResponseAssessment> judgments) {
        this.replies = judgments.stream().map(Optional::of).toList();
    }

    @SafeVarargs
    public static ScriptedAssessmentModel replies(Optional<ResponseAssessment>... replies) {
        return new ScriptedAssessmentModel(List.of(replies), true);
    }

    private ScriptedAssessmentModel(List<Optional<ResponseAssessment>> replies, boolean ignored) {
        this.replies = List.copyOf(replies);
    }

    @Override
    public ResponseAssessment assess(ModelProfile profile, ResponseAssessmentContext context) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(context, "context must not be null");
        contexts.add(context);
        if (contexts.size() > replies.size()) {
            throw new IllegalStateException("scripted assessment model exhausted: no more scripted judgments");
        }
        return replies.get(contexts.size() - 1)
                .orElseThrow(() -> new ModelContractInvalidException(List.of("unknown_field")));
    }

    public List<ResponseAssessmentContext> contexts() {
        return List.copyOf(contexts);
    }
}
