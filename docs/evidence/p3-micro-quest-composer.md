# P3 Micro Quest Composer

Date: 2026-10-02
Branch: `feature/p3-micro-quest-composer`
Baseline: `647ffc7`

## Architecture
`QuestGenerationPlan` remains the source of truth for category, environment, difficulty and duration.
`MicroQuestComposer` delegates only humanized text to a local model.
`FunctionGemmaResponseSanitizer` accepts only a tightly bounded text payload.
`MicroQuestRenderer` reconstructs a trusted `QuestDraft` from model text plus the original plan.
`ProceduralComposer` remains the complete fallback.

## Contract
Model-owned fields: `title`, `description`, `objectives`.
Deterministic fields: `category`, `environment`, `difficulty`, `estimatedDuration`, XP/rewards and future governed metadata.
Examples are supplied dynamically from approved procedural quests; this is few-shot guidance, not fine-tuning.

## Runtime compatibility gate
LiteRT-LM 0.17.1 requires a Java 21-compatible build and reports Kotlin metadata 2.4.0.
The PinhoQuest production project uses Kotlin 2.1.21 and Java 17.
Directly adding the LiteRT-LM AAR to production caused Kotlin metadata/compiler failure, so the dependency was removed again.
The isolated harness compiles with Android Studio JBR 21.0.10 and remains the approved experimental runtime vehicle.

## FunctionGemma smoke — 2026-10-02
Artifact: `todolist-functiongemma_q8_ekv1024.litertlm`.
Five fixtures completed with zero runtime exceptions.
Observed latency: 4536–5302 ms per fixture.
Observed PSS during generation: 643137–702423 KiB.
Model initialization: 2655 ms; initial PSS 344920 KiB.

However, all observed outputs were protocol/function-oriented rather than clean quest text.
Responses contained `<start_of_turn>model`, `<escape>`, malformed argument-like fragments and repeated function-declaration content.
The attempted JSON `ResponseFormat` did not make FunctionGemma behave as a general JSON composer.

## Decision
Do not wire FunctionGemma into `PinhoQuestAppGraph` yet.
Do not broaden the sanitizer into generic regex repair.
The runtime/memory profile is promising, and the isolated harness now matches the official LiteRT-LM Kotlin tool declaration API.

## Native function-call probe — 2026-10-02
The harness defines exactly one pure text tool, `composeQuestText(title, description, objectives)`, using LiteRT-LM `ToolSet`, `@Tool`, `@ToolParam` and `tool(...)`.
The conversation disables automatic tool execution so no external side effects can occur.
Five fixtures completed with zero runtime exceptions.
Observed generation latency: 1906–3431 ms per fixture.
Observed generation PSS: 804363–815049 KiB.
Observed response envelope: `<start_function_call>call:compose_quest_text{...}<end_function_call>`.
In this model/runtime combination, `Message.toolCalls` was empty while the rendered/message representation contained the model-emitted function-call envelope.
The envelope includes a tool name and argument object, but the 270M model reproduced function-declaration material inside the arguments in several fixtures instead of clean quest text.
A second prompt iteration that removed explicit tool-name/schema wording did not improve adherence: the model instead emitted the training-aligned `add_task` function declaration.
Therefore the native tool API is confirmed, but this checkpoint is not yet suitable for the required `compose_quest_text` contract.

The strict `FunctionGemmaToolCallExtractor` is implemented in `quest-core` and only accepts the exact `compose_quest_text` envelope plus the three bounded text arguments. It is covered by positive and negative unit tests.
`MicroQuestComposer` now tries this native extractor first and falls back safely; a failed local parse is explicitly marked `PROCEDURAL_FALLBACK`.

## FunctionGemma trigger discovery — 2026-10-02
The official Kotlin example uses `ConversationConfig(systemInstruction = Contents.of("You can do function call."), tools = ...)`. The official FunctionGemma formatting guide also documents a function-calling trigger/context and the six control tokens.
Adding the system instruction changed the observed behavior materially: `Message.toolCalls` became populated for a custom `compose_quest_text` tool.
With the original three-field tool (`title`, `description`, `objectives`), the checkpoint still returned only a `title` argument.
A single-string `compose_quest_text(text)` experiment produced a real tool call but returned only one short action from the source, rather than the requested humanized multi-part quest.
This establishes that the runtime/tool protocol is correct, while zero-shot semantic composition for this custom schema is insufficient.

