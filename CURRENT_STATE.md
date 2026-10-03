# Pinho Quest — CURRENT STATE

Date: 2026-10-03
Branch: feature/cr-0-runtime-consolidation
CR frontier: CR-7 — Native hardening validation (implemented_unvalidated; blocked on JDK 21 runtime/toolchain availability)
Baseline: f17ae65 — docs(p3): diagnose toolcalling contract boundary

## 1. Current project state

Pinho Quest remains a governed Android-first micro-adventure generator.

Production quest generation is still deterministic/procedural:

QuestRequest
-> QuestSessionService
-> QuestContextProvider
-> QuestPlanner
-> QuestGenerationPlan
-> ProceduralComposer
-> QuestValidator
-> Room

FunctionGemma is NOT wired into PinhoQuestAppGraph production.

The P3 FunctionGemma work remains experimental and is being consolidated before any production wiring.

## 2. P3 experimental state

The current P3 branch proved several important facts:

- FunctionGemma native tool-call transport can be exercised on Android through LiteRT-LM.
- The isolated native probe can register composeQuestText and receive Message.toolCalls.
- The quantized pilot120 artifact can reach a native tool call on Android.
- The quantized pilot120 Android result can omit objectives even when native tool calling is used.
- The FP ~1.75 GB Android A/B terminated with LOW_MEMORY before inference and is therefore inconclusive.
- Constrained decoding produced a semantically incomplete call and unacceptable latency; it is not the primary correction path.
- SFT hardening improved native-call completeness in host evaluation but did not prove runtime termination/transport correctness.
- No further expensive model/quantization experiment should precede contract consolidation.

## 3. Current diagnosis

The strongest diagnosis is contract/protocol divergence between training and runtime, with a secondary mobile-conversion regression.

Four runtime/governance breaks are the main CR frontier:

1. Tool registration mismatch (historical, corrected in CR-3):
   the Android adapter previously used tools=emptyList(), while FunctionGemma was trained for native tool calling.

2. Protocol mismatch (historical, corrected in CR-3):
   the Android P3 adapter previously requested generic ResponseFormat.json(...) instead of using the native compose_quest_text tool protocol.

3. Contract duplication:
   P3 has a three-field FunctionGemma contract while P3-5A/QuestDraftCodec also defines a six-field structured-output contract. The MicroQuestPromptSerializer is a third, plain-text representation.

4. Budget fragmentation:
   prompt, output, engine, cache and conversion limits exist at different layers without one canonical inference budget.

Additional governance defects:

- MicroQuestPromptSerializer does not emit the native FunctionGemma developer/tool declaration path.
- P3-5A PromptFactsAssembler/BoundedPromptEnvelope exists but is not connected to MicroQuestComposer.
- MicroQuestCompositionRequest bounds collection sizes but does not fully sanitize/bound individual tag/example contents.
- MicroQuestComposer currently permits native FunctionGemma extraction and generic JSON sanitization as two output protocols.
- The SFT dataset generator duplicated the function-declaration start marker.
- Core inference now exposes a typed native ToolCall outcome, but the legacy raw String Success outcome remains until CR-4 removes the second output boundary.

## 4. Authority boundary to preserve

Deterministic system remains authoritative for:

- category;
- environment;
- difficulty;
- duration;
- game candidate;
- tags and tag-derived policy;
- facts/provenance;
- validation;
- persistence;
- reward/progression.

FunctionGemma may compose only:

- title;
- description;
- objectives.

RAW model output must never reach:

- renderer;
- UI;
- persistence;
- domain Quest construction.

RAW may exist only inside the runtime adapter/parsing boundary and must become a bounded typed result before crossing into core.

## 5. Tag flow

Current deterministic tag flow:

Room tags
-> SystemTagCatalog.categoryAffinities()
-> QuestContext.categoryAffinities
-> QuestPlanner
-> QuestGenerationPlan.selectedCategory

Tag labels are not currently passed directly to MicroQuestComposer.

