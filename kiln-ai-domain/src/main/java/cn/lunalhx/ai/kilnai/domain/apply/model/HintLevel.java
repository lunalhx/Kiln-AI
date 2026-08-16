package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The cross-subject Hint Level of the reference ladder (CONTEXT.md): H1
 * Orient, H2 Cue, H3 Strategy, H4 Scaffold, H5 Reveal. H1-H5 are assistance;
 * H5 exposes the complete solution and closes the attempt as Solution
 * Revealed.
 */
public enum HintLevel {
    H1("orient"),
    H2("cue"),
    H3("strategy"),
    H4("scaffold"),
    H5("reveal");

    private final String disclosureKind;

    HintLevel(String disclosureKind) {
        this.disclosureKind = disclosureKind;
    }

    public String disclosureKind() {
        return disclosureKind;
    }

    public int level() {
        return ordinal() + 1;
    }

    public static HintLevel of(int level) {
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("hint level must be between 1 and 5 but was " + level);
        }
        return values()[level - 1];
    }

    public static HintLevel fromDisclosureKind(String kind) {
        for (HintLevel level : values()) {
            if (level.disclosureKind.equals(kind)) {
                return level;
            }
        }
        throw new IllegalArgumentException("unknown disclosure kind: " + kind);
    }
}
