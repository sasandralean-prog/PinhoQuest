# Pinho Quest — CURRENT STATE

Date: 2026-10-03
Branch: feature/cr-0-runtime-consolidation
CR frontier: CR-3 — Native Android Tool Transport (validated_bounded: build/bridge scope)
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

Next frontier: CR-4 — RAW isolation and output-protocol convergence.