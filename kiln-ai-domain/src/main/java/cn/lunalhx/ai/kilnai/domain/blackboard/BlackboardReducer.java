package cn.lunalhx.ai.kilnai.domain.blackboard;

import java.util.Objects;
import java.util.Set;

public interface BlackboardReducer<T> {

    String channel();

    Set<String> authorizedFields();

    BlackboardDelta reduce(LearningBlackboard current, T accepted);

    default BlackboardDelta authorized(BlackboardDelta delta) {
        Objects.requireNonNull(delta, "delta must not be null");
        if (!channel().equals(delta.channel())) {
            throw new IllegalStateException("reducer cannot write channel " + delta.channel());
        }
        for (String field : delta.fields().keySet()) {
            if (!authorizedFields().contains(field)) {
                throw new IllegalStateException("reducer cannot write field " + field);
            }
        }
        return delta;
    }
}
