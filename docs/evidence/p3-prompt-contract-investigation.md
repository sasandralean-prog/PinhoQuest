# P3 Prompt/Context Contract Investigation

Date: 2026-10-02
Status: **investigation complete; contract boundary identified**

## Executive finding

The hypothesis that uncontrolled prompt/context payloads can destabilize the local runtime is **partially supported**, but prompt size alone is not the primary cause demonstrated by the current evidence.

The stronger finding is:

> PinhoQuest does not yet have a canonical, deterministic LLM prompt contract. The production graph currently stops at `QuestGenerationPlan -> ComposerPort`; the LLM composer and research-to-LLM sanitization boundary are not implemented yet.

The benchmark therefore exercised an artificial prompt, while production has not yet leaked tags or research into a model prompt.

## Evidence

### 1. Minimal prompt

A single shared Engine, with a fresh Conversation for each request, was exercised with ten minimal prompts:

`OK -> "Responda apenas OK."`

Result:

- 10/10 successful generations
- no `FAILED_PRECONDITION`
- no crash
- completion remained stable through the 10th Conversation

### 2. Prompt-length-only probe

The same one-shot output contract was exercised with input sizes ranging from a few characters to about 1,000 characters.

Result:

- 10/10 successful generations
- no `FAILED_PRECONDITION`

Therefore, input character count by itself is not enough to reproduce the failure.

### 3. Long structured quest prompt

The original structured quest prompt was used with one fresh Conversation per fixture on a shared Engine.

Result:

- first 5 structured generations succeeded
- the 6th failed almost immediately
- subsequent conversations failed with:

`LiteRtLmJniException: Failed to call nativeSendMessage: FAILED_PRECONDITION: Chosen prefill work group size exceeds available state entries (1).`

Successful model output was small, but the runtime returned ChatML/channel wrappers and Markdown around the JSON.

### 4. Compact structured quest prompt

The same basic structured output contract was kept, but the instruction envelope was reduced substantially and irrelevant prose was removed.

Result:

- 8/8 fixtures completed successfully
- no `FAILED_PRECONDITION`
- generated drafts remained structurally small
- the model still ignored some semantic constraints and repeated generic patterns, proving that small context does not solve quality/authority problems by itself

## Interpretation

There are three distinct mechanisms that must not be conflated:

### A. Context growth inside one Conversation

LiteRT-LM's Conversation is explicitly stateful and keeps message history in model context. The current LiteRT-LM issue #3444 documents the exact `available state entries` failure when remaining context falls below the model bundle's usable prefill signature. The upstream project subsequently merged a fix to clamp requested `max_num_tokens` to static model capacity.

That mechanism explains why long multi-turn sessions can hit the error, but it does not by itself explain our fresh-Conversation reproduction.

### B. Engine-level lifecycle state

LiteRT-LM also has open reports where state survives Conversation boundaries or the Engine becomes wedged across repeated conversations, including a Qwen CPU case where closing/recreating Conversations does not fully reset Engine state.

This is a credible explanation for the failure observed after repeated structured generations on the shared Engine.

### C. Prompt envelope as an amplifier

Our controlled probes show that the semantic/structural shape of the request matters.

A short one-shot envelope survives repeated Conversation creation.
A compact structured envelope also survives repeated Conversation creation.
The longer structured envelope reproduces the native failure.

Therefore the prompt contract should be treated as a **runtime budget boundary**, even though it is not sufficient to explain the engine lifecycle defect.

## Architectural hole discovered

Current production path:

`QuestRequest -> QuestPlanner -> QuestGenerationPlan -> ComposerPort -> QuestDraft -> QuestValidator`

Current `QuestGenerationPlan` contains:

- mode
- session filters
- selected category
- selected environment
- affinity weight
- optional game candidate

The plan does **not** currently contain a raw prompt, raw tag collection, raw research body, or unbounded external payload.

Current tag handling is deterministic:

`Room tags -> SystemTagCatalog.categoryAffinities() -> QuestContext`

This is good and should remain the authority.

Research is planned as a separate subsystem and is not yet connected to the composer. The design already requires research observations to be normalized, preserve provenance, and keep personal profile/ranking local.

## Proposed canonical LLM boundary

Before `LocalInferencePort`, introduce a deterministic prompt boundary with three layers:

`Domain state / research observations`
→ `PromptFactsAssembler`
→ `BoundedPromptEnvelope`
→ `LocalInferencePort`

### PromptFactsAssembler

Only deterministic code can select what the model is allowed to see.

Input sources:

- frozen `QuestGenerationPlan`
- a bounded selection of enabled tags
- validated game/flower facts when relevant
- temporary session filters

It must discard:

- affinity numeric internals
- Room entities
- raw HTML
- raw search results
- source documents
- URLs unless explicitly required by a dedicated factual task
- timestamps and provenance details not needed for composition
- unrelated profile/history data

### BoundedPromptEnvelope

The runtime receives a small, versioned contract rather than arbitrary text.

Conceptually:

`PromptEnvelope`
- schemaVersion
- task
- category
- environment
- timeRange
- difficulty
- selectedTags[]
- researchHints[]
- outputContract

Each collection is bounded and each textual field has a deterministic maximum length.

The envelope should be serializable into a stable, compact prompt representation with one fixed instruction preamble.

### ResearchHints

Research is sanitized before it can cross the boundary.

For example, a game hint should contain only allowlisted factual fields such as:

- canonical name
- applicable platform
- genre
- validated free-status observation

It should **not** contain the full article/search result.

Flower lore should follow the same rule: validated facts cross first; creative prose is produced only after factual validation.

### Output boundary

The runtime response must enter a strict codec:

`raw runtime message`
→ `extract allowed payload`
→ `decode schema`
→ `domain constraints`
→ `QuestValidator`

No LLM response may become a `Quest` directly.

## Important consequence for P3

Do not solve the current runtime problem by:

- increasing `maxNumTokens` blindly;
- allowing larger prompt payloads;
- retrying indefinitely;
- injecting raw context;
- moving validation into the LLM;
- replacing deterministic tags/research selection with model reasoning.

The safer production contract is actually the opposite:

**small deterministic input + bounded output + one canonical validator + explicit runtime lifecycle.**

## Current recommendation

Split P3-5 conceptually into two gates:

### P3-5A — Prompt contract

Implement and unit-test:

`PromptFactsAssembler`
→ `BoundedPromptEnvelope`
→ deterministic compact serialization

with tests proving that raw research, raw tags, profile data, and unbounded text cannot cross the boundary.

### P3-5B — Runtime adapter

Only after the prompt contract exists, test:

`BoundedPromptEnvelope`
→ selected runtime
→ `QuestDraftCodec`
→ `QuestValidator`

At the runtime layer, the adapter must also own the single-shot/lifecycle policy and never expose the native runtime object to core.

## External LiteRT-LM evidence

The official LiteRT-LM documentation describes Conversation as stateful and responsible for prompt-template rendering/history management.

Upstream issue #3444 documents the exact `available state entries` failure as a context-capacity boundary, while PR #3592 merged a fix to clamp requested token capacity to static model capacity.

Upstream issue #2256 documents Engine-level state surviving Conversation recreation for a Qwen CPU workload, and issue #2028 documents repeated Conversation failures on Android in another deployment configuration.

These upstream reports make it unsafe to attribute our reproduction exclusively to PinhoQuest prompt construction.

## Gate

P3-4 remains **BLOCKED / NOT SELECTED**.

P3-5 should not integrate a production runtime until both:

1. the prompt contract is deterministic and bounded; and
2. the selected runtime passes lifecycle + 20-fixture benchmark evidence.

