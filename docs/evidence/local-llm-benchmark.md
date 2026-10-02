# P3 Runtime Benchmark Gate

Date: 2026-10-02
Status: **BLOCKED / NOT SELECTED**

## Scope

Task 4 of the P3 local-LLM plan was executed as an isolated Android harness so the PinhoQuest production graph remained unchanged.

Target model:

- Repository: `litert-community/Qwen3-0.6B-int4`
- Model: `qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm`
- Model blob SHA-256: `2df6821ec12702dafd33915e7a1a1adc7c4b053f3672fd9555dfaf3a114c4139`
- Size: `347251840` bytes
- Format: LiteRT-LM
- Quantization: dynamic INT4 block32
- KV cache limit: 1280
- License: Apache-2.0
- Runtime artifact: `com.google.ai.edge.litertlm:litertlm-android:0.17.1`

The model metadata and exact SHA/size are published in the model repository commit `6aa2daf8aba4aa456797fb8040b36a3948bcfda7`.

## Device

- Android emulator: `sdk_gphone64_x86_64`
- ABI: `x86_64`
- Android SDK: 33
- Backend: LiteRT-LM CPU / XNNPACK
- maxNumTokens: 1280
- Sampler: topK=40, topP=0.9, temperature=0.2, seed=42
- 20 fixed quest-generation fixtures

This is runtime evidence only; it is not treated as representative of a physical low/mid-range phone.

## Observed cold/warm initialization

Cold initialization:

- 8014 ms
- PSS immediately after initialization: 647392 KiB

Warm initialization after XNNPACK cache:

- 3162 ms
- PSS immediately after initialization: 571781 KiB

Peak observed PSS during successful generation:

- 930324 KiB (~909 MiB)

## Lifecycle result

The first run kept one Engine and created a fresh Conversation for each fixture.

- Fixtures attempted: 20
- Completed generations: 5
- Runtime failures: 15
- First reproducible failure: fixture 5 (sixth Conversation on the same Engine)
- Failure:
  `LiteRtLmJniException: Failed to call nativeSendMessage: FAILED_PRECONDITION: Chosen prefill work group size exceeds available state entries (1).`

The failures then occurred immediately on subsequent fresh Conversation attempts.

This is a lifecycle/runtime gate failure, not a prompt-validation failure.

The earlier single-Conversation experiment also reached the model's 1280-token context limit after repeated requests and produced a different native state failure. That experiment is retained only as diagnostic evidence and is not considered a valid benchmark.

## Output observations

Successful calls produced a recoverable JSON object, but wrapped it in LiteRT/Qwen ChatML markers and Markdown fences despite the prompt requesting JSON only.

The decoded object contained the expected structural fields:

- title
- description
- objectives
- bonusObjectives
- estimatedMinutes
- estimatedDifficulty

However, the model also showed weak fixture adherence in the observed successful samples (for example, repeating the same coding-oriented quest pattern across distinct requests). Therefore no semantic quality claim is made from this run.

Because the runtime lifecycle gate failed before 20 independent fixture executions completed, a formal validator-acceptable output rate and candidate selection were **not declared**.

## Follow-up benchmark — 2026-10-02

A second Qwen run used the bounded compact prompt together with LiteRT-LM structured ResponseFormat.json, maxOutputToken=192, disabled thinking, and a fresh Conversation per fixture on one Engine.

- Fixtures attempted: 20
- Runtime errors: 0
- Strict contract acceptance: 17/20
- Rejected as incomplete by the post-run contract probe: fixtures 8, 9, 12
- Observed generation latency: 14.6 s to 42.6 s per fixture in the saved TSV evidence
- Observed post-generation PSS: about 825-883 MiB
- The previous FAILED_PRECONDITION lifecycle error was not reproduced in this constrained configuration.

This changes the diagnosis: the failure is configuration-sensitive rather than a universal proof that every repeated Conversation on this Engine fails. However, the constrained Qwen outputs still contained prompt/instruction leakage, repeated or malformed objective content, and weak fixture adherence. Therefore 17/20 strict structural acceptance is not treated as a V1 quality gate pass.

A second Apache-2.0 candidate, litert-community/SmolLM2-360M-Instruct, was fetched at the exact upstream size of 373719040 bytes with SHA-256 8e2834da211b439751af968ed650febdde5a8cb8d88bc6c1a3059f049caa5c2e. A five-fixture smoke was intentionally started before committing to a full 20-fixture run. Fixture 0 took 90170 ms and reached 1301849 KiB PSS, while the generated text was largely prompt-shaped rather than a clean quest draft. The candidate was stopped at that point because the first-fixture evidence already violated the intended lightweight/cost profile.

No model/runtime combination is selected from this follow-up.

## Decision

Do **not** select LiteRT-LM 0.17.1 + Qwen3-0.6B no-think as the V1 production runtime from this evidence.

Do **not** add the runtime to the canonical PinhoQuest production generation path.

Do **not** weaken admission, validation, or lifecycle gates to make the candidate pass.

P3-5 remains blocked until a runtime/model/backend combination completes the Task 4 benchmark gate, including 20 fixed fixtures and lifecycle stability.

## PinhoQuest repository state

At the end of this investigation:

- branch: `feature/p3-local-llm`
- HEAD: `451be84`
- existing P3 commits:
  - `d5e918e` — validated local model store
  - `1606b78` — local model download flow
  - `451be84` — resource-aware inference admission
- production worktree: clean
- benchmark harness: isolated outside the production worktree

## Next gate

The next action is to isolate whether the lifecycle failure belongs to:

1. LiteRT-LM 0.17.1 Android binding,
2. repeated Conversation creation/close on one Engine,
3. this specific Qwen3 INT4 artifact,
4. or the harness/API usage.

Only after that reproduction is understood should another candidate/backend be benchmarked.

