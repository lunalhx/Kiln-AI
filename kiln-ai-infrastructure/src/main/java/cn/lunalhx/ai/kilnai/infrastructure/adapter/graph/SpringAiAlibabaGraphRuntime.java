package cn.lunalhx.ai.kilnai.infrastructure.adapter.graph;

import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningGraphRuntimePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.fixture.SpikeFixture;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.GraphRunBudget;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.GraphRunBudgetHolder;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.service.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.service.StartGraphRun;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

public final class SpringAiAlibabaGraphRuntime implements LearningGraphRuntimePort {

    private final SpikeStorePort store;
    private final PendingLearnerEventHolder pendingEvents;
    private final LearningBlackboardMapper mapper;
    private final LearningStateGraphFactory graphFactory;
    private final GraphRunBudgetHolder budgets;
    private final int nodeLimit;
    private final IntSupplier toolLimit;

    public SpringAiAlibabaGraphRuntime(
            SpikeStorePort store,
            PendingLearnerEventHolder pendingEvents,
            LearningBlackboardMapper mapper,
            LearningStateGraphFactory factory,
            GraphRunBudgetHolder budgets,
            int toolLimit
    ) {
        this(store, pendingEvents, mapper, factory, budgets, GraphRunBudget.ORDINARY_NODE_LIMIT, toolLimit);
    }

    public SpringAiAlibabaGraphRuntime(
            SpikeStorePort store,
            PendingLearnerEventHolder pendingEvents,
            LearningBlackboardMapper mapper,
            LearningStateGraphFactory factory,
            GraphRunBudgetHolder budgets,
            IntSupplier toolLimit
    ) {
        this(store, pendingEvents, mapper, factory, budgets, GraphRunBudget.ORDINARY_NODE_LIMIT, toolLimit);
    }

    public SpringAiAlibabaGraphRuntime(
            SpikeStorePort store,
            PendingLearnerEventHolder pendingEvents,
            LearningBlackboardMapper mapper,
            LearningStateGraphFactory factory,
            GraphRunBudgetHolder budgets,
            int nodeLimit,
            int toolLimit
    ) {
        this(store, pendingEvents, mapper, factory, budgets, nodeLimit, () -> toolLimit);
    }

    public SpringAiAlibabaGraphRuntime(
            SpikeStorePort store,
            PendingLearnerEventHolder pendingEvents,
            LearningBlackboardMapper mapper,
            LearningStateGraphFactory factory,
            GraphRunBudgetHolder budgets,
            int nodeLimit,
            IntSupplier toolLimit
    ) {
        this.store = store;
        this.pendingEvents = pendingEvents;
        this.mapper = mapper;
        this.graphFactory = factory;
        this.budgets = budgets;
        this.nodeLimit = nodeLimit;
        this.toolLimit = toolLimit;
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
        budgets.open(flowId, new GraphRunBudget(nodeLimit, toolLimit.getAsInt()));
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
        } finally {
            budgets.close(flowId);
        }
    }
}
