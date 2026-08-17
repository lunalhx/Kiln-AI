package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ScriptedResponseVerificationModel implements ResponseVerificationPort {

    private final List<Optional<ResponseAssessment>> replies;
    private final List<ResponseAssessmentContext> contexts = new ArrayList<>();

    public ScriptedResponseVerificationModel(List<ResponseAssessment> judgments) {
        this.replies = judgments.stream().map(Optional::of).toList();
    }

    @SafeVarargs
    public static ScriptedResponseVerificationModel replies(Optional<ResponseAssessment>... replies) {
        return new ScriptedResponseVerificationModel(List.of(replies), true);
    }

    private ScriptedResponseVerificationModel(List<Optional<ResponseAssessment>> replies, boolean ignored) {
        this.replies = List.copyOf(replies);
    }

    @Override
    public ResponseAssessment verify(ModelProfile profile, ResponseAssessmentContext context) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(context, "context must not be null");
        contexts.add(context);
        if (contexts.size() > replies.size()) {
            throw new IllegalStateException("scripted response verification model exhausted: no more scripted judgments");
        }
        return replies.get(contexts.size() - 1)
                .orElseThrow(() -> new ModelContractInvalidException(List.of("unknown_field")));
    }

    public List<ResponseAssessmentContext> contexts() {
        return List.copyOf(contexts);
    }
}
