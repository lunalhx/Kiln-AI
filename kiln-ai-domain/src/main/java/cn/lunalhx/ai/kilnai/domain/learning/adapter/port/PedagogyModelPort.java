package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import cn.lunalhx.ai.kilnai.domain.artifact.PedagogyPlan;
import cn.lunalhx.ai.kilnai.domain.learning.model.PedagogyContextView;

public interface PedagogyModelPort {

    PedagogyPlan propose(PedagogyContextView context, String compiledPrompt);
}
