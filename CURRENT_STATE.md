# Pinho Quest — CURRENT STATE

Date: 2026-10-05
Branch: feature/cr-0-runtime-consolidation
CR frontier: CR-9 — productive Android JNI/LiteRT-LM integration diagnosis
Baseline: bdf4956 — feat(p5): harden settings and accessible user feedback

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


## 24. CR-7.2 — Linux FunctionGemma re-export and native Pilot120 validation — 2026-10-04

Status: `validated_bounded` for native transport; product semantic-quality gate remains open.

The CR-7.1 Pilot120 `merged_step10` checkpoint was re-exported through the existing Linux/Wsl2 `litetune` environment with the base identity `google/functiongemma-270m-it`. The exporter applied the FunctionGemma family rules instead of allowing the local `gemma3_text` config to fall through to `generic_model`:

- `--litert_lm_model_type_override=function_gemma`
- `--jinja_chat_template_override=.../litetune/templates/functiongemma.jinja`
- `--externalize_embedder`

Recipe: `dynamic_wi8_afp32`.

Export evidence:
- duration: 214.784 s;
- artifact: `D:\AI\HuggingFacesLLM\p3_sft_small\cr71\litetune_functiongemma_dynamic\dynamic_wi8_afp32\model.litertlm`;
- size: 456,643,888 bytes;
- SHA-256: `8cdb37d1debde293ca1975f036be6b948db05b32bda6b1235fdd789cbb78036e`;
- return code: 0.

The final unpacked bundle contains `llm_model_type { function_gemma {} }`, the FunctionGemma chat template, and `prefer_activation_type=fp32` on prefill/decode. The artifact was copied to the Android application sandbox and verified with the same size and SHA-256 before inference.

Native Android evidence:
- `P3NativeToolCallE2ETest.realModelReturnsExactlyOneCanonicalToolCall` passed;
- observed native tool calls: 1;
- stop reason: `NATIVE_TOOL_CALL`;
- observed conversation-token delta: 272;
- warm inference elapsed: 9,943 ms;
- exact tool name: `compose_quest_text`;
- exact argument set: `title`, `description`, `objectives`;
- `objectives` arrived as a typed `List`.

This establishes that the prior `generic_model` export was a real export-layer/runtime metadata problem. The native transport can now carry the Pilot120 checkpoint through `Message.toolCalls` without a production text parser or contract weakening.

Important limit: the current E2E prompt is a transport-focused probe, not the canonical production P3 prompt. Its generated objective echoed the instruction to call the tool, so product wording quality, relevance, tone and diversity are still not validated.

Decision:
- Pilot120 is promoted to the preferred P3 candidate for further validation.
- `MicroQuestToolContract` remains unchanged.
- No generic-text parser or case-specific repair is allowed.
- No production AppGraph wiring is approved yet.
- Next gate: canonical bounded P3 prompt E2E/quality benchmark on this valid FunctionGemma-exported Pilot120 artifact.

Evidence: `docs/evidence/p3-pilot120-functiongemma-reexport-2026-10-04.md`.


## 25. CR-7.3 — P3 semantic-quality benchmark — 2026-10-04

Status: `validated_bounded` for native FunctionGemma/LiteRT-LM transport; semantic-quality gate remains open and production wiring is not approved.

The valid CR-7.2 `dynamic_wi8_afp32` FunctionGemma artifact at `D:\AI\HuggingFacesLLM\p3_sft_small\cr71\litetune_artifacts\dynamic_wi8_afp32\model.litertlm` was copied to `emulator-5554` and exercised through the real `AndroidLiteRtLmInferencePort` / `LiteRtLmRuntime` / `P3LiteRtToolSet` path. The tested artifact SHA-256 is `b4006c215963c17735c0a6d5bcb5587c9fc581ef77aac930548395255c15d56d`. A separate same-size export exists under `litetune_functiongemma_dynamic` with SHA `8cdb37d1...`; it is not the artifact used for this benchmark.

### Native transport gate
- `P3NativeToolCallE2ETest.realModelReturnsExactlyOneCanonicalToolCall` passed: 1 test, 0 failures.
- Testcase time: 16.996 s; suite time: 20.747 s.
- Telemetry from the successful run: `observedToolCalls=1`, `stopReason=NATIVE_TOOL_CALL`.
- The mapper accepted exactly the canonical `title`, `description`, `objectives` argument set; no parser relaxation was introduced.

### Semantic battery
The temporary diagnostic battery `P3Pilot120SemanticE2ETest` exercised 12 real quest-generation cases on the same emulator/model path.

Observed results:
- 12/12 cases completed through the test harness;
- 11/12 produced a native `ToolCall` and decoded to `MicroQuestText`;
- 1/12 returned `TechnicalFailure` (learning case);
- measured latencies ranged from 5.581 s to 26.830 s;
- mean latency: approximately 7.86 s;
- median latency: approximately 5.85 s;
- excluding the cold first case, mean latency was approximately 6.14 s.

### Semantic-quality failures observed
The benchmark is intentionally not a promotion gate yet. It exposed real product-quality defects:
- repeated generic title `Jogue de outro jeito` across unrelated categories;
- title/description leakage from training examples, including `Exemplo aprovado | ...`;
- generated text that resembles a training instruction rather than a quest (`Escreve o texto final de uma quest humana curta.`);
- objectives that sometimes duplicate the description or include a generic terminal sentence only;
- one technical failure despite a valid native serving path;
- evidence of English title drift (`Detail Hunt`) in a Portuguese request;
- no evidence that a generic post-hoc text parser would solve these issues without hiding model-quality failures.

### Gate decision
CR-7 native transport/export is now `validated_bounded`.

CR-7.3 semantic quality remains `implemented_bounded`/open. The current artifact is fast enough to justify continued product-oriented tuning, but it is not yet good enough for production quest generation. The next correction should target the training/prompt data boundary and semantic consistency, not the native mapper or ToolCall transport.

The benchmark file remains uncommitted until its assertions are hardened into a deliberate semantic gate. No production AppGraph wiring is approved. P4/P5 remain unchanged.

---

# 23. Canonical P3 / P4 frontier — 2026-10-04

> **This section supersedes older frontier summaries above when they conflict with the current CR record.**

## P3 status

P3 is now **STANDBY / NON-BLOCKING FOR P4**.

| CR | Status | Decision |
|---|---|---|
| CR-0 | PASS | planning/documentation baseline |
| CR-1 | PASS | canonical FunctionGemma tool contract |
| CR-2 | PASS | bounded prompt/input governance |
| CR-3 | PASS | native Android tool transport / bridge boundary |
| CR-4 | PASS | RAW isolation and output convergence |
| CR-5 | PASS | canonical inference budget and telemetry boundary |
| CR-6 | PASS | training/runtime contract regeneration |
| CR-7 | PASS | semantic hardening / canonical artifact validation |
| CR-8 | PASS | Android resource/latency benchmark for canonical artifact |
| CR-9 | TECHNICALLY RESOLVED / E2E PENDING | productive Android LiteRT-LM storage contract corrected; production post-install E2E still requires the canonical external model artifact |

