package cn.lunalhx.ai.kilnai.application.fake;

import cn.lunalhx.ai.kilnai.application.port.ToolSession;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CalculatorToolSession implements ToolSession {

    private final Set<String> available;
    private final CopyOnWriteArrayList<String> calls = new CopyOnWriteArrayList<>();

    public CalculatorToolSession(boolean calculatorAvailable) {
        this.available = calculatorAvailable ? Set.of("calculator@1") : Set.of();
    }

    public java.util.List<String> calls() {
        return List.copyOf(calls);
    }

    @Override
    public Map<String, Object> call(String toolId, Map<String, Object> input) {
        calls.add(toolId);
        if (!available.contains(toolId)) {
            throw new CapabilityGap("required tool unavailable: " + toolId);
        }
        BigDecimal oldValue = new BigDecimal(String.valueOf(input.get("old")));
        BigDecimal newValue = new BigDecimal(String.valueOf(input.get("new")));
        BigDecimal result = newValue.subtract(oldValue)
                .divide(oldValue, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .stripTrailingZeros();
        return Map.of("result", result.toPlainString());
    }
}