Google's FunctionGemma guidance explicitly notes that smaller models have limited intent capacity and that fine-tuning is the route for consistent non-standard tool schemas.

## Next gate
Do not add generic repair logic.
Do not promote partial tool output to a complete quest.
Keep the deterministic generator as the authority for structure, facts, difficulty, time and objectives.
Evaluate one of two bounded paths:
1. a FunctionGemma checkpoint already aligned with the desired composition tool; or
2. a tiny fine-tuning/distillation experiment using deterministic PinhoQuest examples to teach exactly one micro-composition contract.
The production gate remains: valid bounded text + deterministic rendering + `QuestValidator`, with procedural fallback on any failure.


## P3 SFT pilot120 — 2026-10-02

Artifact: `D:\AI\HuggingFacesLLM\p3_sft_small\pilot120\work\checkpoint`.

The isolated CPU LoRA experiment completed 60/60 training steps with exit code 0 and checkpoints at steps 10, 20, 30, 40, 50 and 60 plus the final checkpoint. The base model remained the local FunctionGemma 270M checkpoint.

Validation evidence:
- 30/30 validation cases emitted exactly one `compose_quest_text` call.
- 30/30 contained the complete `title`, `description` and `objectives` contract.
- 12/12 independent unseen cases emitted exactly one complete call.
- 11/12 unseen cases matched both approved fact and objective strings exactly; the remaining case preserved the approved meaning with a wording variation.
- The same 12 unseen cases evaluated against the bucket zero-shot ONNX model produced 0/12 complete calls; outputs were malformed, empty, unrelated or repeated function-like text.

This is evidence that the small SFT experiment materially improved adherence to the P3 composition contract. It is not, by itself, production approval.

## Pipeline gate — 2026-10-02

Added `MicroQuestComposerPipelineGateTest` covering the canonical convergence boundary:
`model output -> strict extractor/sanitizer -> MicroQuestRenderer -> QuestValidator`.

Four scenarios pass:
1. valid native FunctionGemma call -> `LOCAL_MODEL` -> `QuestValidator.Valid`;
2. valid JSON model output -> bounded sanitizer -> `LOCAL_MODEL` -> `QuestValidator.Valid`;
3. malformed model output -> `PROCEDURAL_FALLBACK` -> `QuestValidator.Valid`;
4. inference failure -> `PROCEDURAL_FALLBACK` -> `QuestValidator.Valid`.

The full `:quest-core:test` suite also passes.

## Adapter boundary observation

The current Android `AndroidLiteRtLmInferencePort` requests `ResponseFormat.json(...)` and serializes the rendered response string; it does not currently expose the native tool-call path used by the isolated FunctionGemma probe. Therefore the pipeline gate proves the core convergence contract and both accepted response representations, but it does not yet prove an on-device Android run using the pilot120 weights.

## P3 Android runtime probe — 2026-10-02

The isolated Android harness built successfully with LiteRT-LM 0.17.1, Java 21 and Kotlin 2.4.0, and the debug APK installed successfully on the connected `emulator-5554`.

The harness process remained alive after launch without emitting the expected `P3Lifecycle` benchmark lines or producing the benchmark result file. The only observable runtime artifact was an approximately 272 MB XNNPACK cache for `todolist-functiongemma_q8_ekv1024.litertlm`. Repeated launches reproduced the same behavior; there was no crash stack in the filtered `logcat` capture.

This is a runtime-execution evidence gap, not a contract failure. The result does not justify wiring the same dependency into the production app or claiming pilot120 on-device approval.

## Current gate

`quest-core` targeted P3 pipeline gate: PASS.
`quest-core` full test suite: PASS.
Android experimental build/install: PASS.
Android FunctionGemma execution evidence: NOT ESTABLISHED.
Production model integration: NOT APPROVED.

Next frontier: isolate the LiteRT-LM Android runtime execution boundary (model artifact, initialization and first generation) in the experimental harness before considering any production adapter change. Keep the production app free of the incompatible LiteRT-LM dependency until that evidence exists.