## CR-7 / CR-8 canonical evidence

The canonical CR-7.4 artifact is `D:\AI\HuggingFacesLLM\p3_sft_small\cr74_semantic_isolation\export_litert_canonical\model.litertlm`.

SHA-256: `e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb`.

Size: `284,692,656 bytes`.

Native Android validation established 5/5 successful generations with 5/5 `compose_quest_text` ToolCalls and no semantic/runtime errors in the validated native harness. CR-8 measured 5 generations with 2.55 s average wall time, 2.32–2.88 s range, approximately 956 MB average PSS and 959 MB maximum PSS, with no errors in that benchmark.

These results establish the model artifact and canonical native inference path as validated evidence. They do not imply that the current productive APK path is healthy.

## CR-9 — paused and blocked (historical diagnosis; superseded by checkpoint 34)

The following section records the earlier JNI hypothesis. The current Gate is PASS; see section 34 for the decisive filename/format finding.

### What was blocked

CR-9 is the production integration / productive UI E2E gate. The app can still operate through the deterministic `ProceduralComposer`; no additional fallback is being introduced and no model-specific bypass is being accepted.

### Earlier JNI hypothesis (superseded)

The productive APK initially appeared to fail before native inference because the JNI implementation for `NativeLibraryLoader.nativeCheckLoaded()` could not be resolved. Controlled loader probing later established that this message was the expected initial already-loaded probe, not the terminal failure. The decisive failure was the extensionless model filename.

Observed evidence:
- canonical `.litertlm` artifact is present;
- APK contains `lib/arm64-v8a/liblitertlm_jni.so` and `lib/x86_64/liblitertlm_jni.so`;
- LiteRT-LM dependency is 0.17.1;
- production graph points at the canonical model/composer path;
- existing native P3 instrumentation harness and productive APK differ in runtime behavior;
- productive E2E therefore stops at native runtime loading rather than at quest semantics.

### Why this is a BLOCK, not a model failure

CR-8 already supplied evidence that the canonical artifact can execute and generate native `compose_quest_text` calls in the validated native harness. The productive failure occurs at the Java/Kotlin ↔ LiteRT-LM ↔ JNI boundary before a valid tool call can be observed.

The next CR-9 investigation should compare side by side:
1. exact LiteRT-LM Java/Kotlin classes;
2. resolved AAR/dependency versions;
3. JNI `.so` contents and naming;
4. Android classloader/native-loader behavior;
5. ABI and packaging/extraction behavior;
6. differences between the CR-8/native harness and productive APK.

Do not solve this with another semantic fallback, parser repair, device-specific bypass or arbitrary `System.loadLibrary` workaround before the loader contract is understood.

### Resume condition

CR-9 can resume only when the productive APK can initialize LiteRT-LM and produce the canonical native ToolCall through the same governed boundary already validated in CR-7/CR-8. Then the full user-visible quest flow can be re-run.

P3 remains available for later resumption, but it does not block P4.

## P4 — OPEN

P4 is now the active frontier. Its purpose is Web Research and content/catalog enrichment, with online/offline semantic states, provenance, normalization, deduplication and persistent catalogs as defined in `Engineering_Genome/Planejamento.md`.

P4 must not wait for CR-9. The deterministic quest path remains the operational authority while P3 is in standby.

## Design consolidation

The internal UI design authority is now `docs/design/UI_DESIGN_CONTRACT.md`.

It consolidates the visual language, button hierarchy, navigation, screen hierarchy, loading/empty/error states, typography/density rules, pixel-art boundary, accessibility expectations and phase boundaries for P4–P7.

The design contract is intentionally semantic rather than pixel-final: P4 can implement functionality without creating screen-specific visual patterns, while P6 remains the phase for detailed visual polish and microinteractions.

## 24. P4.1 — governed research boundary and normalized game catalog core — 2026-10-04

P4 has started with a bounded, non-UI checkpoint focused on the semantic boundary that must exist before real web adapters and persistence are introduced.

### Implemented

- Added `ResearchOutcome<T>` with the canonical semantic states required by P4:
  - `Success`
  - `Unavailable`
  - `RateLimited`
  - `InvalidResponse`
  - `UnsupportedSource`
  - `TechnicalFailure`
- Added `ResearchProvenance` requiring a non-empty source URI and research timestamp.
- Added `GameResearchPort` as the core boundary for future web adapters; the core does not depend on a concrete provider.
- Added normalized `GameDiscovery` with canonical identity, platform/genre sets, availability and provenance.
- Added `GameDiscoveryCatalog` with deterministic identity normalization and provenance-preserving deduplication.
- Added unit coverage for semantic failure distinction, duplicate normalization/merge and the empty-catalog semantic state.

### Architectural decision

P4 does not start by wiring a provider directly into the UI. The research result is first represented as a governed semantic outcome, then normalized into domain-ready discoveries, then merged into a catalog. This preserves the P4 boundary:

```
external source
    ↓
adapter
    ↓
ResearchOutcome
    ↓
GameDiscovery normalization
    ↓
deduplication + provenance merge
    ↓
catalog
```

The web source remains an external dependency and never becomes a second authority for persistent state.

### Validation status

- `git diff --check`: PASS (only existing LF/CRLF warnings).
- Targeted Gradle test was started twice but the Gradle process remained blocked without producing test execution output; it was terminated rather than declaring a false PASS.
- Therefore P4.1 is `implemented_unvalidated` pending a successful test execution.

### Next bounded P4 step

Implement the first concrete research adapter behind `GameResearchPort`, with provider response normalization and semantic mapping to the six P4 states, before adding Room persistence or UI wiring. The adapter must be testable without requiring the real network in unit tests.

CR-9 remains PAUSED / BLOCKED and is not modified by P4.1.


## 25. P4.2 — concrete web research adapter — 2026-10-04

P4 progressed from the semantic research boundary into its first concrete provider adapter.

### Implemented

- Added ResearchHttpTransport so network I/O is replaceable in tests.
- Added UrlConnectionResearchHttpTransport using the Android/JDK standard HTTP stack; no new third-party networking dependency was introduced.
- Added WikipediaGameResearchAdapter behind the existing GameResearchPort.
- The adapter queries the public Wikipedia REST summary endpoint for a candidate and accepts the result as a game discovery only when the returned evidence explicitly contains "video game"/"video games".
- HTTP and parsing failures map to the governed P4 states:
  - 404 → Unavailable
  - 429 → RateLimited
  - other 4xx → UnsupportedSource
  - 5xx → TechnicalFailure
  - malformed/insufficient JSON → InvalidResponse
  - transport exception → TechnicalFailure
  - non-game page → Unavailable
