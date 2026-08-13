package cn.lunalhx.ai.kilnai.infrastructure.adapter.graph;

import cn.lunalhx.ai.kilnai.domain.learning.service.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.AuthorizedNodeResult;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.LearningNodeKernel;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.JacksonStateSerializer;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public final class LearningStateGraphFactory {

    private final LearningNodeKernel kernel;
    private final PendingLearnerEventHolder pendingEvents;
    private final LearningBlackboardMapper mapper;
    private final ApplicationCheckpointSaver saver;

    public LearningStateGraphFactory(
            LearningNodeKernel kernel,
            PendingLearnerEventHolder pendingEvents,
            LearningBlackboardMapper mapper,
            ApplicationCheckpointSaver saver
    ) {
        this.kernel = kernel;
        this.pendingEvents = pendingEvents;
        this.mapper = mapper;
        this.saver = saver;
    }

    public CompiledGraph compile() {
        try {
            StateGraph graph = new StateGraph("learning-spike", () -> {
                Map<String, KeyStrategy> strategies = new HashMap<>();
                strategies.put(LearningBlackboardMapper.BLACKBOARD_KEY, KeyStrategy.REPLACE);
                strategies.put(LearningBlackboardMapper.NEXT_ROUTE_KEY, KeyStrategy.REPLACE);
                return strategies;
            }, new JacksonStateSerializer(OverAllState::new) {
            });
            graph.addNode("ingest", node_async(this::ingest));
            graph.addNode("pedagogy", node_async(this::pedagogy));
            graph.addNode("explain", node_async(this::explain));
            graph.addNode("apply", node_async(this::apply));
            graph.addNode("assess", node_async(this::assess));
            graph.addEdge(START, "ingest");
            Map<String, String> routes = Map.of(
                    "explain", "explain",
                    "pedagogy", "pedagogy",
                    "apply", "apply",
                    "assess", "assess",
                    "end", END
            );
            graph.addConditionalEdges("ingest", edge_async(this::route), routes);
            graph.addConditionalEdges("pedagogy", edge_async(this::route), routes);
            graph.addEdge("explain", "ingest");
            graph.addEdge("apply", "ingest");
            graph.addEdge("assess", END);
            CompileConfig config = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder().register(saver).build())
                    .interruptAfter("explain", "apply")
                    .build();
            return graph.compile(config);
        } catch (GraphStateException exception) {
            throw new IllegalStateException("failed to compile learning graph", exception);
        }
    }

    private Map<String, Object> ingest(OverAllState state) {
        LearningBlackboard board = mapper.fromState(state);
        ResumeGraphRun event = pendingEvents.peek(board.flowId());
        AuthorizedNodeResult result = kernel.ingest(
                board, event == null ? null : event.kind(), event == null ? null : event.text()
        );
        if (event != null && event.kind() != cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind.ANSWER_SUBMITTED) {
            pendingEvents.poll(board.flowId());
        }
        return finish(result);
    }

    private Map<String, Object> pedagogy(OverAllState state) {
        return finish(kernel.pedagogy(mapper.fromState(state)));
    }

    private Map<String, Object> explain(OverAllState state) {
        return guarded(mapper.fromState(state), kernel::explain);
    }

    private Map<String, Object> apply(OverAllState state) {
        return guarded(mapper.fromState(state), kernel::apply);
    }

    private Map<String, Object> assess(OverAllState state) {
        LearningBlackboard board = mapper.fromState(state);
        ResumeGraphRun event = pendingEvents.poll(board.flowId());
        return finish(kernel.assess(board, event == null ? "" : event.text()));
    }

    private String route(OverAllState state) {
        Object route = state.data().get(LearningBlackboardMapper.NEXT_ROUTE_KEY);
        return route == null ? "end" : route.toString();
    }

    private Map<String, Object> guarded(LearningBlackboard board, java.util.function.Function<LearningBlackboard, AuthorizedNodeResult> action) {
        try {
            return finish(action.apply(board));
        } catch (RuntimeException exception) {
            kernel.buffer().discard(board.flowId());
            throw exception;
        }
    }

    private Map<String, Object> finish(AuthorizedNodeResult result) {
        return mapper.toFramework(result.blackboard(), result.nextRoute());
    }
}
