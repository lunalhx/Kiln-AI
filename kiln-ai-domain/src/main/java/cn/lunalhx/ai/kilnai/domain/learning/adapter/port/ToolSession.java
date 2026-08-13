package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import java.math.BigDecimal;
import java.util.Map;

public interface ToolSession {

    Map<String, Object> call(String toolId, Map<String, Object> input);
}
