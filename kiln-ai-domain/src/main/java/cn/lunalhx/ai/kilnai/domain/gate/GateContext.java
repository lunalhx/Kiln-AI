package cn.lunalhx.ai.kilnai.domain.gate;

public record GateContext(java.util.Set<String> legalActions) {

    public static GateContext empty() {
        return new GateContext(java.util.Set.of());
    }
}
