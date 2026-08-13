package cn.lunalhx.ai.kilnai.domain.blackboard;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlackboardReducerTest {

    @Test
    void teachingReducerMayOnlyWriteItsChannel() {
        TeachingResultReducer reducer = new TeachingResultReducer();
        LearningBlackboard board = LearningBlackboard.initial(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );

        BlackboardDelta delta = reducer.authorized(reducer.reduce(board, "visible explanation"));

        assertEquals("teaching", delta.channel());
        assertEquals("visible explanation", delta.fields().get("visibleContent"));
    }

    @Test
    void unauthorizedFieldIsRejected() {
        TeachingResultReducer reducer = new TeachingResultReducer();
        BlackboardDelta illegal = new BlackboardDelta("teaching", Map.of("modelCallCount", 99));

        assertThrows(IllegalStateException.class, () -> reducer.authorized(illegal));
    }

    static final class TeachingResultReducer implements BlackboardReducer<String> {
        @Override
        public String channel() {
            return "teaching";
        }

        @Override
        public Set<String> authorizedFields() {
            return Set.of("visibleContent", "status", "allowedEventKinds", "explanationDelivered", "acceptedAction");
        }

        @Override
        public BlackboardDelta reduce(LearningBlackboard current, String accepted) {
            return new BlackboardDelta(channel(), Map.of(
                    "visibleContent", accepted,
                    "status", FlowStatus.AWAITING_LEARNER_INPUT,
                    "allowedEventKinds", List.of(LearnerInputKind.CONTINUE_REQUESTED),
                    "explanationDelivered", true,
                    "acceptedAction", TeachingAction.EXPLAIN
            ));
        }
    }
}
