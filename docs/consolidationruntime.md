# Consolidation Runtime — Pinho Quest

Status: CR-7 implemented_unvalidated (native matrix compiled but semantic device gate remains blocked by missing canonical native prompt/probe); CR-4/CR-5/CR-6 validated_bounded
Date: 2026-10-03
Branch: feature/cr-0-runtime-consolidation
Baseline: f17ae65
Scope: P3 FunctionGemma runtime, contracts, prompt boundary, budgets and validation

## 0. Purpose

This document defines the Runtime Consolidation (CR) sprint family.

The goal is to converge P3 onto one governed FunctionGemma tool-calling protocol before any production wiring or expensive model re-training/conversion.

The central rule is:

> One semantic LLM protocol has one source of truth. Training data, prompt construction, Android tool registration, decoders and tests are representations/adapters of that contract, never independent contracts.

The consolidation is deliberately staged. Each CR sprint has one primary responsibility, explicit gates and a stop condition.

## 1. Why this work exists

The investigation at f17ae65 found that the P3 model was trained for native FunctionGemma tool calling, while parts of the runtime were still shaped like a generic structured-JSON generator.

That divergence can explain:

- incomplete tool arguments;
- unexpected continuation after a tool call;
- parser fallback behavior;
- semantic differences between host and Android;
- unnecessary context/output capacity;
- RAM and latency pressure;
- difficulty determining whether a failure belongs to the model, adapter or conversion.

The correct response is not to keep increasing the prompt, output budget or training data until the model happens to pass.

The correct response is to make the protocol explicit and singular.

## 2. Observed architecture

### Production

QuestRequest
-> QuestSessionService
-> QuestContextProvider
-> QuestPlanner
-> QuestGenerationPlan
-> ProceduralComposer
-> QuestValidator
-> Room

FunctionGemma is not currently in production.

### Experimental P3

QuestGenerationPlan
+ optional tags/examples
-> MicroQuestCompositionRequest
-> MicroQuestPromptSerializer
-> GenerationRequest(String, maxOutputTokens=128)
-> LocalInferencePort
-> AndroidLiteRtLmInferencePort
-> raw text
-> FunctionGemmaToolCallExtractor
-> generic JSON sanitizer fallback
-> MicroQuestRenderer
-> QuestValidator

The experimental path is the target of CR.

## 3. Diagnostic findings

### CR finding A — Tool registration mismatch

Training target:
native FunctionGemma function call.

Historical Android adapter:
tools = emptyList().

Result:
the runtime did not expose compose_quest_text as a registered tool even though the model was trained around that protocol.

Correction completed in CR-3:
the bridge registers exactly one canonical tool and uses manual tool calling with native Message.toolCalls.

### CR finding B — Structured JSON vs native function calling

Historical Android path:
ResponseFormat.json(textSchema()).

Training path:
native FunctionGemma call syntax.

Result:
the runtime asked for a different output protocol.

Correction completed in CR-3:
generic ResponseFormat.json was removed from the FunctionGemma P3 transport path. Structured JSON remains a separate protocol outside this native path.

### CR finding C — Runtime prompt is not equivalent to training prompt

MicroQuestPromptSerializer says:
TAREFA=compose_quest_text
and
RETORNO=chame a funcao compose_quest_text...

But it does not construct the native FunctionGemma developer/tool declaration path.

Solution:
CR-1 defines the canonical tool contract.
CR-2 defines the bounded facts.
CR-3 constructs the native runtime conversation from those contracts.

### CR finding D — Multiple output protocols

MicroQuestComposer currently tries:
1. native FunctionGemmaToolCallExtractor;
2. generic JSON sanitizer;
3. procedural fallback.

Result:
one P3 model can effectively speak two protocols, and adapter errors can be masked.

Solution:
CR-4 makes native tool calling the only P3 model protocol. Invalid native call becomes a governed failure and may converge to the procedural fallback.

### CR finding E — Three-field vs six-field contract collision

P3 creative tool:
title
description
objectives

P3-5A/QuestDraftCodec structured draft:
title
description
objectives
bonusObjectives
estimatedMinutes
estimatedDifficulty

