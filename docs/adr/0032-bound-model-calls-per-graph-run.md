---
status: accepted
---

# Bound model calls per Graph Run

Every Graph Run will receive a traced Graph Run Budget covering planned and repair model-call counts, input and output Tokens, estimated cost, and elapsed time. An Ordinary Run for explanation, practice, or ordinary continuation may plan at most three model calls and make at most four total calls including one shared repair allowance. A High-Consequence Run for Independent Test, Delayed Review, or milestone-changing evidence may plan at most four calls and make at most six total calls, allowing one repair cycle and a required re-verification without opening repeated voting. Each model-producing node remains bounded to one initial call and the run's permitted repair; Agents cannot invoke one another recursively or expand their own budget. Input Interpreter, Pedagogy Agent, and format repair should use a smaller suitable model, while Teaching, Assessment, and Task Verification may use stronger models under isolated contexts. Exact Token, cost, and latency ceilings remain configuration calibrated by measurement, but overflow never silently enlarges context: the graph checkpoints and uses the artifact-specific fallback, Capability Gap, inconclusive outcome, or safe retry response without accepting unsupported evidence.