If tags later influence wording, they must cross through:

approved tag labels
-> PromptFactsAssembler
-> bounded PromptTag values
-> BoundedPromptEnvelope
-> canonical FunctionGemma prompt
-> MicroQuestText

No Room entity, raw profile/history, affinity internals, raw research payload, URL or HTML may cross the LLM boundary.

## 6. Target runtime contract

Target P3 architecture:

QuestGenerationPlan
-> PromptFactsAssembler
-> BoundedPromptEnvelope
-> canonical FunctionGemma developer/tool declaration
-> LiteRT-LM ConversationConfig(tools=[composeQuestText], automaticToolCalling=false)
-> Message.toolCalls
-> strict typed decoder
-> MicroQuestText
-> MicroQuestRenderer
-> QuestValidator
-> Quest

There must be one semantic tool contract. Kotlin tool registration, FunctionGemma declaration, training examples and tests are representations of that contract, not independent definitions.

## 7. Current CR frontier

CR-0 planning/documentation is complete.
CR-1 canonical FunctionGemma tool contract is implemented and validated by quest-core tests.
CR-2 bounded prompt/input governance is implemented and validated by quest-core tests.
CR-3 native Android tool transport is implemented and validated_bounded for source/build/bridge packaging scope.

No production FunctionGemma wiring is approved yet.
No new SFT run is approved yet.
No new large Android conversion/A-B is approved yet.

CR-4 is now the next frontier: remove the legacy raw/generic output path and converge P3 on native ToolCall only.

## 8. Sprint map

CR-0 — Planning and documentation
- Freeze diagnosis and boundaries.
- Define canonical contract architecture.
- Define sprint order and gates.
- No runtime code.

CR-1 — Canonical FunctionGemma tool contract
- Create one source of truth for compose_quest_text.
- Derive/verify argument names, types and bounds.
- Resolve three-field vs six-field ownership.
- Add contract characterization tests.

CR-2 — Bounded prompt and input governance
- Connect P3 to PromptFactsAssembler/BoundedPromptEnvelope or a unified equivalent.
- Sanitize tags/examples before serialization.
- Define exactly which facts may cross the boundary.
- Prove no raw domain/research payload crosses.

CR-3 — Native Android tool transport
- Register exactly compose_quest_text in LiteRT-LM.
- Use manual tool calling and Message.toolCalls.
- Remove generic JSON response format from the FunctionGemma path.
- Introduce typed inference outcome at the adapter boundary.

CR-4 — RAW isolation and output convergence
- Remove silent second output protocol from P3.
- Native tool call -> strict decoder -> MicroQuestText.
- Invalid native call -> governed failure/procedural fallback.
- Prove RAW never reaches renderer/UI/persistence.

CR-5 — Canonical inference budget
- Define prompt, output, context/cache and tool-call budgets.
- Tie adapter limits to one budget object/policy.
- Instrument requested vs actual consumption.
- Establish admission/stop conditions.

CR-6 — Training/runtime contract regeneration
- Regenerate SFT data from the canonical contract/template path.
- Remove duplicated declaration markers.
- Ensure training and runtime use equivalent tool declaration semantics.
- Keep stop-boundary behavior consistent with native runtime.

CR-7 — Native hardening validation
- Re-run small host/native checks through the canonical protocol.
- Validate exact one-call behavior, all required arguments and bounded values.
- Validate latency and failure semantics.
- Gate any model artifact as candidate/validated only from evidence.

CR-8 — Mobile conversion and quantization A/B
- Compare the corrected native runtime across approved artifacts.
- Measure tool-call completeness, latency, PSS/RAM, termination and context behavior.
- Treat FP LOW_MEMORY as a capacity limitation, not a semantic failure.
- Quantization regression is accepted only with reproducible evidence.

CR-9 — Production integration gate
- Only after CR-1..CR-8 pass.
- Wire the validated local composer into PinhoQuestAppGraph.
- Preserve ProceduralComposer as governed fallback.
- Run full quest-generation E2E.
- Update production architecture/current state only after runtime evidence.

