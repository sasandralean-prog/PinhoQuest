# Pinho Quest Local LLM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a fully local optional quest-composer model with verified ModelStore installation, resource-aware admission, structured output validation, and invisible procedural fallback.

**Architecture:** Keep model/runtime details behind `LocalInferencePort`. `InferenceAdmissionController` decides whether inference may run now; `QuestEngine` still converges both LLM and procedural output through the same `QuestValidator`.

**Tech Stack:** Kotlin/JVM contracts, Android runtime adapter, WorkManager for model download, SHA-256 verification, a current supported on-device LLM runtime selected by benchmark, Room/DataStore only for metadata/preferences.

**Spec:** `docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`

## Global Constraints

- Model inference runs on-device; ordinary quest generation never requires a remote LLM.
- Procedural composer remains a complete fallback.
- The LLM never decides XP, rarity, reward ownership, factual game availability, or botanical facts.
- No device-name whitelist and no invented universal CPU-free percentage gate.
- Admission uses memory pressure/headroom, thermal state, runtime readiness, workload/concurrency, and local execution history.
- Model files stay in private ModelStore and are not part of `.pqbackup`.

## Review Focus

- Corrupted/partial model downloads must never be promoted as installed.
- Low-memory or thermally throttled states must select procedural fallback before loading/generating.
- Invalid or hallucinated structured output must be rejected by `QuestValidator`, not partially accepted.
- Process death during model download must resume/retry safely without leaving two promoted versions.
- Runtime/model upgrade must not silently change the active model until manifest and hash validate.

---

### Task 1: Model package and store contracts

**Files:**
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/model/ModelManifest.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/inference/ModelStorePort.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/model/AndroidModelStore.kt`
- Test: `android-data/src/test/kotlin/com/pinhoquest/data/model/AndroidModelStoreTest.kt`

**Interfaces:**
- `data class ModelManifest(modelId, version, runtimeFormat, sha256, bytes, license, source, contextLimit, supportedBackends)`
- `ModelStorePort.install(stagedFile, manifest): ModelInstallResult`
- `ModelStorePort.active(): InstalledModel?`

- [x] **Step 1: Write tests** for correct hash/size, wrong hash, wrong size, interrupted `.part`, and atomic version promotion.
- [x] **Step 2: Run** model-store tests. **Expected:** FAIL.
- [x] **Step 3: Implement** private ModelStore with staged file, SHA-256/manifest validation, and atomic promotion.
- [x] **Step 4: Run** tests. **Expected:** PASS.
- [x] **Step 5: Commit** `feat: add validated local model store`.

### Task 2: Download/install flow

**Files:**
- Create: `app/src/main/kotlin/com/pinhoquest/model/ModelDownloadWorker.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/model/ModelInstallCoordinator.kt`
- Modify: `app/src/main/kotlin/com/pinhoquest/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/kotlin/com/pinhoquest/ui/settings/SettingsScreen.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/model/ModelInstallFlowTest.kt`

**Interfaces:**
- Produces user-invoked `Baixar cérebro criativo`, resumable/retryable download, and verified installed state.

- [x] **Step 1: Write tests** for decline, successful install, interrupted retry, bad hash, and humanized validation-failure copy.
- [x] **Step 2: Run** tests. **Expected:** FAIL.
- [x] **Step 3: Implement** WorkManager-backed download/install; primary UI copy must not expose raw checksum/runtime errors.
- [x] **Step 4: Run** tests. **Expected:** PASS.
- [x] **Step 5: Commit** `feat: add local model download flow`.

### Task 3: Inference admission contracts

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/inference/InferenceModels.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/inference/InferenceAdmissionController.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/inference/AndroidResourceSnapshotProvider.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/inference/InferenceAdmissionControllerTest.kt`

**Interfaces:**
- `data class InferenceResourceSnapshot(memoryState, thermalState, runtimeState, appWorkload, inferenceBusy, learnedProfile)`
- `InferenceAdmissionController.decide(snapshot, model): AdmissionDecision`
- `AdmissionDecision = Admit | UseFallback(reason)`