- Successful discoveries preserve source URL and research timestamp through ResearchProvenance.
- The adapter deliberately leaves platform, genre and availability as unknown/empty when this source does not provide authoritative structured values. It does not invent facts to fill the schema.
- Added unit tests for successful normalization/provenance, non-game rejection and semantic HTTP/JSON failure mapping.

### Architectural boundary

The current P4 research path is now:

GameResearchPort
       ↑
WikipediaGameResearchAdapter
       ↓
ResearchHttpTransport
       ↓
external source
       ↓
ResearchOutcome
       ↓
GameDiscovery
       ↓
GameDiscoveryCatalog

The concrete provider remains outside quest-core; core depends only on the port and semantic models.

### Validation status

- git diff --check: PASS (only pre-existing LF/CRLF warnings).
- Targeted :android-data:testDebugUnitTest --tests com.pinhoquest.data.research.WikipediaGameResearchAdapterTest was attempted.
- Gradle repeatedly remained blocked during daemon/build startup without reaching test execution output. The process was terminated rather than declaring a false PASS.
- Therefore P4.2 remains implemented_unvalidated.

### Deliberate limitation

This checkpoint does not yet claim that Wikipedia is the final content authority for the game's catalog. It is the first real adapter proving the provider boundary and semantic failure mapping. Structured platform/genre/availability enrichment should be added through an appropriate source/adapter rather than inferred from prose.

### Next bounded P4 step

Close P4.2's validation if Gradle becomes runnable. Then introduce the catalog-facing orchestration layer that can combine provider results and preserve provenance/deduplication, before wiring Room or UI.

CR-9 remains PAUSED / BLOCKED and untouched.


## 26. P4.2 / P4.3 validation checkpoint — 2026-10-04

### P4.2 status update

The concrete Wikipedia game research adapter is now validated.

Additional build correction:
- android-data now applies the Kotlin serialization plugin required by its @Serializable provider-response DTOs.
- No project-wide Kotlin/runtime version change was introduced.

Validation evidence:
- :android-data:testDebugUnitTest --tests com.pinhoquest.data.research.WikipediaGameResearchAdapterTest — BUILD SUCCESSFUL.
- 3/3 adapter tests passed:
  - successful video-game summary → governed GameDiscovery + provenance;
  - non-game page → Unavailable;
  - HTTP/JSON failure semantics remain distinct.
- The earlier test failures were caused by the test fixture JSON being escaped inside a Kotlin raw string; the fixture was corrected and the adapter then passed.

P4.2 gate: validated_bounded.

### P4.3 — research orchestration

Implemented in quest-core:
- GameResearchProvider gives each provider an explicit stable id without putting provider identity into GameDiscovery.
- GameResearchCoordinator executes configured providers for the same query.
- Every provider outcome is preserved in GameResearchProviderResult.
- Successful items are merged through the canonical GameDiscoveryCatalog.
- Duplicate discoveries therefore converge through the existing identity normalization/provenance merge path.
- Provider failures are not silently converted into catalog emptiness; the run keeps the original semantic outcome for each provider.
- No Room persistence or UI wiring was introduced in this checkpoint.

Architecture:

GameResearchCoordinator
        |
        +--> provider A -> ResearchOutcome
        +--> provider B -> ResearchOutcome
        +--> provider N -> ResearchOutcome
                         |
                         v
                  GameDiscoveryCatalog
                         |
                         v
                    GameResearchRun

Validation evidence:
- :quest-core:test --tests com.pinhoquest.core.research.GameDiscoveryCatalogTest --tests com.pinhoquest.core.research.GameResearchCoordinatorTest — BUILD SUCCESSFUL.
- Coordinator tests prove successful results are usable while rate-limited providers remain explicitly recorded, and duplicate evidence is deduplicated with provenance retained.
- Catalog tests now also prove conflicting FREE vs FREE_TO_PLAY evidence remains UNKNOWN instead of selecting one source arbitrarily.

P4.3 gate: validated_bounded.

### Current P4 frontier

P4 currently has:
1. governed semantic research states;
2. provenance;
3. normalized game discoveries;
4. canonical deduplication;
5. concrete web adapter;
6. multi-provider orchestration.

Still outside this checkpoint:
- persistent research/catalog storage;
- cache/refresh policy;
- online/offline UI state;
- real provider composition in AndroidDataGraph;
- broader structured source enrichment for platform/genre/availability;
- flower research/catalog pack acquisition.

Next bounded step: define the persistent/catalog snapshot contract and refresh policy only after deciding which research facts must survive offline use. Do not make Room the first authority for raw provider responses; persist normalized, provenance-bearing catalog state.

CR-9 remains PAUSED / BLOCKED and untouched.

## 27. P4.4 — persistent normalized game catalog + cache policy — 2026-10-04

### Scope

P4 now crosses the persistence boundary for the normalized game catalog.

Implemented:
- GameCatalogCachePolicy defines an explicit TTL/freshness rule.
- GameCatalogSnapshot carries researchedAt + expiresAt together with the normalized catalog.
- GameDiscoveryCatalogStore is the core persistence boundary.
- RoomGameDiscoveryCatalogStore persists normalized discoveries and provenance, not raw provider responses.
- Room schema advanced from version 2 to version 3.
- MIGRATION_2_3 creates game_catalog_snapshots and game_discoveries.
- AndroidDataGraph exposes the catalog store and registers the migration.
- Successful empty snapshots are persisted as a valid semantic no-new-options state.
- Provider failures are not written by the store; the existing coordinator keeps provider outcomes separate from successful catalog data.

### Persistence contract

The stored game catalog contains only normalized facts:
- canonical identity/name;
- platforms;
- genres;
- availability;
- provenance source URI + research timestamp;
- snapshot research timestamp;
- explicit snapshot expiry.

Raw HTTP/JSON provider payloads are not persisted.

### Cache semantics

Current policy is explicit rather than implicit:
- fresh when now - researchedAt <= maxAge;
- stale after the TTL;
- expiry is stored with the snapshot;
- an empty successful snapshot remains distinguishable from a missing snapshot.

The policy is intentionally not hardcoded into the provider adapter. A later application-level refresh coordinator can decide whether to reuse, refresh, or preserve stale data according to this contract.

### Validation evidence

Passed:
- :quest-core:test :android-data:testDebugUnitTest :android-data:compileDebugKotlin — BUILD SUCCESSFUL.
- Migration2To3Test — 2/2 instrumentation tests PASS on Pixel_4_API_33 (Android 13).
- RoomGameDiscoveryCatalogStoreTest — 2/2 instrumentation tests PASS on Pixel_4_API_33 (Android 13).
- Room schema generation/copy completed successfully.
- Database migration and actual Room round-trip were exercised on the Android emulator.

### Gate

