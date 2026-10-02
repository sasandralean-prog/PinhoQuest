# P3-5A / P3-5B Contract Implementation

Date: 2026-10-02
Status: **P3-5A implemented; P3-5B core contract implemented; Android runtime integration gated**

## P3-5A

The canonical prompt boundary is:

`QuestGenerationPlan + sanitized inputs -> PromptFactsAssembler -> BoundedPromptEnvelope -> CompactPromptSerializer`

Implemented guarantees:
- only enabled tag labels cross the boundary;
- tag affinity, source, IDs and Room entities are discarded;
- tags are deduplicated, normalized, sorted and bounded;
- research crosses only as typed `PromptResearchHint` allowlists;
- research text is normalized and bounded;
- no raw HTML, search-result body, source URI, profile history or provenance payload is representable by the prompt contract;
- schema version and task are explicit;
- output fields are a fixed allowlist;
- prompt size has a deterministic character budget.

Default prompt limits are intentionally conservative and model-independent at the core boundary.
The character limit is a safety budget, not an assertion about tokenizer token count.

## P3-5B core

Implemented:
- `GenerationRequest` with explicit `maxOutputTokens`;
- `LocalInferencePort` runtime abstraction;
- semantic `InferenceOutcome` results;
- strict `QuestDraftCodec`;
- runtime wrapper extraction for valid JSON surrounded by ChatML/Markdown;
- exact output-field allowlist;
- strict JSON string/array typing;
- requested time and difficulty constraints are checked against the trusted plan;
- category and environment come only from the trusted `QuestGenerationPlan`.

Invalid model output cannot become a `Quest` through this codec.
Promotion still belongs exclusively to `QuestValidator`.
## Validation

`PromptFactsAssemblerTest`: 5/5 passed.

`QuestDraftCodecTest`: 6/6 passed after adding wrong-type rejection.

Full `:quest-core:test`: passed.

## Explicit gate

The Android runtime adapter and `LocalLlmComposer` are intentionally deferred until the P3-4 runtime/lifecycle benchmark gate passes.

Reason:
the Qwen/LiteRT-LM reproduction showed a native lifecycle failure after repeated generations on one Engine, so wiring that runtime before lifecycle characterization would couple production to an unvalidated execution policy.

Next integration boundary:

`BoundedPromptEnvelope -> CompactPromptSerializer -> LocalInferencePort -> selected Android runtime -> QuestDraftCodec -> QuestValidator`
