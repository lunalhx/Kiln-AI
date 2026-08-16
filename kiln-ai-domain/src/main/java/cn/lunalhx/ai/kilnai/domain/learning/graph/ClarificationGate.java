package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The Clarification Gate (CONTEXT.md): the interaction node — never a sixth
 * Teaching Node Profile — that classifies a Clarification Asked event as
 * Procedural or Substantive through the bounded classifier port. A procedural
 * request is answered directly by the gate itself with a deterministic
 * restatement of the interface behavior, response format, notation, or
 * conditions the Task Package already exposes, without loading a Teaching
 * Node Profile; substantive or uncertain requests are routed by the graph to
 * the assistance path — a temporary Explain on a Practice attempt, or an
 * explicit assistance warning and learner consent on an Independent Test or
 * Review attempt (ADR-0014).
 */
public final class ClarificationGate {

    private final ClarificationClassifierPort classifierPort;

    public ClarificationGate(ClarificationClassifierPort classifierPort) {
        this.classifierPort = Objects.requireNonNull(classifierPort, "classifierPort must not be null");
    }

    /**
     * Classifies one free-form clarification message against the
     * learner-visible task text. An uncertain classification is returned
     * unchanged: the graph must treat it like substantive assistance, never
     * guess a procedural answer.
     */
    public ClarificationClassification classify(String message, String taskText) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(taskText, "taskText must not be null");
        return classifierPort.classify(message, taskText);
    }

    /**
     * The deterministic procedural answer of one Task Package: it restates
     * only the answer fields, their requiredness and input families, and the
     * submission rule the package already exposed, so no knowledge is added
     * and the answer never narrows the solution (ADR-0014).
     */
    public String proceduralAnswer(LearnerProjection projection) {
        Objects.requireNonNull(projection, "projection must not be null");
        StringBuilder answer = new StringBuilder("答题说明：本题");
        answer.append(projection.answerFields().stream()
                .map(field -> field.label() + (field.required() ? "（必填）" : "（可选）")
                        + (field.acceptedInputFamilies() == null || field.acceptedInputFamilies().isEmpty()
                        ? ""
                        : "，接受输入方式：" + String.join("、", field.acceptedInputFamilies())))
                .collect(Collectors.joining("；", "包含作答字段 ", "。")));
        answer.append("正式提交仅限一次，请按题目中给出的变量与输入方式填写。");
        return answer.toString();
    }
}