P4.4 status: validated_bounded.

### Still outside P4.4

Not yet wired:
- automatic online/offline refresh orchestration;
- stale-while-revalidate behavior;
- UI exposure of online/offline states;
- persistent provider-specific refresh metadata;
- flower research persistence/catalog acquisition.

Next bounded checkpoint: introduce the application-level refresh decision boundary that combines cache freshness + current research outcomes without allowing a failed provider to erase a previously valid catalog.

CR-9 remains PAUSED / BLOCKED and untouched.
## 28. P4.5 — dynamic adaptive game catalog cache boundary — 2026-10-04

### Scope

P4.5 refines the previous fixed-TTL cache into a dynamic, deterministic policy. Cache lifetime is derived from research signals instead of one immutable max-age value.

### Implemented

- GameCatalogCacheSignals captures provider success ratio and normalized catalog coverage.
- GameCatalogCachePolicy derives an effective freshness window bounded by configured minimum/maximum ages.
- Stronger successful research with broader usable catalog coverage receives a longer freshness window; weaker/partial research receives a shorter window.
- Cache decisions distinguish NoCache, NoUsableCache, UseFresh, RefreshRequired and ServeStale.
- Offline stale data remains usable instead of being discarded.
- A failed online refresh is represented as a refresh decision and does not erase the existing snapshot; persistence remains responsible only for successful normalized snapshots.
- No provider-specific hardcode or device-specific workaround was introduced.

### Architectural rule

> Cache lifetime is adaptive configuration, not a fixed case-specific constant.

The policy is deterministic for identical signals, bounded by explicit minimum/maximum configuration, and independent from any concrete web provider.

### Validation

Added focused unit coverage for adaptive freshness, offline stale preservation, online refresh decisions, missing-cache versus empty-catalog semantics, deterministic output, and refresh-failure preservation.

CR-9 remains PAUSED / BLOCKED and untouched.

### Next bounded P4 step

Wire the decision boundary into an application-level refresh coordinator that reads the persisted snapshot, executes research only when the policy requires it, writes only successful normalized snapshots, and preserves stale data across provider failures/rate limits.

### P4.5 validation result

Targeted validation completed after one test-fixture correction:
- :quest-core:test --tests com.pinhoquest.core.research.GameCatalogCachePolicyTest --tests com.pinhoquest.core.research.GameResearchCoordinatorTest — BUILD SUCCESSFUL.
- 8 tests completed successfully.
- git diff --check — PASS; only existing LF/CRLF warnings were reported.

P4.5 status: validated_bounded.

## 29. P4.6 — bounded game candidate rotation + usage-aware cache — 2026-10-04

### Scope

P4.6 adds a second cache-rotation axis beyond age: quest usage. The game catalog is intentionally bounded to a small candidate pool instead of becoming a long-lived game database.

### Implemented

- Default game candidate capacity is **20**.
- Research snapshots are bounded to the policy capacity before persistence.
- Candidate selection excludes identities already marked as used by the caller.
- Candidate ordering is deterministic: newest research evidence first, then canonical identity.
- Usage pressure participates in dynamic cache freshness: as the proportion of already-used candidates increases, the adaptive freshness window becomes shorter.
- When fewer than 20 unused candidates remain, the policy requests a usage-driven refresh.
- Existing provenance remains attached to normalized research facts.
- The persistent research store remains an evidence/catalog store; it does not invent game-usage facts.

### Important boundary

The current quest domain does **not yet carry a canonical game identity on GAME quests**. Therefore P4.6 deliberately does not guess that a game was used by parsing quest title/description or by inspecting unrelated completion text.

The policy already accepts an explicit `usedIdentityKeys` set. The next integration step is to connect that set to the canonical game-quest usage history once the game identity is present in the quest domain/completion boundary.

This preserves the architectural rule:

> A game is considered used only when the system has an explicit canonical game identity for that quest.

### Validation

Targeted tests:
- adaptive freshness with provider/catalog/usage signals;
- used-game exclusion;
- hard capacity of 20 candidates;
- fewer-than-20-unused refresh pressure;
- deterministic candidate order;
- stale/fresh cache decisions;
- missing-cache versus empty-catalog semantics;
- bounded GameResearchRun.snapshot().

`:quest-core:test --tests GameCatalogCachePolicyTest --tests GameResearchCoordinatorTest` — **BUILD SUCCESSFUL**.

CR-9 remains PAUSED / BLOCKED.


## 30. P4.7 — disposable game usage cycle + semantic UsageId — 2026-10-05

### Scope

P4.7 formalizes game reuse without turning usage memory into permanent game identity.

### Implemented

- Added permanent `GameIdentityKey` as the canonical game identity boundary.
- Added `GameCandidateCycleId` to identify a bounded candidate-pool generation.
- Added `GameQuestVariant` with structured semantic axes (`activity`, category, environment, objective pattern).
- Added `GameQuestUsageId`, a deterministic SHA-256 digest derived from a versioned canonical contract containing cycle, canonical game identity, and structured semantic variant.
- Raw quest title/description text is not used as the usage identity.
- Added `GameQuestUsage` and `GameQuestUsageStore` boundaries for disposable usage memory.
- `GameCatalogSnapshot` now carries its `cycleId`.
- Room schema advanced from v3 to v4 with `cycleId` on catalog snapshots and a bounded `game_quest_usages` table plus indexes.
- `RoomGameDiscoveryCatalogStore` now persists/reads cycle metadata and implements usage recording, lookup by cycle, and cycle cleanup.
- Usage remains independent from research provenance: clearing a usage cycle does not erase the permanent game discovery/provenance record.
- Existing 20-candidate capacity remains unchanged.
- Cache freshness authority was corrected: the persisted `GameCatalogSnapshot.expiresAtEpochMillis` is now the authority for deciding freshness; adaptive signals are used when calculating the expiry of a newly created snapshot.

### Lifecycle rule

`GameIdentity` survives cache rotation. `GameQuestUsageId` does not represent the game and may disappear with its candidate cycle.

A game used in cycle A can therefore return in cycle B. Different structured semantic quest variants also receive different usage IDs within the same cycle.

Because the current quest domain still does not carry a canonical game identity, P4.7 does not silently attach usage to generic `Quest` objects or infer identity from rendered text.

### Validation

- `:quest-core:test` — **BUILD SUCCESSFUL**.
- `GameQuestUsageTest` covers deterministic SHA-256 IDs, semantic-variant differentiation, cycle differentiation, normalization, and structured semantic axes.
- `:android-data:testDebugUnitTest :android-data:compileDebugKotlin` — **BUILD SUCCESSFUL**.
- Room KSP successfully generated schema v4 including `game_catalog_snapshots.cycleId` and `game_quest_usages`.
- `git diff --check` produced no whitespace errors; only the repository's existing LF/CRLF normalization warnings were reported.
- No commit or push performed.
- CR-9 remains **PAUSED / BLOCKED** and was not modified by this checkpoint.

