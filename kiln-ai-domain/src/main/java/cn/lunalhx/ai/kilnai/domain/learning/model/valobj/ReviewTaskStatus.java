package cn.lunalhx.ai.kilnai.domain.learning.model.valobj;

/**
 * The durable lifecycle of one Review Task. SCHEDULED work becomes DUE when
 * its due time arrives; only DUE work is startable. STARTED work is bound to
 * an open Review Attempt. COMPLETED and CANCELLED tasks remain auditable but
 * are never actionable.
 */
public enum ReviewTaskStatus {
    SCHEDULED,
    DUE,
    STARTED,
    COMPLETED,
    CANCELLED
}
