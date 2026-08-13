package cn.lunalhx.ai.kilnai.domain.learning.service;

import java.util.UUID;

public record StartGraphRun(UUID learnerId, String fixtureId, UUID idempotencyKey, UUID flowId) {
}