## 9. Cross-sprint invariants

These rules apply to every CR sprint:

1. One canonical tool contract.
2. One canonical model-facing input boundary.
3. One canonical FunctionGemma output protocol.
4. No silent parser protocol switching.
5. No raw input to the model.
6. No raw model output to UI/render/domain/persistence.
7. Deterministic metadata remains authoritative.
8. Exactly one P3 tool call per request.
9. Budget is bounded before inference.
10. Fallback converges into the same validator.
11. No device-specific bypass.
12. No production wiring before validation gates pass.
13. Small checkpoints only; no giant cross-sprint implementation.
14. Evidence distinguishes observed fact, inference and hypothesis.
15. A model/conversion regression is not "fixed" with regex repair unless the protocol itself remains valid.

## 10. Evidence baseline

Primary diagnostic:
docs/evidence/p3-toolcalling-contract-diagnosis.md

Baseline commit:
f17ae65

Relevant evidence already collected:

- native LiteRT-LM tool transport works in the isolated probe;
- bucket FunctionGemma produces native tool calls but not the P3 contract reliably;
- quantized pilot120 produces native tool calls but can omit objectives;
- FP Android run is inconclusive because of LOW_MEMORY;
- constrained decoding is too slow and still semantically incomplete;
- host SFT hardening does not replace native runtime validation.

## 11. CR-0 decision

CR-0 does not claim that the model itself is defective.

CR-0 records the working hypothesis:

P3 training and runtime currently speak different contracts, while mobile conversion is a secondary independent variable.

The first correction must therefore be architectural contract convergence, not additional training or larger inference budgets.

## 12. CR-1 / CR-2 implementation checkpoint

### CR-1 — canonical tool contract

Implemented in quest-core:
- `MicroQuestToolContract` is the single semantic source for `compose_quest_text`.
- Canonical arguments are exactly `title`, `description`, and `objectives`.
- Canonical bounds are title 1..80, description 12..220, objectives 1..4, each objective 1..120.
- The FunctionGemma extractor and legacy sanitizer now reference the canonical contract instead of independent bounds.
- Characterization tests explicitly prove the three-field tool does not own category, environment, duration, difficulty, bonus objectives, XP or rewards.
- The six-field `QuestPromptContract` remains a separate structured-draft protocol and is not silently reused as the P3 FunctionGemma tool schema.

### CR-2 — bounded prompt/input governance

Implemented in quest-core:
- `PromptFactsAssembler.assembleTagLabels()` reuses the existing deterministic tag sanitization/bounding path for P3 labels.
- `MicroQuestPromptFactsAssembler` is the P3 model-facing input boundary.
- `BoundedMicroQuestPrompt` is the only request payload accepted by `MicroQuestPromptSerializer`.
- Tags are control-character sanitized, whitespace normalized, deduplicated, sorted and capped at the existing prompt limit.
- Examples are control-character sanitized, whitespace normalized, field-bounded, objective-bounded and capped at three approved examples.
- Serializer tests prove control characters and oversized collections do not cross into the serialized model prompt.

Validation evidence:
- `:quest-core:test` completed successfully after CR-1/CR-2 implementation.
- No production FunctionGemma wiring was enabled.
- No model retraining or conversion experiment was started.

Checkpoint state: `validated_bounded` for the quest-core scope. Android transport, native Message.toolCalls and production wiring remain outside this gate.

## 13. CR-3 implementation checkpoint

### Native Android tool transport

Implemented:
- Added :litertlm-bridge as the sole source module that imports the LiteRT-LM API.
- Registered exactly one native P3 tool: compose_quest_text.
- Tool metadata is derived from MicroQuestToolContract; no second semantic schema was introduced.
- automaticToolCalling=false is preserved.
- The runtime reads native Message.toolCalls instead of rendering model text and reparsing generic JSON.
- Exactly one tool call is required; unexpected tool names, missing arguments, extra arguments and zero/multiple calls become InferenceOutcome.InvalidOutput.
- A new InferenceOutcome.ToolCall crosses the adapter boundary as typed data.
- AndroidLiteRtLmInferencePort now depends on the bridge and no longer imports LiteRT-LM directly.
- Generic ResponseFormat.json(...) was removed from the P3 Android transport path.