These can coexist only if their ownership is explicit.

Solution:
P3 FunctionGemma owns only MicroQuestText's three creative fields.
Deterministic QuestGenerationPlan owns category, environment, duration and difficulty.
The six-field codec must not be silently treated as the FunctionGemma P3 tool schema.

### CR finding F — P3-5A governance boundary is disconnected

Existing good path:
QuestGenerationPlan + approved facts
-> PromptFactsAssembler
-> BoundedPromptEnvelope
-> CompactPromptSerializer
-> LocalInferencePort.

P3 MicroQuestComposer currently bypasses that boundary.

Solution:
CR-2 connects P3 to the canonical bounded-input mechanism or consolidates it into one equivalent boundary without creating another parallel path.

### CR finding G — Input sanitization is incomplete in the P3 request

MicroQuestCompositionRequest limits:
tags <= 6
examples <= 3.

But individual values are not fully sanitized/bounded before prompt construction.

Solution:
CR-2 sanitizes and bounds every model-facing tag/example/fact before serialization. Combined prompt length is a final guard, not the primary sanitizer.

### CR finding H — RAW crosses the core inference abstraction

LocalInferencePort now exposes a typed native ToolCall outcome for the corrected path.

CR-3 keeps the legacy Success(String) outcome temporarily so CR-4 can remove the second output protocol without conflating transport correction with output convergence.

RAW, when unavoidable inside the runtime, is not used by the new native tool transport.

### CR finding I — Budget fragmentation

Current limits include:
- MicroQuestPromptSerializer: 1200 characters;
- GenerationRequest: 128 output tokens;
- Android EngineConfig: 1280 max tokens;
- PromptContractLimits: 1400 characters;
- conversion: prefill 32 and cache 256.

These values are not derived from one budget policy.

Solution:
CR-5 introduces one canonical inference budget with prompt/context/output/tool-call ceilings and explicit adapter enforcement.

Character limits remain useful admission guards, but they do not replace token-aware budgeting.

### CR finding J — Training dataset declaration duplication

The pilot dataset contains a duplicated declaration-start marker.

Solution:
CR-6 regenerates training data from the canonical contract/template path and adds a test that rejects duplicated declaration markers.

### CR finding K — Stop boundary must be validated at runtime

SFT completions containing <start_function_response> are not automatically evidence of a training error; FunctionGemma documentation treats this marker as an inference stop sequence.

The important runtime question is whether the adapter obtains a complete Message.toolCalls result and stops at the native call boundary.

Solution:
CR-3 and CR-7 validate native transport before another fine-tuning intervention.

## 4. Canonical target architecture

QuestRequest
-> QuestContextProvider
-> QuestPlanner
-> QuestGenerationPlan
-> PromptFactsAssembler
-> BoundedPromptEnvelope
-> Canonical FunctionGemma Tool Contract
-> native LiteRT-LM ConversationConfig
-> Message.toolCalls
-> strict typed decoder
-> MicroQuestText
-> MicroQuestRenderer
-> QuestValidator
-> Quest

### Authority split

Deterministic authority:
- category;
- environment;
- difficulty;
- duration;
- game candidate;
- tag policy;
- approved facts;
- validation;
- persistence.

FunctionGemma creative authority:
- title;
- description;
- objectives.

No LLM-generated category/environment/difficulty/duration is accepted as domain truth.

## 5. Canonical contract model

The core contract should conceptually be one object:

MicroQuestToolContract

It owns:
- tool name: compose_quest_text;
- tool description;
- argument names;
- argument types;
- required/optional status;
- bounds;
- semantic ownership.

Canonical arguments:

title: String, 1..80
description: String, 12..220
objectives: List<String>, 1..4, each 1..120

The exact Kotlin/API representation may differ by adapter, but semantic values must not diverge.

Representations derived from the contract:

MicroQuestToolContract
-> Kotlin LiteRT-LM tool registration
-> FunctionGemma declaration
-> training examples
-> decoder validation
-> contract tests

This is not duplication if every representation is generated/verified against one semantic source.