- [ ] **Step 1: Write tests** for low-memory, severe thermal state, missing model/runtime, concurrent inference, and healthy headroom.
- [ ] **Step 2: Add review-focus test** proving healthy general workload cannot override low-memory rejection; no CPU-percent shortcut exists.
- [ ] **Step 3: Run** tests. **Expected:** FAIL.
- [ ] **Step 4: Implement** policy and Android resource snapshot using supported memory/thermal signals.
- [ ] **Step 5: Run** tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add resource aware inference admission`.

### Task 4: Runtime/model benchmark gate

**Files:**
- Create: `docs/evidence/local-llm-benchmark.md`
- Create: `app/src/androidTest/kotlin/com/pinhoquest/inference/LocalModelBenchmarkTest.kt`

**Interfaces:**
- Produces one selected V1 model/runtime/backend strategy with exact model ID/version/license/size and device evidence.

- [ ] **Step 1: Enumerate** currently supported on-device runtime/model candidates satisfying Android and license requirements; record sources.
- [ ] **Step 2: Benchmark** at least two viable lightweight candidates or backend configurations against 20 fixed `QuestGenerationPlan` fixtures.
- [ ] **Step 3: Record** install size, memory observation, generation latency, thermal change, validator-acceptable output rate, and failure behavior on the available low/mid-range Android test device.
- [ ] **Step 4: Select** the smallest/lowest-cost candidate that reliably produces short validator-acceptable drafts; document why alternatives lost.
- [ ] **Step 5: Commit** evidence/configuration only; do not route production generation through it yet.

### Task 5: Local inference adapter and structured composer

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/inference/LocalInferencePort.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/quest/LocalLlmComposer.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/inference/AndroidLocalInferenceRuntime.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/inference/QuestDraftCodec.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/quest/LocalLlmComposerTest.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/inference/QuestDraftCodecTest.kt`

**Interfaces:**
- `LocalInferencePort.generate(request: GenerationRequest): InferenceOutcome`
- `LocalLlmComposer.compose(plan: QuestGenerationPlan): QuestDraft`
- Structured output fields: `title`, `description`, `objectives`, `bonusObjectives`, `estimatedMinutes`, `estimatedDifficulty`.

- [ ] **Step 1: Write tests** for valid structured output, missing field, extra prose around payload, malformed payload, and unsupported difficulty/time values.
- [ ] **Step 2: Run** tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** adapter/codec using the selected runtime; invalid output must never map directly to a valid `Quest`.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add local LLM quest composer`.

### Task 6: Natural-language tag suggestion

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/tag/TagSuggestionService.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/tags/AddAboutMeDialog.kt`
- Modify: `app/src/main/kotlin/com/pinhoquest/ui/onboarding/OnboardingScreen.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/tag/TagSuggestionServiceTest.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/ui/TagSuggestionFlowTest.kt`

**Interfaces:**
- `TagSuggestionService.suggest(rawText: String, existing: List<Tag>): TagSuggestionResult`
- Suggestions are `DERIVED` candidates only; none become active until explicit user confirmation.

- [ ] **Step 1: Write tests** for empty text, duplicate-existing tags, valid multi-tag suggestions, malformed model output, and explicit confirmation requirement.
- [ ] **Step 2: Run** tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** local inference-backed suggestions with bounded procedural fallback; never overwrite USER tags.
- [ ] **Step 4: Implement** the “Adicionar algo sobre mim” dialog and confirmation UI.
- [ ] **Step 5: Run** core/UI tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add user confirmed derived tags`.

### Task 7: Admission-aware QuestEngine integration

**Files:**
- Modify: `quest-core/src/main/kotlin/com/pinhoquest/core/quest/QuestEngine.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/inference/InferenceExecutionProfile.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/quest/QuestEngineInferenceFallbackTest.kt`

**Interfaces:**
- One canonical generation flow chooses Local LLM only after admission and always converges through `QuestValidator`.

- [ ] **Step 1: Write tests** proving admitted inference is attempted, non-admitted inference is never loaded, invalid inference falls back, and both paths still pass `QuestValidator`.
- [ ] **Step 2: Run** tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** orchestration and persist local-only execution-profile metrics after each attempt.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: integrate local inference with procedural fallback`.

### Task 8: Local-LLM runtime checkpoint

**Files:**
- Create: `docs/checkpoints/V1-P3-local-llm.md`

- [ ] **Step 1: Run** all unit/instrumentation/lint/build gates.
- [ ] **Step 2: On healthy device resources**, generate at least five quests and record that local inference was admitted and every promoted quest passed validation.
- [ ] **Step 3: Under low-memory/thermal/concurrent conditions**, verify procedural fallback without user-facing technical error.
- [ ] **Step 4: Restart** after model installation and verify the same validated model remains active.
- [ ] **Step 5: Record evidence and commit** `docs: validate local LLM runtime`.