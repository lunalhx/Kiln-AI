package cn.lunalhx.ai.kilnai.domain.gate;

@FunctionalInterface
public interface GatePolicy<T> {

    GateResult<T> evaluate(T candidate, GateContext context);
}