## 6. Canonical input boundary

The model must receive only a bounded envelope.

Conceptual flow:

QuestGenerationPlan
+
approved creative facts
-> PromptFactsAssembler
-> BoundedPromptEnvelope
-> canonical FunctionGemma conversation

Allowed:
- selected category;
- selected environment;
- trusted difficulty/time facts;
- bounded approved tag labels when creatively useful;
- bounded approved fact/objective seeds;
- style/language policy.

Forbidden:
- Room entities;
- raw database objects;
- raw profile/history;
- affinity internals;
- raw web pages;
- URLs/HTML;
- unbounded research payloads;
- secrets;
- diagnostic internals;
- arbitrary caller text.

No raw input is allowed merely because the combined prompt is below a character limit.

## 7. Canonical output boundary

The only accepted P3 model output is:

native tool call
name == compose_quest_text
with exactly the canonical arguments.

Then:

Message.toolCalls
-> strict decoder
-> MicroQuestText
-> renderer

Any other output is invalid for this protocol.

Do not silently reinterpret:
- generic JSON;
- free-form prose;
- partial JSON;
- second tool calls;
- arbitrary text tails.

Invalid output may trigger the existing governed procedural fallback, but fallback must return through QuestValidator and the same domain authority.

## 8. RAW isolation rule

RAW model output must not reach:
- UI;
- renderer;
- Room;
- Quest;
- reward/progression;
- logging that exposes user data unnecessarily.

The adapter may observe raw runtime data for diagnostics under bounded policy.

The core should receive typed outcomes, for example:

InferenceOutcome.ToolCall
or
InferenceOutcome.Failure

The exact type name is implementation detail; the boundary rule is not.

## 9. Budget model

The canonical budget must cover:

1. prompt admission;
2. model context;
3. generated output;
4. number of tool calls;
5. fallback work;
6. adapter/runtime limits.

P3 invariant:
maxToolCalls = 1.

The system must distinguish:
- requested budget;
- actual usage;
- hard ceiling;
- stop reason.

A character count is not a token budget. It may remain as a cheap admission guard, but token/context limits must be governed separately.

No CR sprint may increase a budget merely to make a test pass without evidence that the contract requires it.

## 10. Tag contract

Current deterministic path:

Room tags
-> SystemTagCatalog.categoryAffinities()
-> QuestPlanner
-> QuestGenerationPlan

This remains authoritative.

If tag labels are later included for wording:

approved tag labels
-> sanitizer/bounds
-> BoundedPromptEnvelope
-> FunctionGemma

The model does not decide:
- tag ownership;
- category;
- affinity;
- reward;
- progression.

## 11. Sprint plan

### CR-0 — Planning and documentation

Objective:
freeze the diagnosis and define the convergence plan.

Scope:
- current state;
- contract inventory;
- problem/solution map;
- sprint order;
- invariants;
- gates;
- no-go rules.

Allowed:
documentation only.

Forbidden:
runtime behavior changes, model retraining, conversion, production wiring.

Exit gate:
- docs committed;
- branch pushed;
- current state updated;
- no runtime code changed.

### CR-1 — Canonical FunctionGemma tool contract

Objective:
create one semantic source of truth for compose_quest_text.

Work:
- canonical tool name;
- arguments;
- types;
- bounds;
- description;
- ownership;
- tests;
- explicit separation from six-field QuestDraftCodec.

Gate:
all representations can be checked against one contract.

Stop:
if two components still need independent semantic definitions.

### CR-2 — Bounded prompt/input governance

Objective:
make the P3 model-facing input path governed.

Work:
- PromptFactsAssembler integration;
- bounded envelope;
- tag/example sanitization;
- allowlist;
- dedupe;
- character/token admission;
- no raw domain/research payloads.

Gate:
tests prove untrusted/oversized/control-character input cannot cross the model boundary.

Stop:
if P3 needs a second prompt-governance path.

### CR-3 — Native Android tool transport

Objective:
make Android speak the same native FunctionGemma protocol as training.

