# P3 Toolcalling Contract Diagnosis

Date: 2026-10-03
Branch: feature/p3-micro-quest-composer
Status: investigation

## Executive finding

The current P3 regression is better explained by contract/protocol divergence than by the model simply refusing instructions.

There are currently three different representations of the intended contract:

1. the P3 training representation: FunctionGemma developer turn + native function declaration + user turn + native function call;
2. the MicroQuestPromptSerializer representation: plain text instructions saying to call compose_quest_text, but without a tool declaration;
3. the Android production inference representation: no tools registered and ResponseFormat.json(...) used instead of the native FunctionGemma tool-call path.

The P3-trained model is therefore not receiving the same interface it was trained against.

## 1. Actual production quest flow

Current production graph:

QuestRequest
-> QuestSessionService.generate
-> QuestContextProvider
-> QuestPlanner
-> QuestGenerationPlan
-> ProceduralComposer
-> QuestValidator
-> Room

PinhoQuestAppGraph still wires ProceduralComposer. FunctionGemma is not in production.

Tags currently enter the quest pipeline here:

Room tags
-> SystemTagCatalog.categoryAffinities()
-> QuestContext.categoryAffinities
-> QuestPlanner
-> selectedCategory

The tag labels themselves do not currently cross into MicroQuestComposer. The QuestEngine calls composer.compose(plan), not composeWithDetails(plan, tags, examples).

## 2. Experimental P3 flow

The experimental P3 path is:

QuestGenerationPlan
+ tags/examples
-> MicroQuestCompositionRequest
-> MicroQuestPromptSerializer
-> GenerationRequest(prompt, maxOutputTokens=128)
-> LocalInferencePort
-> runtime adapter
-> raw rendered message
-> FunctionGemmaToolCallExtractor
-> FunctionGemmaResponseSanitizer fallback
-> MicroQuestRenderer
-> QuestValidator

The critical observation is that the runtime adapter is currently not a FunctionGemma tool adapter.

## 3. Where the contract diverges

### A. Training input

The SFT dataset manually embeds:

<start_of_turn>developer
You compose micro-quest text...
<start_function_declaration>
declaration:compose_quest_text{...}
<end_function_declaration>
<end_of_turn>

Then the user turn carries the quest context.

The supervised completion is:

<start_function_call>call:compose_quest_text{title:...,description:...,objectives:[...]}<end_function_call><start_function_response>

This matches the FunctionGemma tool-calling grammar in broad shape.

### B. Training prompt defect

The generated SFT dataset visibly contains:

<start_function_declaration><start_function_declaration>declaration:compose_quest_text...

The first marker comes from the base string and the second comes from the declaration constant.

This is malformed training context. It may not be the main cause of objective omission, but it is a concrete contract-generation defect and should not remain in future training datasets.

### C. Runtime prompt defect

MicroQuestPromptSerializer emits:

TAREFA=compose_quest_text
...
RETORNO=chame a funcao compose_quest_text com title, description e objectives.

It does not emit the FunctionGemma tool declaration and does not create the developer message containing the required trigger phrase.

Google's FunctionGemma guidance explicitly treats the developer turn containing the function declaration and the function-calling trigger as essential for activating the tool-calling behavior. citeturn487163search0turn487163search7

Therefore the runtime prompt is not equivalent to the training prompt.

### D. Android adapter defect

AndroidLiteRtLmInferencePort creates:

ConversationConfig(
    tools = emptyList(),
    automaticToolCalling = false,
    ...
)

and calls:

sendMessage(..., ResponseFormat.json(textSchema()))

So the adapter asks the model for generic structured JSON and registers zero tools.

The P3-trained model, however, was trained to emit the FunctionGemma native tool call.

By contrast, the isolated NativeComposeProbe registers the actual composeQuestText tool and uses automaticToolCalling=false, which is the documented manual-tool flow; LiteRT-LM returns the generated call through Message.toolCalls. citeturn129700search1turn129700search5

This is the strongest current candidate for the protocol regression.

### E. Output protocol duplication

MicroQuestComposer first tries FunctionGemmaToolCallExtractor, then falls back to FunctionGemmaResponseSanitizer.

That means one P3 model is currently allowed to speak two output protocols:

- native FunctionGemma tool call;
- generic JSON object.

This increases ambiguity and can conceal an adapter mismatch.

For the P3 FunctionGemma path, the authoritative protocol should be the native tool call. Generic JSON should remain a separate protocol/model path rather than a silent fallback for the same model.

## 4. Why objectives disappeared

The evidence now separates two different failure classes.

### Class 1: protocol mismatch

When a FunctionGemma-trained model is run through the generic JSON adapter, the model is not being given the exact trained interface. Missing/partial fields are therefore not strong evidence of model incapability.

### Class 2: mobile conversion degradation

The quantized pilot120 .litertlm was tested through the native tool API and still omitted objectives.

That is real evidence of a second problem after conversion.

The FP conversion did not reach inference because the ~1.75 GB bundle caused the emulator process to terminate with LOW_MEMORY. Therefore the FP A/B is inconclusive.

