package cn.lunalhx.ai.kilnai.api.response;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, Instant timestamp) {
}