Work:
- register compose_quest_text;
- automaticToolCalling=false;
- inspect Message.toolCalls;
- remove generic JSON response format from this path;
- typed adapter result.

Gate:
a small native Android probe returns exactly one valid tool call with all required arguments.

Stop:
if the runtime still requires raw-text recovery to understand the tool call.

### CR-4 — RAW isolation/output convergence

Objective:
make native tool call the only P3 output protocol.

Work:
- strict decoder;
- remove silent generic JSON fallback;
- raw isolation;
- governed failure;
- procedural fallback convergence.

Gate:
static/integration tests prove renderer and UI cannot receive raw model output.

Stop:
if any path can render a model String directly.

### CR-5 — Canonical budget governance

Objective:
eliminate budget inflation and disconnected limits.

Work:
- InferenceBudget;
- context/prompt/output/tool-call ceilings;
- adapter enforcement;
- telemetry for requested vs actual usage;
- explicit stop reason.

Gate:
one P3 budget policy governs every inference layer.

Stop:
if a lower layer silently expands a higher-level budget.

### CR-6 — Training/runtime contract regeneration

Objective:
make SFT data structurally equivalent to runtime.

Work:
- canonical tool declaration;
- canonical template;
- remove duplicated declaration marker;
- validate completion shape;
- preserve correct stop boundary semantics.

Gate:
training samples and runtime declaration are semantically equivalent.

Stop:
if dataset generation still manually duplicates protocol fragments.

### CR-7 — Native hardening validation

Objective:
validate the corrected protocol before model/conversion conclusions.

Work:
- small native tool-call matrix;
- one-call invariant;
- complete required arguments;
- bounds;
- malformed call rejection;
- latency;
- failure semantics.

Gate:
repeatable native success under bounded runtime.

Stop:
if failures cannot be localized to model vs adapter vs conversion.

### CR-8 — Mobile conversion/quantization A/B

Objective:
measure conversion as an isolated variable.

Work:
- same canonical prompt;
- same canonical tool schema;
- same adapter;
- multiple artifacts only as needed;
- PSS/RAM;
- latency;
- completion;
- termination.

Gate:
conversion candidate passes semantic and resource gates.

Stop:
LOW_MEMORY is recorded as capacity evidence; do not misclassify it as semantic regression.

### CR-9 — Production integration

Objective:
wire only the validated implementation into production.

Work:
- PinhoQuestAppGraph integration;
- LocalLlmComposer as creative composer;
- ProceduralComposer fallback;
- end-to-end UI flow;
- Room persistence;
- validator.

Gate:
full user-visible quest generation passes without bypassing authority boundaries.

## 12. Gate vocabulary

planned:
documented, not implemented.

implemented_unvalidated:
code exists, required evidence missing.

validated_bounded:
required tests/evidence pass within the stated scope and limits.

blocked:
a prerequisite or environment prevents valid evidence.

rejected:
the approach violates a contract or fails required evidence.

Production must not consume planned, implemented_unvalidated or blocked artifacts.

## 13. No-go rules

Do not:
- add regex repair to hide a protocol mismatch;
- add generic JSON fallback to a native FunctionGemma path;
- increase output/context budget without a contract reason;
- feed raw tags/examples/research to the model;
- let model output become domain truth;
- wire FunctionGemma into production before CR gates;
- repeat large Android conversion tests before native contract correctness;
- call an inconclusive LOW_MEMORY run a semantic failure;
- call a model failure before adapter/tool transport has been validated.

## 14. Evidence and provenance

The CR program inherits the investigation evidence recorded in:

docs/evidence/p3-toolcalling-contract-diagnosis.md

Important evidence:
- native LiteRT-LM tool transport works in isolation;
- quantized pilot120 native call can omit objectives;
- FP A/B was inconclusive due to LOW_MEMORY;
- constrained decoding was too slow and incomplete;
- host hardening does not substitute for native Message.toolCalls validation.

External protocol reference:
Google FunctionGemma formatting/function-calling documentation.
LiteRT-LM Kotlin tool-calling/configuration documentation.

## 15. CR-0 exit record

