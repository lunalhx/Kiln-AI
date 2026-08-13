package cn.lunalhx.ai.kilnai.application.graph;

public interface LearningGraphRuntimePort {

    LearnerVisibleInteraction start(StartGraphRun command);

    LearnerVisibleInteraction resume(ResumeGraphRun command);

    LearnerVisibleInteraction query(java.util.UUID flowId);

    PublicTraceView trace(java.util.UUID flowId);
}