### Next bounded P4 step

Connect `GameCandidateCycleId` and `GameQuestUsageStore` to the application-level game-quest generation boundary only after a canonical game identity contract exists there. The integration must record usage from structured game identity + semantic variant, never from UI text.

P4.7 status: **validated_bounded**.


## 31. P4.8 — canonical GAME quest identity boundary — 2026-10-05

### Scope

P4.8 connects the disposable candidate-cycle/usage model to the GAME quest planning boundary without putting research metadata or cache state into the generic `Quest` domain model.

### Implemented

- Added `GameQuestContext` as the canonical application/core boundary carrying a researched `GameDiscovery` plus its `GameCandidateCycleId`.
- Added `GameQuestCandidateSelector`, which selects only from the current bounded snapshot and excludes explicit usage identities for the same cycle.
- Selection returns the canonical `GameIdentityKey` together with the candidate cycle; no identity is inferred from rendered quest text.
- Added `GameQuestSeed` as the bridge into the existing quest planner. It carries `gameIdentity` and `cycleId` while preserving the generic `Quest` model unchanged.
- `GameQuestSeed.from(GameQuestContext)` is the single conversion boundary from research/catalog state into GAME quest planning state.
- Usage IDs remain derived from structured semantic quest variants rather than title/description text.
- Same game can re-enter through a new candidate cycle; exhaustion of a cycle yields no candidate instead of silently reusing a game.

### Validation

- `GameQuestCandidateSelectorTest` covers canonical identity propagation, same-cycle exclusion, exhausted-cycle behavior, usage-refresh pressure, and reintroduction in a new cycle.
- `GameQuestSeedTest` covers canonical identity/cycle propagation into quest planning.
- Full `:quest-core:test` — **BUILD SUCCESSFUL**.
- `:android-data:testDebugUnitTest :android-data:compileDebugKotlin` — **BUILD SUCCESSFUL**.
- No generic `Quest`/`QuestDraft` field was added for research/cache metadata.
- No inference fallback or CR-9 behavior was changed.
- No commit or push performed.

### Architectural rule

> Research identifies the game; the candidate cycle governs temporary availability; the GAME quest seed carries identity into planning; the generic quest remains unaware of cache mechanics.

P4.8 status: **validated_bounded**.

## 32. P4.9 — GAME candidate generation boundary — 2026-10-05

### Scope

P4.9 introduces the first application/core coordinator boundary for researched GAME candidates without inventing a game query source or contaminating the generic `Quest` domain with research/cache metadata.

### Implemented

- Added `GameQuestGenerationCoordinator`.
- `selectCandidate()` reads the persisted `GameCatalogSnapshot`, resolves usage for that exact `cycleId`, and delegates candidate selection to the canonical `GameQuestCandidateSelector`.
- The selected candidate crosses the research boundary as both `GameQuestContext` and `GameQuestSeed`.
- Exhausted candidate cycles remain explicit through `GameQuestCandidateResult.CycleExhausted`; the coordinator does not silently reuse a used game.
- `recordUsage()` accepts only a structured `GameQuestContext` + `GameQuestVariant` + timestamp and persists `GameQuestUsage` through `GameQuestUsageStore`.
- Usage identity remains derived from canonical game identity + candidate cycle + structured semantic variant; rendered quest title/description/objective text is not accepted as an input to the usage boundary.
- The accidental experimental `candidateContext` back-reference was removed from `GameQuestSeed`; the seed remains a one-way bridge into planning.
- No new fallback path was introduced and CR-9/JNI was not modified.

### Important boundary decision

The current product flow has a GAME button but no canonical game-query/input contract in the UI or request model. P4.9 therefore stops at the reusable application/core coordinator boundary instead of inventing a query such as a random hardcoded game. Research/refresh wiring into a user-triggered GAME request remains a separate bounded step.

Likewise, P4.9 does not fabricate `GameQuestVariant` from rendered quest text. The caller must provide the structured semantic variant before usage can be recorded.

### Validation

- `./gradlew.bat :quest-core:test :android-data:testDebugUnitTest :android-data:compileDebugKotlin --no-daemon` — **BUILD SUCCESSFUL**.
- `GameQuestGenerationCoordinatorTest` validates canonical candidate propagation, same-cycle usage exclusion, and structured usage recording.
- Existing P4 research/candidate tests remain green through the full core/data validation command.
- `git diff --check` — **PASS** earlier in this working tree; only existing LF/CRLF normalization warnings.
- No commit or push performed.
- CR-9 remains **PAUSED / BLOCKED** and was not modified.

P4.9 status: **validated_bounded**.

## 33. P4.10 — GAME research/query orchestration boundary — 2026-10-05

### Implemented

- Added canonical `GameResearchRequest(query, platform?)` as the explicit input contract for a user-triggered GAME research request.
- Added `GameResearchOrchestrator` to own cache decision, online/offline behavior, research execution, candidate-cycle creation, snapshot persistence, and stale preservation.
- Fresh cache is served without invoking research.
- Stale online cache requests refresh; a successful refresh creates a new `GameCandidateCycleId` and replaces the catalog through the existing store boundary.
- Refresh with no usable research result preserves the existing snapshot and returns `ServedStaleAfterRefreshFailure`.
- Offline with an existing catalog preserves the stale catalog; offline without a catalog produces `NoUsableCatalog`.
- Empty/invalid GAME query is rejected at the canonical request boundary.
- No provider-specific query is invented; the orchestrator receives the explicit query from its caller and passes it unchanged to `GameResearchCoordinator`.
- No raw provider payload is persisted.
- No fallback generation path was introduced and CR-9/JNI remains untouched.

### Validation

- `./gradlew.bat :quest-core:test :android-data:testDebugUnitTest :android-data:compileDebugKotlin --no-daemon` — **BUILD SUCCESSFUL in 19s**.
- 23 actionable tasks: 9 executed, 14 up-to-date.
- New `GameResearchOrchestratorTest` covers fresh-cache short circuit, successful stale refresh/new cycle, failed refresh preserving the existing snapshot, offline/no-cache behavior, and invalid query rejection.
- Existing P4 research/cache/candidate tests remained green.
- `git diff --check` — **PASS**; only known LF/CRLF normalization warnings were emitted.
- No commit or push performed.

P4.10 status: **validated_bounded**.

### Next bounded P4 step

Connect `GameResearchRequest` to the real application GAME action and then hand the resulting `GameCatalogSnapshot` to `GameQuestGenerationCoordinator`. This is the remaining application wiring boundary; it must preserve the explicit query contract, cache decisions, cycle semantics, stale preservation, and structured usage boundary without inferring intent from rendered quest text.


## 27. P4.11 — application GAME boundary — 2026-10-05

### Repository/UI asset synchronization