CR-0 is complete only when:
- CURRENT_STATE.md records this frontier;
- this document is committed;
- branch is pushed;
- no runtime code was changed;
- CR-1 is explicitly the next frontier.

CR-0 is intentionally a planning checkpoint, not a runtime validation checkpoint.


## 16. CR-1 / CR-2 implementation record

### CR-1 — canonical FunctionGemma tool contract

Status: implemented; quest-core validation passed.

Implemented artifacts:
- `quest-core/.../micro/MicroQuestToolContract.kt`
- `MicroQuestToolCallExtractor` now consumes canonical name, required arguments and bounds.
- `FunctionGemmaResponseSanitizer` references the same canonical bounds as a compatibility path; it is still scheduled for removal from the P3 runtime in CR-4.
- `MicroQuestToolContractTest` characterizes the exact three-field protocol and its ownership boundary.

Canonical contract:
- tool: `compose_quest_text`
- `title`: String, 1..80
- `description`: String, 12..220
- `objectives`: List<String>, 1..4, each 1..120

The contract test explicitly verifies that category, environment, duration, difficulty, bonus objectives, XP and rewards are not tool arguments.

The six-field `QuestPromptContract` remains a separate structured-draft protocol. It is not silently promoted to the FunctionGemma P3 schema.

### CR-2 — bounded prompt/input governance

Status: implemented; quest-core validation passed.

Implemented artifacts:
- `PromptFactsAssembler.assembleTagLabels()` reuses the existing deterministic sanitization/bounding rules for P3 tag labels.
- `MicroQuestPromptFactsAssembler` is the P3 input-governance boundary.
- `BoundedMicroQuestPrompt` carries only the bounded envelope and sanitized approved examples.
- `MicroQuestCompositionRequest` no longer stores raw tags/examples; it accepts only the bounded prompt object.
- `MicroQuestPromptSerializer` serializes only bounded facts and approved examples.

Governance now proves:
- control characters are normalized before serialization;
- tags are deduplicated, sorted and capped;
- examples are normalized, field-bounded and capped at three;
- example objectives are capped at four and each objective is bounded;
- the serializer never receives the original raw collections.

The P3 path still does not consume raw research/web payloads. If research facts are introduced later, they must enter through the typed `PromptResearchHint` allowlist and the existing `PromptFactsAssembler` path.

### Validation gate

Command:
`gradlew :quest-core:test --no-daemon --console=plain`

Result:
`BUILD SUCCESSFUL`

Scope:
- quest-core only;
- no Android runtime transport changes;
- no model retraining;
- no model conversion;
- no production FunctionGemma wiring.

### Remaining boundary

CR-1/CR-2 are committed and pushed as `266ee73` and promoted to `validated_bounded` for the quest-core scope.

CR-3 is the next implementation frontier: native LiteRT-LM tool registration, manual tool calling and typed `Message.toolCalls` transport.

CR-4 remains responsible for removing the generic JSON compatibility path from the P3 runtime. The current sanitizer remains present only so CR-1/CR-2 do not silently broaden the scope into output-protocol convergence.

## 17. CR-3 implementation record

Status: validated_bounded for source/build/bridge scope.

### Runtime transport

Implemented:
- `:litertlm-bridge` is the only project module that imports the LiteRT-LM API.
- `P3LiteRtToolSet` registers exactly one native tool: `compose_quest_text`.
- Tool description and parameter descriptions come from `MicroQuestToolContract`.
- `automaticToolCalling=false` remains enabled so the adapter consumes the model's native tool-call message explicitly.
- `LiteRtToolCallMapper` requires exactly one `Message.toolCalls` entry, the canonical tool name and the exact canonical argument set.
- Unexpected names, zero/multiple calls and missing/extra arguments converge to `InferenceOutcome.InvalidOutput`.
- `InferenceOutcome.ToolCall` is now available at the core boundary.
- `AndroidLiteRtLmInferencePort` no longer imports LiteRT-LM and only delegates to the bridge.
- `ResponseFormat.json(...)` and the P3 JSON schema were removed from the Android FunctionGemma transport path.

