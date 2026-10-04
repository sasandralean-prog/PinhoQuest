# P3 Pilot120 FunctionGemma Re-export Evidence

Date: 2026-10-04
Status: validated_bounded for native transport; semantic quality gate still open

## Question

Can the CR-7.1 Pilot120 checkpoint be served through the LiteRT-LM native FunctionGemma tool boundary by correcting the export identity and runtime-compatible template in the Linux conversion environment?

## Baseline failure

The previous Pilot120 Android artifact was exported as `generic_model`. Real Android inference executed, but `Message.toolCalls` remained empty and the model emitted textual pseudo-function-call content. The governed P3 mapper correctly rejected that output. No production text parser was added.

## Candidate checkpoint

- Checkpoint: `D:\AI\HuggingFacesLLM\p3_sft_small\cr71\merged_step10`
- Base identity supplied to exporter: `google/functiongemma-270m-it`
- Host SFT evidence: complete FunctionGemma calls with `title`, `description` and `objectives` before mobile export.

## Linux export

Environment: WSL2 `DebianRepair`, `litetune` export environment `e9d2cdffa927`.

The canonical litetune family rules added:

- `--litert_lm_model_type_override=function_gemma`
- `--jinja_chat_template_override=.../litetune/templates/functiongemma.jinja`
- `--externalize_embedder`

Recipe: `dynamic_wi8_afp32`.

Export result:

- duration: 214.784 s
- bytes: 456,643,888
- SHA-256: `8cdb37d1debde293ca1975f036be6b948db05b32bda6b1235fdd789cbb78036e`
- return code: 0

The final unpacked bundle contains `llm_model_type { function_gemma {} }` and the FunctionGemma chat template. The prefill/decode section carries the existing `prefer_activation_type=fp32` metadata.

## Android artifact integrity

The exact exported file was copied to the emulator application sandbox and verified before inference:

- size: 456,643,888 bytes
- SHA-256: `8cdb37d1debde293ca1975f036be6b948db05b32bda6b1235fdd789cbb78036e`

An earlier copy was intentionally rejected because the emulator had only about 266 MiB free and the file was truncated to about 306 MiB. Temporary P3 model files were removed, free space recovered to about 1.3 GiB, and the artifact was copied again successfully.

## Native Android result

`P3NativeToolCallE2ETest.realModelReturnsExactlyOneCanonicalToolCall` passed with the exact re-exported artifact.

Observed telemetry from logcat:

- `observedToolCalls=1`
- `stopReason=NATIVE_TOOL_CALL`
- observed conversation token delta: 272
- warm inference elapsed: 9,943 ms
- runner time: 10.155 s

Returned typed call:

`compose_quest_text`

Arguments:

- `title`: String
- `description`: String
- `objectives`: List<String>

The test also proves the argument key set is exactly the canonical `MicroQuestToolContract` required set.

## Semantic-quality caveat

The native transport probe uses a deliberately compact transport-focused prompt. Its returned text included an instruction-echo objective (`Criar uma chamada para compose_quest_text.`), so this is not evidence that production quest wording is already acceptable.

The current conclusion is therefore:

`host checkpoint semantics -> Linux FunctionGemma export -> native Message.toolCalls` is working.

What remains is the separate product-quality gate using the canonical bounded P3 prompt/serializer, including tone, relevance, objective usefulness and fixture diversity.

## Decision

- Pilot120 remains the preferred P3 candidate.
- The P3 three-field contract is unchanged.
- No textual parser fallback is introduced.
- The export correction is accepted as the bounded native transport correction.
- No production AppGraph wiring is approved yet.
- Next gate: real canonical P3 prompt E2E/quality benchmark on this valid FunctionGemma-exported artifact.
