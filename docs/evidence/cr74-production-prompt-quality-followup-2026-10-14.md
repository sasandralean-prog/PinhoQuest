# CR-7.4 Production Prompt Quality Follow-up

Date: 2026-10-14  
Status: **prompt-contract hardening implemented; real-device semantic-quality gate remains open**

## Device observation

A device test after model installation showed a quest card with the title \`Pequeno desvio\`, a description repeating the internal instruction not to invent facts or repeat instructions, and an objective repeating the schema limit \`1-4 textos de 1-120 chars\`. The native call was therefore syntactically successful but semantically unacceptable.

## Root cause

The runtime prompt serializer did not give the model the category-specific approved facts and action seed used by the FunctionGemma training dataset. It also included an empty examples section, and emitted \`Tempo=ANY\` when the UI renderer defaults an unfiltered quest to 15–30 minutes. These changes made runtime input differ from the model's training fixtures.

Separately, \`MicroQuestToolCallDecoder\` enforced field names, types, counts, and character lengths, but did not reject echoed prompt instructions or schema placeholders. The malformed semantic payload therefore reached \`MicroQuestRenderer\` and \`QuestValidator\`, whose domain validation covers structural integrity rather than linguistic quality.

## Changes

- Runtime and training now read per-category Portuguese and English seeds from \`MicroQuestCategorySeedCatalog.kt\`.
- Runtime prompt fields use the same \`Categoria\`, \`Ambiente\`, \`Dificuldade\`, \`Tempo\`, \`Idioma\`, \`Fatos aprovados\`, and \`Objetivo aprovado\` structure as training fixtures.
- An unfiltered prompt now sends the same 15–30 minute range that \`MicroQuestRenderer\` displays.
- The decoder rejects known instruction-echo phrases and numeric output-contract placeholders. The existing \`MicroQuestComposer\` path converts rejected output to the validated procedural fallback with reason \`INVALID_MODEL_OUTPUT\`.
- Unit and instrumented tests cover the observed leakage; training generator tests confirm it reads the same seed catalog as runtime.

## Validation boundary

A green compile or a successful native ToolCall is **not** proof of good quest semantics. The actual CR-7.4 model still needs a device quality run over representative categories and environments, checking whether its output remains useful and natural after the aligned prompt. When the model echoes internal text, the expected behavior is silent procedural fallback rather than presenting that text to the player.