### Toolchain isolation

LiteRT-LM 0.17.1 carries newer Kotlin metadata than the PinhoQuest Kotlin 2.1.21 toolchain. The solution does not upgrade the project globally.

Instead:
- the bridge compiles against LiteRT-LM as Java source;
- the bridge uses JDK 21 for compilation while its emitted Java bytecode remains Java 17 compatible;
- the app receives LiteRT-LM at runtime;
- app *CompileClasspath configurations are pinned to the project's Kotlin stdlib 2.1.21, preventing the LiteRT runtime's Kotlin metadata from entering Kotlin source compilation.

This is a build-boundary compatibility rule, not an application/runtime bypass.

### Validation evidence

- :litertlm-bridge:test — BUILD SUCCESSFUL.
- :app:compileDebugKotlin — BUILD SUCCESSFUL.
- :app:testDebugUnitTest — BUILD SUCCESSFUL.
- :app:assembleDebug — BUILD SUCCESSFUL.
- Debug APK size: 60,983,037 bytes.
- APK inspection confirms lib/arm64-v8a/liblitertlm_jni.so and lib/x86_64/liblitertlm_jni.so are packaged.

### Gate status

CR-3 status: validated_bounded for source, bridge tests, Kotlin classpath isolation and APK packaging.

Not yet claimed:
- semantic success of the selected FunctionGemma artifact through this new production bridge;
- native device matrix/hardening evidence;
- removal of the legacy raw/generic output path.

Those belong to CR-7 and CR-4 respectively.

Next frontier: CR-5 — Canonical inference budget governance.
## 14. CR-4 implementation checkpoint

### RAW isolation and output convergence

Implemented:
- Removed InferenceOutcome.Success(String) from the core inference contract.
- Removed the generic JSON response sanitizer from the P3 runtime and its tests.
- Removed the raw function-call text extractor from the P3 runtime and its tests.
- Added MicroQuestToolCallDecoder, which accepts only typed InferenceOutcome.ToolCall data and enforces MicroQuestToolContract types and bounds.
- MicroQuestComposer accepts only native ToolCall as a model-success path.
- Invalid native calls and non-tool inference outcomes converge directly to ProceduralComposer.
- MicroQuestRenderer receives MicroQuestText, never raw model text.

Validation evidence:
- :quest-core:test — BUILD SUCCESSFUL.
- Static source search found no runtime/test references to InferenceOutcome.Success, FunctionGemmaResponseSanitizer or FunctionGemmaToolCallExtractor outside documentation/evidence.

Gate status: validated_bounded for the P3 core output boundary.

Remaining validation belongs to CR-7+ only: native device hardening, model artifact evidence and production integration.

Next frontier: CR-5 — canonical inference budget governance.

## 15. CR-5 implementation checkpoint

### Canonical inference budget

Implemented:
- Added InferenceBudget as the single P3 inference policy.
- P3 values are centralized at 1200 prompt characters, 1280 context tokens, 128 output tokens and exactly 1 tool call.
- GenerationRequest now carries the whole budget instead of an independent output-token value.
- GenerationRequest rejects prompts above the budget before the inference port is called.
- MicroQuestPromptSerializer enforces the same budget object rather than a disconnected character constant.
- AndroidLiteRtLmInferencePort passes the canonical budget into the LiteRT-LM bridge.
- LiteRtLmRuntime configures EngineConfig and sendMessage from that same budget.
- The bridge rejects a request whose budget does not exactly match its configured runtime budget; a lower layer cannot silently expand or replace the policy.

### Telemetry

