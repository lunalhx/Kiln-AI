package cn.lunalhx.ai.kilnai.graph.saa;

import cn.lunalhx.ai.kilnai.application.fixture.SpikeFixture;
import cn.lunalhx.ai.kilnai.application.graph.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.application.graph.LearningGraphRuntimePort;
import cn.lunalhx.ai.kilnai.application.graph.PublicTraceView;
import cn.lunalhx.ai.kilnai.application.graph.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.application.graph.StartGraphRun;
import cn.lunalhx.ai.kilnai.application.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.application.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;
import java.util.UUID;

public final class SpringAiAlibabaGraphRuntime implements LearningGraphRuntimePort {

    private final SpikeStorePort store;
    private final PendingLearnerEventHolder pendingEvents;
    private final LearningBlackboardMapper mapper;
    private final LearningStateGraphFactory graphFactory;

    public SpringAiAlibabaGraphRuntime(
            SpikeStorePort store,
            PendingLearnerEventHolder pendingEvents,
            LearningBlackboardMapper mapper,
            LearningStateGraphFactory graphFactory
    ) {
        this.store = store;
        this.pendingEvents = pendingEvents;
        this.mapper = mapper;
        this.graphFactory = graphFactory;
    }

    @Override
    public LearnerVisibleInteraction start(StartGraphRun command) {
        LearningBlackboard initial = LearningBlackboard.initial(
                command.flowId(), command.learnerId(), SpikeFixture.CONCEPT_ID,
                SpikeFixture.CONTRACT_ID, SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID
        );
        invoke(graphFactory.compile(), mapper.toFramework(initial, "ingest"), command.flowId(), false);
        return store.latestInteraction(command.flowId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "graph run produced no interaction"));
    }

    @Override
    public LearnerVisibleInteraction resume(ResumeGraphRun command) {
        pendingEvents.offer(command);
        invoke(graphFactory.compile(), Map.of(), command.flowId(), true);
        return store.latestInteraction(command.flowId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "graph run produced no interaction"));
    }

    @Override
    public LearnerVisibleInteraction query(UUID flowId) {
        return store.latestInteraction(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
    }

    @Override
    public PublicTraceView trace(UUID flowId) {
        return store.publicTrace(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "trace not found"));
    }

    private void invoke(CompiledGraph graph, Map<String, Object> inputs, UUID flowId, boolean resume) {
        try {
            RunnableConfig.Builder builder = RunnableConfig.builder().threadId(flowId.toString());
            if (resume) {
                builder.resume();
            }
            graph.invoke(inputs, builder.build());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            ApplicationException wrapped = new ApplicationException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    exception.getClass().getName() + ": " + exception.getMessage()
            );
            wrapped.initCause(exception);
            throw wrapped;
        }
    }
}