### Toolchain boundary

LiteRT-LM 0.17.1 is compiled with newer Kotlin metadata than the project's Kotlin 2.1.21 compiler. Rather than upgrading the whole application toolchain inside CR-3, the dependency is isolated:
- bridge Java source compiles against LiteRT-LM with JDK 21 while emitting Java 17-compatible bytecode;
- bridge compile-only dependency prevents the native API from becoming a Kotlin source dependency of the app;
- app owns the LiteRT runtime dependency for APK packaging;
- app `*CompileClasspath` configurations force the project Kotlin stdlib 2.1.21 so runtime-only Kotlin 2.4 metadata does not enter source compilation.

This is a build compatibility boundary. It does not bypass the native FunctionGemma protocol and does not alter model behavior.

### Tests and evidence

Passed:
- `:litertlm-bridge:test`
- `:app:compileDebugKotlin`
- `:app:testDebugUnitTest`
- `:app:assembleDebug`

APK evidence:
- debug APK size: 60,983,037 bytes;
- `lib/arm64-v8a/liblitertlm_jni.so` packaged;
- `lib/x86_64/liblitertlm_jni.so` packaged.

### Scope boundary

CR-3 does not claim that the selected FunctionGemma artifact is semantically correct on-device through this new bridge. The earlier isolated native probe already established that LiteRT-LM can return native tool calls, but CR-7 owns the repeatable native matrix for this corrected implementation.

CR-4 remains responsible for deleting the legacy `InferenceOutcome.Success(String)`/generic JSON compatibility path from P3 and proving RAW isolation end-to-end.

Next frontier: CR-4 — RAW isolation and output convergence.
## 18. CR-4 implementation record

Status: validated_bounded for P3 core output convergence.

### Output convergence

CR-4 removed the second output protocol from P3.

Before:
- native FunctionGemma call extraction;
- generic JSON sanitization;
- procedural fallback.

After:
- native LiteRT-LM Message.toolCalls;
- exact canonical tool name and argument set;
- typed MicroQuestToolCallDecoder;
- MicroQuestText;
- MicroQuestRenderer;
- QuestValidator;
- governed procedural fallback on invalid or non-tool outcomes.

### RAW isolation

InferenceOutcome.Success(String) was removed. The core inference boundary no longer carries raw model text.

The P3 composer no longer depends on a text parser or JSON sanitizer. The renderer receives only MicroQuestText produced by the native tool-call decoder.

The legacy raw parser/sanitizer sources and tests were removed so the second protocol cannot be silently revived through an unused compatibility class.

### Validation gate

Passed:
gradlew :quest-core:test --no-daemon --console=plain

The test suite covers valid native convergence, invalid native arguments to procedural fallback, non-tool outcomes to fallback, typed argument rejection, objective count and description bounds, and unexpected tool-name rejection.

Static source verification found no runtime/test references to the removed raw output path.

### CR-4 gate decision

validated_bounded for the P3 core output boundary.

Not yet claimed:
- native Android semantic hardening of the corrected end-to-end path;
- model artifact correctness;
- production integration.

Those remain owned by CR-7 through CR-9.

Next frontier: CR-5 — canonical inference budget governance.

## 19. CR-5 implementation record

Status: validated_bounded for canonical budget enforcement and build integration.

### Canonical policy

CR-5 centralizes the P3 budget in InferenceBudget. The current P3 policy preserves previously observed bounded values instead of increasing capacity:

- maxPromptCharacters = 1200
- maxContextTokens = 1280
- maxOutputTokens = 128
- maxToolCalls = 1

GenerationRequest now carries this budget, and prompt admission is rejected before the LocalInferencePort is invoked when the prompt exceeds the configured character ceiling.

MicroQuestPromptSerializer, MicroQuestComposer and the Android LiteRT-LM bridge consume the same budget object. The bridge configures EngineConfig from maxContextTokens and sendMessage from maxOutputTokens. It also requires request.budget to equal the runtime budget, preventing silent lower-layer expansion or substitution.

### Telemetry boundary

