package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * The closed vocabulary of legal next moves the Workflow Guard derives from
 * committed Learning State and the Pedagogy Plan may select. Teaching Actions
 * are tools, not mandatory stages (CONTEXT.md); {@link #RESUME_PRACTICE} is
 * the deterministic single move back into an open Apply Practice Attempt
 * after a temporary Explain and is never selectable by a plan because the
 * Guard bypasses the model whenever it is the only legal move.
 */
public enum TeachingAction {

    EXPLAIN("explain"),
    APPLY_PRACTICE("apply_practice"),
    TEACH_BACK("teach_back"),
    INDEPENDENT_TEST("independent_test"),
    RESUME_PRACTICE("resume_practice");

    private final String jsonName;

    TeachingAction(String jsonName) {
        this.jsonName = jsonName;
    }

    /**
     * The closed snake_case JSON name of the action in the serialized
     * {@code pedagogy_execution_context/v1} legal set and the
     * {@code pedagogy_plan/v1} draft.
     */
    @JsonValue
    public String jsonName() {
        return jsonName;
    }

    public static TeachingAction fromJson(String jsonName) {
        return Arrays.stream(values())
                .filter(action -> action.jsonName.equals(jsonName))
                .findFirst()
                .orElseThrow(() -> new ApplyDraftException("unknown teaching action: " + jsonName));
    }
}
