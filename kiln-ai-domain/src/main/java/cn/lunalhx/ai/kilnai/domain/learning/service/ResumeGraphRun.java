package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;

import java.util.UUID;

public record ResumeGraphRun(
        UUID flowId,
        UUID idempotencyKey,
        int interactionVersion,
        LearnerInputKind kind,
        String text
) {
}
