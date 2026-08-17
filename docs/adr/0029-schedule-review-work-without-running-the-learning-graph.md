---
status: accepted
---

# Schedule Review work without running the Learning StateGraph

After an Independent Test passes, Kiln-AI will create one durable Review Task due in one day. A conventional scheduler only changes Scheduled work to Due and surfaces it to the learner; it does not run a model, resume the graph, or pre-generate a Task Package. When the learner starts the Due item, Kiln-AI marks it Started, resumes the original Learning Flow in Delayed Review, and generates and verifies a Fresh Equivalent Task just in time. Successful completions schedule the next Review Task 3, 7, and then 21 days after the actual completion time; passing the fourth review establishes Durable and ends that cadence. A missed task remains Due without spawning duplicate or stacked future reviews, and a Concept has at most one unfinished Review Task. A verified Review failure applies the existing milestone downgrade rule, cancels later scheduled work, enters remediation through the Pedagogy Agent, and restarts at a one-day Review only after a fresh Independent Test passes. An unfinished Review Task retains the Active Learning Work claim until its normal outcome or explicit audited cancellation under ADR-0070; it cannot be silently bypassed by starting another Flow.