Implemented:
- Added InferenceTelemetry and InferenceTelemetrySink.
- The bridge records requested prompt/context/output/tool-call ceilings.
- LiteRT-LM Conversation.getTokenCount() is sampled before and after the request, giving an observed conversation-token delta without pretending it is a direct output-token counter.
- Observed native tool-call count and explicit stop reason are recorded.
- No raw prompt/model payload is included in telemetry.

Important limitation:
LiteRT-LM 0.17.1 Message does not expose a direct output-token usage field. CR-5 therefore does not fabricate one. Exact output-token consumption remains a CR-7/native instrumentation question if a future API exposes it.

### Validation evidence

Passed:
- :quest-core:test
- :litertlm-bridge:test
- :app:testDebugUnitTest
- :app:compileDebugKotlin

Static review found no remaining P3 source definitions matching the previously scattered 1200-character, 128-output-token or 1280-engine-token literals outside the canonical budget.

Gate status: validated_bounded for canonical budget enforcement and build integration. Runtime token-usage semantics remain bounded by the documented API limitation above.

Next frontier: CR-6 — training/runtime contract regeneration.

## 16. CR-6 implementation checkpoint

### Training/runtime contract regeneration

Implemented:
- Added the committed training generator at `tools/p3_training/generate_dataset.py`.
- The generator reads `MicroQuestToolContract.kt` as the semantic authority instead of re-declaring the P3 tool name, required arguments, types and descriptions in Python.
- The FunctionGemma declaration is assembled from that parsed contract and emits exactly one declaration-start marker.
- The SFT completion is bounded at `<end_function_call>`; the old synthetic `<start_function_response>` tail is rejected by validation rather than being taught as part of the model completion.
- Existing dataset coverage is preserved: 6 categories × 5 environments × 3 difficulties × 4 language slices = 360 rows, shuffled with the historical seed 42 and split 80/20.

### Validation evidence

Passed:
- 6/6 Python contract/regeneration tests.
- Generated dataset: 360 rows, 288 train, 72 validation.
- Generated dataset contains exactly one `compose_quest_text` call per completion.
- Generated dataset contains zero duplicated `<start_function_declaration>` markers.
- Generated dataset contains zero `<start_function_response>` completion tails.
- All generated completions end at `<end_function_call>`.

The implementation deliberately does not start another SFT run. CR-6 proves regeneration shape and contract equivalence; CR-7 owns native hardening validation.

Gate status: validated_bounded for training-data generation, declaration uniqueness and stop-boundary dataset shape.

Next frontier: CR-7 — native hardening validation.

## 21. CR-7 hardening checkpoint

Status: implemented_unvalidated; native bridge build/test is available with JDK 21, but semantic device validation remains blocked because the current core serializer does not yet share the canonical FunctionGemma chat-template declaration with the CR-6 generator and no native probe exists in this worktree.

Implemented in this checkpoint:
- Expanded `LiteRtToolCallMapperTest` into an explicit native-protocol matrix covering zero, one and multiple calls; unexpected tool name; missing, extra and null argument maps.
- Preserved the one-call and exact-argument invariants from `MicroQuestToolContract`.
- Added/kept the bridge Java toolchain boundary configured to use a Java 21 compiler while emitting Java 17 bytecode, matching the LiteRT-LM 0.17.1 API artifact.

Validation:
- `:quest-core:test` — BUILD SUCCESSFUL.
- `:litertlm-bridge:test` is currently blocked because the local machine has only JDK 17 installed, while `litertlm-android:0.17.1` contains Java class version 65 (Java 21). The attempted build therefore fails before the new native matrix can execute.
- No model inference run was started under the broken compile boundary.

Gate decision:
CR-7 remains `implemented_unvalidated`, not `validated_bounded`. The blocker is environmental/toolchain, not a semantic model conclusion.

CR-8 is intentionally not started: conversion/quantization A/B must reuse a validated native adapter/tool protocol first.

## CR-7 / CR-8 native Android evidence � 2026-10-03

