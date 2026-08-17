package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackAssessmentPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ScriptedTeachBackAssessmentModel implements TeachBackAssessmentPort {

    private final List<Optional<TeachBackAssessment>> replies;
    private final List<TeachBackAssessmentContext> contexts = new ArrayList<>();

    public ScriptedTeachBackAssessmentModel(List<TeachBackAssessment> judgments) {
        this.replies = judgments.stream().map(Optional::of).toList();
    }

    @SafeVarargs
    public static ScriptedTeachBackAssessmentModel replies(Optional<TeachBackAssessment>... replies) {
        return new ScriptedTeachBackAssessmentModel(List.of(replies), true);
    }

    private ScriptedTeachBackAssessmentModel(List<Optional<TeachBackAssessment>> replies, boolean ignored) {
        this.replies = List.copyOf(replies);
    }

    @Override
    public TeachBackAssessment assess(ModelProfile profile, TeachBackAssessmentContext context) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(context, "context must not be null");
        contexts.add(context);
        if (contexts.size() > replies.size()) {
            throw new IllegalStateException("scripted teach-back assessment model exhausted: no more scripted judgments");
        }
        return replies.get(contexts.size() - 1)
                .orElseThrow(() -> new ModelContractInvalidException(List.of("unknown_field")));
    }

    public List<TeachBackAssessmentContext> contexts() {
        return List.copyOf(contexts);
    }
}