The correct next A/B is not another generic prompt. It is:

same prompt
+ same native tool schema
+ same manual-tool-call runtime
+ native Message.toolCalls inspection
with the hardening/stop-boundary checkpoint.

Only after that result should conversion/quantization be treated as the remaining variable.

## 5. Budget inflation / governance audit

There is not one P3 budget. There are several disconnected budgets.

### MicroQuestComposer
GenerationRequest.maxOutputTokens = 128.

### Android engine
EngineConfig.maxNumTokens = 1280.

### Micro prompt
MicroQuestPromptSerializer maxCharacters = 1200.

### Generic prompt subsystem
PromptContractLimits.maxPromptCharacters = 1400.

### Model conversion
pilot120 export used prefill_lengths=[32] and cache_length=256.

These values are not derived from one canonical budget contract.

The most dangerous case is the output-token split:

MicroQuestComposer requests 128 tokens, while the engine is provisioned for 1280. The smaller request is good for safety, but there is no single contract saying why 128 is sufficient and what happens if the model needs more tokens to finish a function call.

The second danger is context budget. The conversion's prefill/cache limits are separate from the character budget. Character count is not tokenizer token count, so the 1200-character guard is useful only as a coarse admission boundary, not a real model-context budget.

## 6. Raw-data leakage risk

P3-5A already implements a good canonical boundary:

QuestGenerationPlan + approved facts
-> PromptFactsAssembler
-> BoundedPromptEnvelope
-> CompactPromptSerializer
-> LocalInferencePort

PromptFactsAssembler sanitizes and bounds enabled tag labels and typed research hints.

However, MicroQuestComposer does not use PromptFactsAssembler or CompactPromptSerializer.

Its MicroQuestCompositionRequest has:

tags.size <= 6
examples.size <= 3

but it does not bound/sanitize the content of each tag or example before concatenation.

Therefore, if a caller later supplies external tag/example text directly to this path, the P3 micro serializer can receive control characters or oversized/untrusted text and only reject the final prompt if the combined string exceeds 1200 characters.

That is not the same governance boundary as P3-5A.

There is also no evidence that raw web research currently reaches the production composer. The existing research pipeline is still architecturally separated.

## 7. Additional contract collision

CompactPromptSerializer emits a six-field JSON contract:

title
description
objectives
bonusObjectives
estimatedMinutes
estimatedDifficulty

MicroQuestContract is a three-field contract:

title
description
objectives

QuestDraftCodec also expects the six-field contract.

These are valid contracts for different stages, but they should not share an undifferentiated model-facing path.

For FunctionGemma P3, the native tool schema should be exactly the three creative fields. Deterministic category, environment, difficulty and duration remain outside the model.

## 8. Stop-boundary finding

The SFT completion currently contains <start_function_response> after the tool call.

FunctionGemma's official formatting documentation identifies <start_function_response> as an inference stop sequence. citeturn487163search0

Therefore the model learning that token after <end_function_call> is not inherently wrong. In a correctly configured FunctionGemma runtime, the engine should stop/parse the function call before a tool response is supplied.

The observed continuation of a second call in our raw Android output is therefore another reason to inspect runtime parsing and tool-call handling before adding more fine-tuning.

The stop-boundary SFT is still useful as an experiment, but it should not be considered the primary fix.

## 9. Tags + quests: authority map

Tags currently influence category selection deterministically.

Enabled tags
-> category affinity
-> QuestPlanner
-> selected category
-> quest generation

The model does not currently own tag selection, tag meaning, affinity scoring, reward calculation or quest category.

When P3 eventually wants tags to influence humanized wording, the safe path is:

Room tag labels
-> PromptFactsAssembler
-> bounded PromptTag[]
-> BoundedPromptEnvelope
-> canonical tool call
-> MicroQuestText
-> trusted renderer

No raw Room entity, affinity value, profile history, search result, URL, HTML or research document should cross the model boundary.

## 10. Recommended correction order

1. Make one canonical FunctionGemma tool contract the source of truth for P3.
2. Make the Android adapter register exactly that tool and return typed Message.toolCalls data.
3. Make the P3 prompt builder consume the bounded envelope rather than inventing a second prompt format.
4. Remove the generic JSON fallback from the FunctionGemma P3 protocol.
5. Add a real token-budget contract tying prompt budget, output budget and model bundle capacity together.
6. Regenerate SFT data from the canonical tool schema/chat-template path; eliminate the duplicated declaration marker.
7. Re-run the hardening checkpoint through native Message.toolCalls.
8. Only then repeat the mobile conversion/quantization A/B.

## Current diagnosis

Most probable root cause:

**P3 training and P3 Android inference are speaking different contracts.**

Secondary, independently demonstrated issue:

**quantized mobile conversion can regress structured tool-call arguments.**

Governance issue:

**the canonical P3-5A sanitizer/bounded prompt path exists but is not connected to MicroQuestComposer.**

Budget issue:

**multiple independent limits exist without one canonical token/context budget contract.**

Production status remains unchanged: FunctionGemma is not wired into PinhoQuestAppGraph.
