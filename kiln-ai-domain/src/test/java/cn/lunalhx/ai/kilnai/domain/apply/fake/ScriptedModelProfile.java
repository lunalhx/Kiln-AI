package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

/**
 * The shared scripted Model Profile of the contract-test harness: a fixed
 * Strong and Small binding plus the operator-owned output-token ceiling. The
 * scripted fakes ignore the profile contents but record the identity they
 * receive, so tests can assert that every model call carries the Flow-frozen
 * profile and that Strong/Small responsibilities reach the right slots.
 */
public final class ScriptedModelProfile {

    public static final ModelProfile PROFILE = new ModelProfile(
            new ModelProfile.ModelBinding(
                    "openai-compatible",
                    "https://api.test/v1",
                    "acme",
                    "scripted-strong",
                    "TEST_STRONG_SECRET"),
            new ModelProfile.ModelBinding(
                    "openai-compatible",
                    "https://api.test/v1",
                    "acme",
                    "scripted-small",
                    "TEST_SMALL_SECRET"),
            2048);

    private ScriptedModelProfile() {
    }
}
