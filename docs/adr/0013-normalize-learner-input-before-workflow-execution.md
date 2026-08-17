---
status: accepted
---

# Normalize learner input before workflow execution

Every resumed Graph Run will pass new learner input through a Learner Input Gate before teaching, assessment, or state-transition logic executes. Structured UI and API actions produce typed Learner Input Events deterministically; only ambiguous free-form text invokes a stateless Learner Input Interpreter, whose Phase 0 output is limited to Answer Submitted, Continue Requested, Hint Requested, Clarification Asked, Assistance Decided, Retry Requested, Flow Control Requested, or Unknown Input plus interpretation metadata. The Workflow Guard then validates the event against current Learning State—for example, Answer Submitted is invalid without an open Task Attempt and Retry Requested is invalid without an Unavailable Interaction—and an illegal or uncertain event requests clarification without advancing state. Retry Requested carries no learner answer and resumes only a durable Pending Operation. The gate and interpreter cannot select a Teaching Node Profile, resolve Skills, assess performance, or mutate Learning State. This keeps natural-language interpretation replaceable and prevents an inferred intent from becoming an unchecked domain command.