The official UI review assets were synchronized from origin/main commit 3323dcb (Imagens oficiais da UI) into the local worktree at:

docs/ScreenTemplatesOficial/

Synchronized files:
- Config.jpg
- Config2.jpg
- Home.jpg
- Home2.jpg
- Inicio.jpg
- JardimArte.jpg
- JardimColection.jpg
- JardimVazio.jpg
- NomeInicio.jpg
- Null
- Perfil.jpg
- Perfil2.jpg
- PinhoQuestIcon.jpg
- TagsInicio.jpg

The files are present locally and staged by the synchronization operation. No commit or push was performed.

### Implemented

Added GameQuestApplicationCoordinator as the application-facing GAME boundary.

Responsibilities:
- accepts an explicit GameResearchRequest;
- delegates cache/online/research semantics to GameResearchOrchestrator;
- delegates candidate identity/cycle/usage semantics to GameQuestGenerationCoordinator;
- exposes Selected, CycleExhausted, and NoUsableCatalog;
- does not derive research intent from rendered quest text;
- does not invent a query or introduce a fallback catalog.

Added GameQuestApplicationCoordinatorTest covering:
- fresh catalog -> canonical candidate selection;
- failed online refresh -> stale catalog preserved and still selectable;
- no catalog/offline -> NoUsableCatalog.

### Validation

- targeted P4.11 tests: PASS;
- full :quest-core:test: PASS;
- :android-data:testDebugUnitTest: PASS;
- :android-data:compileDebugKotlin: PASS;
- git diff --check: PASS (only existing LF/CRLF normalization warnings).

### Boundary deliberately left open

The existing UI GAME action (QuestMode.GAME) currently carries no canonical user/query input. Therefore P4.11 does not invent a query such as a hardcoded game name or generic search term.

The remaining application wiring boundary is:

UI GAME action -> canonical GameResearchRequest -> GameQuestApplicationCoordinator -> QuestContext/GameEngine

This requires a product-level query/source contract before the action can safely be wired end-to-end. The research provider, cache, cycle, identity and usage contracts remain intact.

CR-9 remains PAUSED / BLOCKED and untouched.


## 28. P4.11 — fechamento do GAME action — 2026-10-05

### Product contract closed

The GAME button is now explicitly defined as a cache-consumption action, not a research action.

Canonical behavior:

UI GAME action -> QuestSessionService.generate(QuestMode.GAME) -> GameQuestGenerationCoordinator.selectFreshRandomCandidate() -> QuestEngine

The button does not call GameResearchOrchestrator and does not perform network research.

### Candidate selection

- reads only the persisted GameCatalogSnapshot;
- requires the snapshot to be fresh according to its persisted expiresAtEpochMillis;
- consumes the bounded candidate pool of up to 20 games;
- excludes games already used in the same candidate cycle;
- randomly selects one remaining game;
- rejects a stale cache instead of silently researching or falling back.

### Quest semantic variation

The selected game receives a structured random GameQuestVariant before composition.
The variant contains a semantic activity and objective pattern and is carried through GameQuestSeed into the bounded model prompt.

The model therefore receives the canonical game name plus bounded game facts and a randomized semantic focus, allowing title/description/objectives to vary while remaining coherent with the selected game.

Rendered quest text is never used to determine identity or usage.

### Usage lifecycle

After a successful quest composition, the structured variant is recorded through GameQuestGenerationCoordinator.recordUsage().
Failed generation does not consume the game candidate.

### Validation

- :quest-core:test: PASS after adding fresh-cache, stale-cache, semantic-variant, and prompt-boundary coverage.
- :android-data:testDebugUnitTest: previously PASS and unchanged by this boundary.
- :android-data:compileDebugKotlin: previously PASS and unchanged by this boundary.
- :app:compileDebugKotlin: source path reached but the build remains blocked by the existing Kotlin metadata mismatch in cached kotlin-reflect:2.4.0 / LiteRT-LM 0.17.1 versus project compiler metadata 2.1.0. This is unrelated to the GAME contract and was not altered as a workaround.
- CR-9/JNI remains untouched.

### P4 status

The requested P4 application GAME gap is now closed. Research remains an independent catalog-refresh concern; pressing GAME never triggers it.

## 29. P4 Gate — CLOSED — 2026-10-05

### Gate decision

**P4 is CLOSED for the agreed P4 Web Research / Game Catalog scope.**

The gate is closed against the current product phase boundary: P4 establishes governed discovery, normalized game catalog behavior, provenance, persistence, semantic online/offline states, freshness and GAME consumption. Flower discovery/artwork expansion remains a P7 concern and is not required to advance P5 under the current roadmap.

### Gate evidence

| Criterion | Status | Evidence |
|---|---|---|
| Research boundary | PASS | GameResearchPort + governed ResearchOutcome states |
| Provider adapter | PASS | Wikipedia adapter with semantic response/error mapping |
| Normalization / deduplication | PASS | canonical GameDiscovery identity and catalog merge |
| Provenance / timestamp | PASS | persisted discovery provenance and research timestamps |
| Persistent catalog | PASS | Room snapshot/discovery storage, migrations 2→3→4 |
| Freshness | PASS | persisted expiresAtEpochMillis is canonical |
| Online refresh | PASS | GameResearchOrchestrator refreshes stale catalogs |
| Offline semantics | PASS | valid stale catalog is preserved; no destructive fallback |
| Empty catalog semantics | PASS | empty successful snapshot is distinguishable from no catalog |
| Candidate pool | PASS | bounded to up to 20 candidates |
| Game identity / usage | PASS | permanent GameIdentityKey + disposable semantic usage IDs |
| GAME selection | PASS | fresh-cache-only random candidate selection |
| GAME action boundary | PASS | GAME never triggers network research |
| Semantic variation | PASS | structured GameQuestVariant reaches bounded prompt facts |
| Usage lifecycle | PASS | usage recorded only after successful generation |
| Rendered-text isolation | PASS | rendered quest text never determines identity/usage |
| Validation | PASS | :quest-core:test, :android-data:testDebugUnitTest, :android-data:compileDebugKotlin successful |
| Visual authority | PASS | canonical visual contract and ScreenTemplatesOficial synchronized |

### Explicit non-blockers

- P3/CR-9 LiteRT-LM JNI remains PAUSED/BLOCKED and is not part of the P4 gate.
- :app:compileDebugKotlin remains affected by the previously documented Kotlin metadata mismatch in the cached LiteRT-LM dependency chain; no workaround was introduced for P4.
- P5 is the next active engineering frontier.
- P6 remains the detailed visual refinement phase.
- P7 remains the flower/garden expansion phase.

### Closure invariant

> **P4 discovers and governs content; P5 finishes the application.**
>
> Pressing GAME consumes a fresh governed catalog. It never performs research implicitly.

**Gate: PASS / P4 CLOSED.**