CR-5 adds a typed InferenceTelemetry event and injectable InferenceTelemetrySink. The runtime records requested limits, observed native tool-call count, stop reason and Conversation.getTokenCount() before/after values. The token delta is explicitly named an observed conversation-token delta; it is not mislabeled as exact output-token usage.

LiteRT-LM 0.17.1 Message does not expose a direct output-token usage field, so CR-5 does not invent one. Exact output-token accounting remains a later native-hardening question if the API permits it.

Telemetry is raw-free: prompt contents, model message contents and tool argument payloads are not stored in the telemetry event.

### Gate evidence

Passed:
- :quest-core:test
- :litertlm-bridge:test
- :app:testDebugUnitTest
- :app:compileDebugKotlin

Static source review found no P3 runtime definition matching the formerly scattered 1200, 128 and 1280 limits outside InferenceBudget.

### CR-5 gate decision

validated_bounded for budget policy, pre-inference admission, bridge enforcement, telemetry shape and build integration.

Not claimed:
- exact output-token consumption, because the installed LiteRT-LM API does not expose it directly;
- semantic model success, which remains CR-7;
- conversion/resource A/B, which remains CR-8.

Next frontier: CR-6 — training/runtime contract regeneration.

## 20. CR-6 implementation record

Status: validated_bounded for training/runtime contract regeneration.

### Canonical training authority

CR-6 adds `tools/p3_training/generate_dataset.py`. The generator reads the existing Kotlin `MicroQuestToolContract` instead of introducing a second semantic tool schema. The Python layer derives:
- tool name and version;
- tool description;
- argument names;
- required arguments;
- argument descriptions;
- argument types.

The generated FunctionGemma declaration is therefore a representation of the canonical Kotlin contract, matching the architecture rule that training data and runtime declarations are derived views of one semantic contract.

### Declaration-marker correction

The historical generator manually added `<start_function_declaration>` and then concatenated a declaration string that already began with the same marker. CR-6 removes that duplication by making the declaration builder own only the declaration body/end marker while the template owns the single start marker.

The validator rejects any generated prompt that contains the declaration-start marker more than once.

### Completion/stop boundary

Generated completions now end exactly at `<end_function_call>`. The previous synthetic `<start_function_response>` tail is rejected by the generator validator. This keeps the SFT completion aligned with the native tool-call boundary rather than teaching a tool-response continuation into the model completion.

### Coverage preservation

The historical fixture matrix is preserved exactly at 360 rows:
- 6 categories;
- 5 environments;
- 3 difficulties;
- 4 language slices.

The split remains 288 training / 72 validation with seed 42.

### Gate evidence

Passed:
- 6/6 `tools/p3_training/test_generate_dataset.py` tests;
- generated 360-row dataset;
- zero duplicated declaration-start markers;
- zero response-tail completions;
- 360/360 completions end at `<end_function_call>`;
- 360/360 rows use exactly `compose_quest_text` as the tool.

CR-6 does not start another SFT run and does not claim native semantic success. CR-7 owns the native hardening matrix; CR-8 owns conversion/quantization A/B.

### CR-6 gate decision

validated_bounded for dataset generation, canonical declaration derivation, declaration uniqueness, completion shape and preserved fixture coverage.

Next frontier: CR-7 — native hardening validation.

## 21. CR-7 hardening checkpoint

Status: implemented_unvalidated; blocked on local toolchain availability.

Implemented in this checkpoint:
- Expanded `LiteRtToolCallMapperTest` into an explicit native-protocol matrix covering zero, one and multiple calls; unexpected tool name; missing, extra and null argument maps.
- Preserved the one-call and exact-argument invariants from `MicroQuestToolContract`.
- Configured the bridge Java compilation boundary to use a Java 21 compiler while emitting Java 17 bytecode, matching the LiteRT-LM 0.17.1 API artifact.

Validation:
- `:quest-core:test` — BUILD SUCCESSFUL.
- `:litertlm-bridge:test` is blocked because the machine currently has only JDK 17 installed, while `litertlm-android:0.17.1` contains Java class version 65 (Java 21). The build therefore fails before the native matrix can execute.
- No model inference run was started under the invalid compile boundary.

