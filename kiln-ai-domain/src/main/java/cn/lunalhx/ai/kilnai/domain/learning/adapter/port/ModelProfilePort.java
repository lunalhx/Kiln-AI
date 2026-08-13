package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import cn.lunalhx.ai.kilnai.domain.learning.model.FrozenModelProfile;

public interface ModelProfilePort {

    FrozenModelProfile resolveCurrentDefaults();
}
