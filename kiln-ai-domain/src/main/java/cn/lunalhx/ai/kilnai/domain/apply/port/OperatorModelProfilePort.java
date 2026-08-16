package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

/**
 * The operator-owned seam that resolves the current Model Profile (Strong and
 * Small bindings plus the output-token ceiling) at Learning Flow start. The
 * resolved snapshot is frozen onto the Flow and never re-resolved for later
 * model calls. A missing or invalid operator configuration fails closed with
 * an application error; scripted test doubles are never registered as
 * providers and can never become a production fallback.
 */
public interface OperatorModelProfilePort {

    ModelProfile resolve();
}