Gate decision:
CR-7 remains `implemented_unvalidated`, not `validated_bounded`. The blocker is environmental/toolchain, not evidence of model failure.

CR-8 is intentionally not started: conversion/quantization A/B must reuse a validated native adapter/tool protocol first.

## CR-7 / CR-8 native Android evidence � 2026-10-03

The native Android probe now exercises the actual `LiteRtLmRuntime` + `P3LiteRtToolSet` path with the canonical `compose_quest_text` contract. The environment was corrected to Microsoft OpenJDK 21, and the bridge/core/app unit-test and APK build gates pass.

The quantized pilot120 LiteRT-LM artifact (453495680 bytes) was copied to `emulator-5554` and executed through instrumentation. The model initialized and generated on CPU (~815 MiB PSS, ~313% CPU), proving runtime execution. The governed mapper nevertheless returned `InvalidOutput`, so the native transport is working but the quantized artifact does not currently satisfy the exact three-field P3 contract on-device.

The larger no-PTQ pilot120 artifact (1751747420 bytes) is not rerun in this checkpoint because prior evidence already records emulator LOW_MEMORY. The non-P3 FunctionGemma mobile-actions artifact is not considered a comparator because the runtime classifies it as `ModelUnavailable` in this harness.

CR-7 therefore remains `implemented_unvalidated`, and CR-8 remains experimental/unvalidated. No production dependency or AppGraph wiring is approved.


## 22. CR-7 semantic quality and latency diagnosis — 2026-10-03

The device probe was extended only for diagnosis and then returned to a production-clean bridge. The decisive observation is that the pilot120 model is not failing because one native argument is missing: its Message.toolCalls list is empty.

Instead, the model emits a textual pseudo-call beginning with <start_function_call> inside Message.contents. One captured output contained call:compose_quest_text with arg0/arg1/arg2, malformed extra fields and an unexpected call:pause. This proves that the current quantized artifact has learned a textual function-call representation that does not match the LiteRT-LM native ToolSet boundary.

### Real quest-generation latency sample

Three real prompts were executed through the same Android runtime with the quantized pilot120 artifact:

| sample | elapsed | native calls | outcome |
|---|---:|---:|---|
| 1 | 7.45 s | 0 | InvalidOutput |
| 2 | 4.06 s | 0 | InvalidOutput |
| 3 | 4.27 s | 0 | InvalidOutput |

The first run is treated as a colder runtime/model-init observation. The later ~4.1–4.3 s values are the more useful warm-generation signal. These are real end-to-end instrumentation measurements, not synthetic benchmark timings.

### Quest quality/tone

The observed semantics are currently below the PinhoQuest product bar. Captured text included “Jogue de outro jeito” and “Pequeno computador”, followed by malformed objective-like content. The model did not reliably produce the three intended creative fields, and the requested casual, friendly, playful and low-friction tone was not preserved consistently.

This is therefore both a protocol-adherence problem and a product-quality problem. A parser added after the model would hide the former and would not reliably solve the latter.

### Gemma comparison

The previously tested FunctionGemma mobile-actions G5 artifact was classified ModelUnavailable. The G6 artifact was also classified ModelUnavailable on this same runtime/device path, in about 2.1 s. Neither is a valid A/B comparison for quest quality or generation latency here.

### Fine-tuning direction

The evidence now makes a small corrective SFT/adapter experiment on pilot120 a reasonable candidate. The experiment should target the exact serving representation expected by the selected LiteRT-LM export/runtime path and reinforce the canonical P3 semantic fields and friendly quest tone. It should not introduce a production text parser or a case-specific output repair.

Gemma should remain a secondary candidate until a Gemma artifact is produced that actually initializes under the target runtime. P4/P5 remain untouched while CR-7 is bounded.

### Gate

CR-7 remains implemented_unvalidated. The runtime is executable and the latency is promising enough to justify product-oriented experimentation, but native semantic adherence is not proven and current quest quality is not acceptable. CR-8 remains experimental/unvalidated.
