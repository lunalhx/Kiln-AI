---
status: accepted
---

# Request JSON object on every model call

Every real model call asks the provider for a JSON object through the OpenAI-compatible
`response_format` field. The ChatClient factory attaches
`{ "type": "json_object" }` to every Strong and Small call. Generation (Apply,
Explain, Hint, Teach-back, Pedagogy) uses temperature `0.7` so later tasks can
differ from already-exposed fingerprints. Judgment (verification, assessment,
clarification classification) uses `0.2`. This is a transport constraint: the
adapter still returns raw content, and the Domain remains the strict closed-contract
parser.

The factory does not send a JSON Schema, does not branch by model or provider,
and does not fall back to unconstrained text when a provider rejects the
format. A provider that cannot emit a JSON object fails as a provider outage.
A response that is JSON but violates a closed contract remains a model-contract
error under ADR-0071.

Prompt-side response contracts stay in force and show bare JSON objects, never
markdown fences. `json_schema` is deferred until the catalog can declare that
capability without a per-model compatibility path.
