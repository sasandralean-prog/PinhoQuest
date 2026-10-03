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

The first instrumented probe established the complete runtime lifecycle for `todolist-functiongemma_q8_ekv1024.litertlm`:

- Engine construction: approximately 44 ms.
- `engine.initialize()`: approximately 5.6 s; process PSS rose from about 41 MB to about 792 MB.
- Conversation creation: approximately 4.3 s; PSS remained around 788 MB.
- `sendMessage()`: approximately 2.8 s; peak PSS was about 800 MB.
- The probe completed and wrote `p3-lifecycle.tsv`.
- The response contained a real native `compose_quest_text` tool call.

The previous apparent silence was therefore an observability problem in the harness, not a LiteRT-LM execution failure. The harness now has explicit lifecycle checkpoints around engine construction, initialization, conversation creation and first generation.

### Runtime compatibility versus model suitability

The Android runtime is now proven capable of executing a FunctionGemma-style tool call. However, the tested `todolist-functiongemma_q8_ekv1024.litertlm` model is not P3-contract compliant.

Observed native tool call shape was effectively:

`compose_quest_text(text="Caminhe por dez minutos")`

rather than the required bounded composition contract:

`compose_quest_text(title, description, objectives)`

Therefore:

- Android runtime execution: **ESTABLISHED**.
- Native tool-call transport: **ESTABLISHED**.
- P3 composition contract on this model: **NOT SATISFIED**.
- Production model integration: **NOT APPROVED**.

The pilot120 SFT checkpoint remains a candidate model artifact, but it is not yet an Android LiteRT-LM artifact. The next model-selection step is to determine whether pilot120 can be exported/converted into the same mobile runtime while preserving the learned tool contract, or whether another small FunctionGemma checkpoint is a better mobile starting point.

## Current gate

`quest-core` targeted P3 pipeline gate: PASS.
`quest-core` full test suite: PASS.
Android experimental build/install: PASS.
Android FunctionGemma execution: PASS.
Tested bucket model P3 contract: FAIL.
Production model integration: NOT APPROVED.

The production app remains free of the experimental LiteRT-LM dependency until a model/runtime pair passes the P3 composition contract on-device.

## Pilot120 merge gate - 2026-10-03

The pilot120 LoRA checkpoint was successfully merged into a standalone Hugging Face checkpoint at:

D:\\AI\\HuggingFacesLLM\\p3_sft_small\\pilot120\\merged

The merged artifact contains the full model.safetensors (~1.07 GB) plus tokenizer/configuration files. The merge completed with exit code 0 using PEFT merge_and_unload() against the same local FunctionGemma 270M base used for training.

A post-merge CPU generation check reproduced the learned compose_quest_text envelope on multiple validation cases, including complete title, description and objectives fields. This confirms that the merge operation did not visibly degrade the learned P3 contract before mobile conversion.

## Mobile conversion gate - 2026-10-03

The next required artifact is a .litertlm bundle generated from the merged pilot120 checkpoint. The documented Google AI Edge path is PyTorch -> TFLite -> LiteRT-LM bundle, with FunctionGemma model metadata and the model tokenizer/chat template preserved.

The current Windows host cannot execute the required conversion toolchain as installed: ai-edge-torch 0.7.2 resolves through litert-torch versions that require litert-converter, for which no Windows wheel is available in the current package index. WSL is not installed on the host and Docker is not installed either.

This is an environmental conversion blocker, not a model-quality failure. No production code was changed to work around it, and no unverified artifact was copied into the Android harness.

### Gate status

- Pilot120 training: PASS.
- Pilot120 validation contract: PASS.
- Pilot120 merged checkpoint: PASS.
- LiteRT-LM conversion of pilot120: BLOCKED - conversion environment.
- Pilot120 on-device contract: NOT YET TESTED.
- Production model integration: NOT APPROVED.

The next bounded action is to run the same conversion recipe in a Linux-capable environment, then bring only the resulting .litertlm artifact into the existing instrumented Android harness. The Android gate remains unchanged: native tool call + exact bounded composition contract + deterministic renderer/validator must all pass before production integration.

## Android deployment repeatability gate - 2026-10-03

The experimental benchmark APK was rebuilt successfully and installed on the available Android emulator emulator-5554.

Two clean executions of the instrumented P3Lifecycle probe completed without runtime exception or process crash using the existing todolist-functiongemma_q8_ekv1024.litertlm artifact.

Run 1 observed approximately: engine initialization 3.57 s, conversation creation 0.74 s, sendMessage 1.23 s, peak PSS 843234 KiB.
Run 2 observed approximately: engine initialization 1.30 s, conversation creation 0.53 s, sendMessage 1.21 s, peak PSS 848620 KiB.

Both runs produced a real compose_quest_text tool call. The decoded arguments were effectively {text="Caminhe por dez minutos"} rather than the required three-field bounded composition contract (title, description, objectives).

This repeatability gate establishes that the Android deployment and LiteRT-LM execution path are reproducible on the available emulator, while the current runtime artifact remains unsuitable for the P3 composition contract.

The test did not deploy the pilot120 weights because pilot120 still lacks a converted .litertlm artifact. No production PinhoQuest code or production model dependency was changed.