## 30. P5.1-P5.3 Foundation — implemented and validated — 2026-10-05

P5 is active. The first foundation slice now establishes a single governed backup/restore boundary without creating a second persistence authority.

### P5.1 — Backup contract

- `BackupManifest` defines format, Room schema, profile identity, dataset revision, app version, catalog versions and integrity hash.
- `BackupSnapshot` is the serializable state envelope for Room-backed product state plus the existing DataStore appearance preferences.
- `BackupCodec` owns ZIP (`manifest.json` + `snapshot.json`) encoding, SHA-256 integrity and invalid/corrupt/truncated input handling.
- `AppPreferencesStore.write()` is the canonical preference restore path.

### P5.2 — Snapshot/export foundation

- `BackupDao` reads the complete current Room entity set and provides bulk restore/clear operations.
- `RoomBackupSnapshotBuilder` builds a snapshot from Room as the authority and refuses ambiguous multi-profile state instead of silently dropping data.
- Snapshot creation is deterministic with respect to persisted state; transient runtime/rendered text is not used as an identity source.

### P5.3 — Restore/validation foundation

- `RestorePlanner` validates format/schema, profile identity, preferences, uniqueness and cross-entity references before mutation.
- `RoomRestoreService` accepts only validated backup state, restores Room transactionally, restores DataStore preferences, and rejects invalid input without touching current state.
- The P5.3 Android test proves successful replacement of Room + preferences and proves invalid backup preservation of the current state.

### Validation evidence

- `:quest-core:test` PASS, including `BackupCodecTest` and `RestorePlannerTest`.
- `:android-data:testDebugUnitTest` PASS.
- `:android-data:compileDebugKotlin` PASS.
- `:android-data:compileDebugAndroidTestKotlin` PASS.
- `:android-data:connectedDebugAndroidTest` PASS: 19 tests on Pixel_4_API_33 / Android 13.
- P4 Android-test/schema alignment was repaired separately before this P5 slice: game catalog cycle identity and migration schema/index parity now match the current Room model.

### Boundary status

P5.1-P5.3 foundation is validated. SAF destination UX/export action, user-facing settings surface, accessibility/error presentation and full V1 E2E closure remain in the later P5 checkpoints.

> **Room remains the authority. Backup is a snapshot. Restore is a validated transaction boundary.**
## 31. P5.4-P5.5 Settings and UX Reliability — 2026-10-05

P5.4/P5.5 advanced the application-facing layer without changing Room as domain authority or inventing a second settings store.

### P5.4 — Settings / preferences

- Theme remains canonical through AppPreferencesStore: System / Light / Dark.
- Font scale remains canonical through AppPreferencesStore and is applied through Compose Density.
- Preference writes reject non-positive font scales.
- Settings copy now explains appearance choices and persistence without exposing implementation details or future-work promises.
- Added persistence/validation tests for theme and font-scale preferences.

### P5.5 — Accessibility and humanized errors

- Bottom-navigation emoji decoration no longer contributes duplicate screen-reader semantics; destination labels remain the spoken navigation authority.
- Quest generation loading state now exposes an explicit spoken progress description.
- Existing main screens use scrollable layouts and no fixed-height text containers were found in the current UI tree.
- Added semantic backup/restore copy mappings that preserve warm, truthful language and explicitly reassure that local progress remains safe when validation fails.
- Added V1 user-facing copy audit at docs/evidence/v1-copy-audit.md.

### Validation

- `:android-data:testDebugUnitTest` PASS.
- `:quest-core:test` PASS.
- `:app:compileDebugKotlin` remains BLOCKED by the pre-existing LiteRT-LM 0.17.1 Kotlin metadata mismatch: dependency metadata 2.4.0, project compiler metadata 2.1.0. Forcing kotlin-reflect 2.1.21 does not resolve it; that experimental change was reverted.
- Full app UI/instrumentation accessibility tests therefore remain pending until the compile boundary is resolved. No runtime bypass, parser fallback or device-specific workaround was introduced.

> **P5.4/P5.5 improves the application's human-facing boundary; it does not weaken the runtime gates.**

## 32. P5 pause and CR-9 JNI/LiteRT-LM investigation — 2026-10-05

P5 is intentionally paused while the productive LiteRT-LM Android integration is investigated. P5.5 is PAUSED and P5.6 is WAITING/PAUSED. No P5 work is being used to bypass CR-9.

### P5 state

- P5.1-P5.4 foundation/application work remains implemented and validated at its existing scope.
- P5.5 remains implemented at the human-facing layer, but final app UI/instrumentation validation is paused behind the LiteRT-LM/app compile boundary.
- P5.6 and the P5 Gate are waiting for CR-9 to close.

### CR-9 investigation facts

- LiteRT-LM dependency is 0.17.1.
- Resolved AAR contains arm64-v8a and x86_64 liblitertlm_jni.so.
- The arm64-v8a library exports Java_com_google_ai_edge_litertlm_NativeLibraryLoader_nativeCheckLoaded.
- The productive APK also contains the expected JNI libraries.
- The app compile classpath currently resolves kotlin-reflect 2.4.0 through LiteRT-LM 0.17.1 while the project compiler is Kotlin 2.1.21.
- Therefore the Kotlin metadata mismatch is a dependency/toolchain boundary problem, not evidence that the .litertlm model is incompatible with Kotlin.
- CR-8 already proved the canonical CR-7.4 model can execute through the validated native LiteRT-LM path with 5/5 native ToolCalls.

### Current hypotheses

1. The app should probably not expose LiteRT-LM's Kotlin dependency graph on its Kotlin CompileClasspath merely to package the native runtime; a controlled implementation-to-runtimeOnly experiment is the next build-boundary test.
2. The productive JNI failure still requires complete loader/classloader/linker evidence. Presence of the .so and exported symbol rules out a simple missing-file/missing-symbol diagnosis.
3. nativeCheckLoaded may be an initial probe failure caught by LiteRT-LM's own loader rather than the final causal failure; the complete load sequence must be captured before changing code.

### Investigation document

Detailed evidence and the controlled experiment plan are recorded at docs/evidence/cr9-jni-litertlm-android-diagnosis.md.

### Refactor policy

No model change, global Kotlin upgrade, arbitrary System.loadLibrary call, semantic fallback, parser bypass or device-specific workaround is approved at this checkpoint. Any dependency/version refactor must follow measured evidence from the compile boundary and native loader sequence.

> CR-9 is the current engineering frontier. P5 waits for the runtime boundary; it does not weaken it.


## 33. CR-9 investigation checkpoint — dependency boundary and JNI loader — 2026-10-05

The controlled dependency-boundary experiment produced the first strong correction candidate.

### Build boundary result

The app direct LiteRT-LM dependency was changed from implementation to runtimeOnly while litertlm-bridge retained compileOnly(litertlm-android).