CR-7 native probe is implemented at `app/src/androidTest/kotlin/com/pinhoquest/inference/P3NativeToolCallE2ETest.kt`. It invokes the real `AndroidLiteRtLmInferencePort`/`LiteRtLmRuntime`/`P3LiteRtToolSet` path and requires exactly one `compose_quest_text` call with the canonical `title`, `description` and `objectives` arguments.

Environment gate: Microsoft OpenJDK 21 installed; bridge/core/app unit tests and debug APK/test APK build successfully. APKs install on `emulator-5554` (x86_64/API 33).

Runtime gate: the quantized pilot120 artifact `D:\AI\HuggingFacesLLM\p3_sft_small\pilot120\litert_export\model.litertlm` (453495680 bytes) initialized and executed on-device. The process reached approximately 815 MiB PSS and 313% CPU during generation. The probe completed in approximately 14.7 s but returned `InferenceOutcome.InvalidOutput`, so the native runtime transport works while the model/contract gate fails.

A/B note: the no-PTQ pilot120 artifact is 1751747420 bytes and remains excluded from this rerun because existing evidence records emulator LOW_MEMORY. No semantic conclusion is drawn from that artifact here. A non-P3 FunctionGemma mobile-actions artifact was also tested only as a control and classified `ModelUnavailable`, so it is not a valid P3 comparator.

Gate: CR-7 remains `implemented_unvalidated`; CR-8 remains `implemented_unvalidated`/experimental. No production integration is approved. The next bounded frontier is to diagnose the quantized pilot120 native payload (especially missing/incorrect required arguments) or produce a mobile-converted checkpoint whose native `Message.toolCalls` matches the canonical P3 contract.


## 22. CR-7 semantic/runtime diagnosis — 2026-10-03

The native probe was extended temporarily with test-only diagnostics and then restored to the production-clean bridge. The key finding is stronger than the earlier “missing objectives” hypothesis:

- Quantized pilot120 initialized and executed through the real Android path, but Message.toolCalls contained zero native calls.
- The model instead emitted a textual pseudo-function-call beginning with <start_function_call> inside Message.contents.
- A captured sample contained call:compose_quest_text, but used positional-looking arg0/arg1/arg2, malformed extra fields, and even an unexpected call:pause. This is not the canonical native tool protocol and must not be repaired by a production parser.
- Three real quest prompts were exercised on-device with the pilot120 artifact. Elapsed generation times were approximately 7.45 s, 4.06 s and 4.27 s. The first run is treated as a colder run; the later ~4.1–4.3 s results are the more useful warm-runtime latency signal. All three returned InvalidOutput with observedToolCalls=0.
- Semantic quality is currently below the PinhoQuest bar. Captured output included phrases such as “Jogue de outro jeito” and “Pequeno computador”, plus malformed objective-like text. It did not reliably preserve the requested friendly, playful, low-friction quest tone or the three canonical semantic fields.

Gemma comparison:
- functiongemma-270m-ft-mobile-actions_Google_Tensor_G5.litertlm was previously classified ModelUnavailable in this harness.
- functiongemma-270m-ft-mobile-actions_Google_Tensor_G6.litertlm was also classified ModelUnavailable in this harness, with about 2.1 s until classification. Therefore neither artifact is a valid semantic/latency comparator for P3 on this device/runtime.

Fine-tuning assessment:
- A targeted fine-tuning pass is now a plausible next experiment, but only after deciding the desired serving format. The pilot120 evidence suggests the learned completion format is drifting toward a textual FunctionGemma-style function-call representation instead of the LiteRT-LM native tool boundary.
- The preferred correction is not to add a text parser. A small governed SFT/adapter experiment should instead teach the exact native-call-compatible representation expected by the selected runtime/export path, while preserving MicroQuestToolContract as the sole semantic authority.
- If the existing pilot120 checkpoint is the cheapest viable base, it is the first candidate for a small corrective SFT. Gemma becomes a candidate only if a Gemma artifact that actually initializes under the target LiteRT runtime is produced.

