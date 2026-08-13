package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.service.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.service.StartGraphRun;

public interface LearningGraphRuntimePort {

    LearnerVisibleInteraction start(StartGraphRun command);

    LearnerVisibleInteraction resume(ResumeGraphRun command);

    LearnerVisibleInteraction query(java.util.UUID flowId);

    PublicTraceView trace(java.util.UUID flowId);
}