Evidence:
- kotlin-reflect 2.4.0 disappeared from app debugCompileClasspath.
- :app:compileDebugKotlin PASS.
- :app:assembleDebug PASS.
- APK still contains arm64-v8a and x86_64 liblitertlm_jni.so.

### JNI loader result

A temporary instrumentation probe executed LiteRT-LM NativeLibraryLoader.load() inside the productive APK and PASSed.

The log still contains the initial nativeCheckLoaded No implementation found message. This is expected from LiteRT-LM's already-loaded probe: its loader catches that UnsatisfiedLinkError and then attempts System.loadLibrary("litertlm_jni"). The dedicated loader probe succeeding means that message is not, by itself, a JNI failure.

This materially narrows CR-9: there is no current evidence that LiteRT-LM 0.17.1 lacks the JNI symbol or cannot load its native library in the productive APK.

### What remains open

The productive UI E2E with the canonical model progressed through the UI but failed the historical native-memory assertion at approximately 130–134 MB PSS. This does not prove native inference failure because admission/resource governance may prevent full model residency or converge to procedural generation.

CR-9 therefore remains open. The next proof must observe the actual production inference outcome and native ToolCall, not only loader initialization.

### Version decision

Current evidence supports keeping Kotlin 2.1.21, LiteRT-LM 0.17.1 and the canonical CR-7.4 model. No model swap or global Kotlin upgrade is justified. No LiteRT-LM version change is currently indicated.

Detailed evidence: docs/evidence/cr9-jni-litertlm-android-diagnosis.md.


## 34. CR-9 decisive resolution — LiteRT-LM filename contract — 2026-10-05

The CR-9 causal failure is now isolated and corrected.

### Decisive A/B

The exact same canonical CR-7.4 bytes were exercised through the same productive APK, LiteRT-LM 0.17.1 and x86_64 Android runtime.

- Path ending in /model: Engine.initialize() returned INVALID_ARGUMENT: Unsupported or unknown file format.
- Path ending in /model.litertlm: the exact same SHA-256 e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb initialized and produced the canonical native ToolCall; P3NativeToolCallE2ETest passed.

This rules out missing JNI symbol, ABI mismatch, model-byte corruption, Kotlin metadata, model semantics and arbitrary classloader failure as the primary cause.

### Root cause

AndroidModelStore stored LiteRT-LM artifacts under the extensionless filename model. LiteRT-LM 0.17.1 format detection requires the .litertlm filename in this serving path.

### Correction

AndroidModelStore now stores LiteRT-LM artifacts as model.litertlm and derives the filename from manifest.runtimeFormat. Existing legacy model files are migrated to model.litertlm before active() returns the model.

This is a storage-contract correction. It is not a runtime fallback, parser repair or device-specific bypass.

### Validation

- :android-data:testDebugUnitTest PASS.
- :quest-core:test PASS.
- :app:compileDebugKotlin PASS.
- :app:assembleDebug PASS.
- :app:assembleDebugAndroidTest PASS.
- Direct native ToolCall test with model.litertlm PASS.
- Productive CR9ProductionModelE2ETest PASS (OK, 1 test).

### Gate

CR-9: technically resolved, production E2E validation pending.

The earlier JNI/classloader diagnosis is superseded by this filename/format-detection finding. The loader's initial nativeCheckLoaded No implementation found message is an expected first-load probe and was not the terminal failure.

### 35. P3-CR-9 / P5.5 — Production Model E2E Gate — 2026-10-05

This checkpoint deliberately couples the evidence collection for CR-9 and P5.5 while preserving separate gate criteria.

#### Build and artifact preconditions

- `:android-data:testDebugUnitTest` — PASS.
- `:quest-core:test` — PASS.
- `:app:compileDebugKotlin` — PASS.
- `:app:assembleDebug` — PASS.
- `:app:assembleDebugAndroidTest` — PASS.
- Connected device: `emulator-5554`.
- Canonical CR-7.4 model SHA-256 remains `e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb`.
- The installed model is an external artifact, not packaged inside the APK. Its canonical runtime filename is `model.litertlm`.

#### Productive E2E attempt

The debug APK was installed successfully. The canonical CR-7.4 model was then staged and verified by size/SHA and promoted into the canonical `AndroidModelStore`, producing the active `.../1/model.litertlm` path.

The first fresh productive instrumentation run with `clearPackageData=false` launched `MainActivity`, initialized the LiteRT environment, initialized the incoming `.litertlm` model and began building the XNNPACK weight cache. The emulator then had only ~114–118 MB free; XNNPACK reported `cannot append buffer to cache file` / `Inserting data in the cache failed`, followed by `Fatal signal 6 (SIGABRT)` in `liblitertlm_jni.so`. The tombstone entered through `LiteRtLmJni_nativeCreateEngine` -> `Engine.initialize()`.

After removing duplicate model copies and trimming emulator caches, storage increased to ~1.1 GB free. The exact same active model and bridge then passed `P3NativeToolCallE2ETest` with `1 tests, 0 failed, 0 ignored`. The productive `CR9ProductionModelE2ETest#productiveUiGeneratesWithCanonicalCr74Model` was then rebuilt with a 30-second generation wait and completed with `1 tests, 0 failed, 0 ignored` and no native abort. The full AppGraph/UI path therefore passed after storage recovery.

#### Gate decision

- **CR-9:** `E2E_PASS` — canonical `.litertlm` storage contract is corrected and the productive AppGraph/UI generation path completed successfully.
- **CR-9.1:** `DIAGNOSIS_PASS / CAUSE_IDENTIFIED` — the earlier SIGABRT was caused by emulator storage exhaustion during XNNPACK weight-cache construction (`cannot append buffer to cache file`). With ~1.1 GB free, the same APK/model passes both isolated native ToolCall and productive UI E2E.
- **P5.5:** `IMPLEMENTED / CR9_UNBLOCKED` — the CR-9 dependency is now satisfied; P5.5 may proceed to its own final gate.
- **P5.6:** no longer blocked by CR-9; its remaining dependencies follow the P5 roadmap.

#### Required closure evidence

The next execution must install the canonical CR-7.4 `.litertlm` artifact through the normal model-store contract, then execute the productive AppGraph quest flow and capture evidence that:

`model.litertlm -> LiteRT-LM -> Message.toolCalls -> MicroQuestToolCallDecoder -> MicroQuestText -> QuestValidator -> Quest`.

The proof must also distinguish `LOCAL_MODEL` from `PROCEDURAL_FALLBACK`, and must not use PSS/memory alone as evidence of semantic success. Multiple quest generations are preferred over a single lucky sample.

> **Shared evidence, separate gates: CR-9 closes the runtime path; P5.5 closes the human-facing validation once that runtime path is proven.**

Detailed evidence: docs/evidence/cr9-jni-litertlm-android-diagnosis.md.