Gate decision:
CR-7 remains implemented_unvalidated. The runtime path is proven executable, but native semantic adherence and quest quality are not. CR-8 remains experimental and should not be promoted yet. P4/P5 remain outside this blocker and unchanged.

Production bridge state after diagnosis:
- Temporary raw-output diagnostics were removed from LiteRtLmRuntime and LiteRtToolCallMapper.
- The temporary three-sample quality probe was removed; its measurements are recorded here as evidence rather than as a permanent benchmark harness.

## 23. CR-7.1 — corrective SFT and native-call diagnosis — 2026-10-03

CR-7.1 produced a corrective SFT pilot from the pilot120 adapter/checkpoint rather than introducing a parser or production-side output repair.

### SFT
- Clean corrective dataset created under D:\AI\HuggingFacesLLM\p3_sft_small\cr71 with 84 training examples and 12 validation examples covering organization, music, games, creativity, curiosity, pause, drawing and photos.
- Every target contains exactly one <start_function_call>, the canonical compose_quest_text name, all three semantic fields (title, description, objectives) and <end_function_call>; no <start_function_response> is present in the target dataset.
- A short pilot reached checkpoint cr71\checkpoint_step10. Local generation from that checkpoint with a 256-token allowance produced one complete canonical call (CALLS=1, END=1) containing title, description and objectives. The same checkpoint at 128 tokens was truncated before <end_function_call>, proving the old P3 output budget was too small.
- A further 12-step continuation was rejected as a candidate: it began repeating tool calls and response markers. It was retained only as experimental evidence; checkpoint_step10 is the better SFT candidate.

### Runtime contract correction
- InferenceBudget.P3.maxOutputTokens was raised from 128 to 256 and the corresponding unit expectation was updated.
- This is a bounded semantic change: the model needs enough output budget to serialize the three required arguments and close the native call. It is not a case-specific output repair.

### Root cause of missing native ToolCall
The decisive diagnosis is export metadata, not the mapper:
- The pilot120 .litertlm was exported with llm_model_type.generic_model.
- The known FunctionGemma G5 artifact declares llm_model_type.function_gemma and uses FunctionGemma stop-token metadata.
- With the generic pilot120 bundle, Android telemetry reported observedToolCalls=0 even when the model generated textual <start_function_call> content.
- A diagnostic hybrid bundle using FunctionGemma metadata over the pilot graph changed telemetry to observedToolCalls=1. It then failed later with INVALID_NATIVE_TOOL_CALL / native cleanup corruption because the graph and metadata sections were mixed; this hybrid is diagnostic-only and must not be shipped.

Therefore the production correction is: re-export the CR-7.1 checkpoint through the proper FunctionGemma/LiteRT-LM export path, including function_gemma model-type metadata and matching graph/metadata sections. Do not patch metadata into an unrelated .litertlm bundle.

### Android / latency evidence
- The old generic pilot120 path: warm real-generation signal ~4.1–4.3 s, but no native tool call.
- Hybrid FunctionGemma-metadata diagnostic: native ToolCall channel was observed (observedToolCalls=1), but the intentionally invalid mixed bundle later crashed in native Engine.close with Scudo corruption. This is not a product latency result.
- The local SFT checkpoint is therefore semantically promising, but the final on-device latency/quest-quality gate remains pending a valid re-export.

### Export environment blocker
The official litert-torch/AI Edge export path could not be installed in the current Windows environment because the required ai-edge-tensorflow/litert-converter wheel is unavailable for this environment. WSL on this machine was also non-responsive. No fake/manual binary patch is being promoted as an alternative.

### Gate
- CR-7.1 SFT: implemented_bounded as an experimental training artifact; checkpoint_step10 is the preferred candidate.
- Native runtime correction: identified and bounded, but final valid FunctionGemma export is still required.
- CR-7 overall: remains implemented_unvalidated.
- CR-8: remains experimental/unvalidated.
- P4/P5: unchanged.
