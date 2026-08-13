package cn.lunalhx.ai.kilnai.application.graph;

import java.util.UUID;

public record StartGraphRun(UUID learnerId, String fixtureId, UUID idempotencyKey, UUID flowId) {
}
