package cn.lunalhx.ai.kilnai.domain.learning.fake;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.learning.model.FrozenModelProfile;
import cn.lunalhx.ai.kilnai.domain.learning.model.ModelBindingSnapshot;

public final class SkippingModelProfilePort implements ModelProfilePort {

    public static final FrozenModelProfile SNAPSHOT = new FrozenModelProfile(
            new ModelBindingSnapshot("openai-compatible", "http://127.0.0.1", "test", "unused-strong", "KILN_TEST_API_KEY"),
            new ModelBindingSnapshot("openai-compatible", "http://127.0.0.1", "test", "unused-small", "KILN_TEST_API_KEY")
    );

    @Override
    public FrozenModelProfile resolveCurrentDefaults() {
        return SNAPSHOT;
    }
}
