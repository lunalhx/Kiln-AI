package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed outcome of one atomic hint exposure on a Task Attempt. The
 * store persists the ladder (when absent), appends the exposed level to the
 * Assistance Trace, records the request, and — only for H5 — closes the
 * attempt as Solution Revealed, all in one commit; a crash between the
 * exposure and the boundary commit therefore resumes the same exposed level.
 */
public sealed interface HintExposureOutcome permits
        HintExposureOutcome.Exposed,
        HintExposureOutcome.AlreadyExposed,
        HintExposureOutcome.NotOpen,
        HintExposureOutcome.NotFound {

    record Exposed(TaskAttempt attempt, HintRequestRecord request) implements HintExposureOutcome {
    }

    record AlreadyExposed(TaskAttempt attempt, HintRequestRecord request) implements HintExposureOutcome {
    }

    record NotOpen(TaskAttempt attempt) implements HintExposureOutcome {
    }

    record NotFound() implements HintExposureOutcome {
    }
}
