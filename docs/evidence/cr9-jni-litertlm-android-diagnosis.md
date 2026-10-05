# CR-9 — JNI / LiteRT-LM Android Integration Diagnosis

Date: 2026-10-05
Branch: feature/cr-0-runtime-consolidation
Status: investigation_in_progress; no production code refactor approved yet

## Scope

This investigation separates three boundaries: model artifact compatibility with LiteRT-LM; Kotlin/Gradle compile-time compatibility with LiteRT-LM 0.17.1; and Android runtime loading of the LiteRT-LM classes and JNI shared library.

The canonical CR-7.4 model has SHA-256 e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb and size 284,692,656 bytes.

CR-8 already established that this artifact executes through the validated native LiteRT-LM path and produces 5/5 canonical compose_quest_text ToolCalls. Therefore CR-9 must not classify the model as defective without new evidence.

## Observed facts

### A. LiteRT-LM 0.17.1 packages the JNI library

The resolved Maven dependency is com.google.ai.edge.litertlm:litertlm-android:0.17.1.

The local AAR is 20,493,837 bytes and contains classes.jar plus jni/arm64-v8a/liblitertlm_jni.so (21,802,960 bytes) and jni/x86_64/liblitertlm_jni.so (25,968,008 bytes).

The productive debug APK also contains arm64-v8a and x86_64 liblitertlm_jni.so.

### B. The native symbol exists

Android NDK llvm-nm inspection found the exported symbol Java_com_google_ai_edge_litertlm_NativeLibraryLoader_nativeCheckLoaded in the arm64-v8a 0.17.1 library.

Therefore the hypothesis that the AAR simply lacks nativeCheckLoaded is false.

### C. The Java/Kotlin and native sides nominally agree

The 0.17.1 NativeLibraryLoader declares nativeCheckLoaded(), while the native source exports the corresponding JNI symbol.

The official loader uses JNI_LIBNAME = litertlm_jni, first attempts System.loadLibrary("litertlm_jni"), then tries resource extraction.

The Pinho Quest bridge calls NativeLibraryLoader.INSTANCE.load() before engine.initialize().

### D. The compile-time dependency is genuinely polluted by Kotlin 2.4.0

The project toolchain is Kotlin 2.1.21.

The resolved app compile classpath currently contains org.jetbrains.kotlin:kotlin-reflect:2.4.0 through litertlm-android:0.17.1.

The dependency path is litertlm-android:0.17.1 -> kotlin-reflect:2.4.0 -> kotlin-stdlib:2.4.0.

The current app rule forces Kotlin stdlib 2.1.21 on CompileClasspath, but that does not remove the direct kotlin-reflect 2.4.0 dependency.

### E. The model is not the source of the Kotlin metadata mismatch

A .litertlm model is a runtime model artifact. Kotlin metadata mismatch comes from compiled Kotlin classes in the Maven/AAR dependency, not from the model file.

Changing the model checkpoint/export cannot repair a Kotlin compiler-vs-library metadata mismatch.

## Strong hypotheses

### H1 — the app owns too much of the LiteRT-LM dependency graph

The app does not compile against LiteRT-LM APIs directly. litertlm-bridge is the Java-facing compile boundary and already uses compileOnly(litertlm-android).

Therefore the app should not need LiteRT-LM's Kotlin dependency graph on its Kotlin CompileClasspath merely to package the native runtime.

A clean candidate is bridge = compileOnly LiteRT-LM, app = runtimeOnly LiteRT-LM, with no direct LiteRT-LM API imports from app Kotlin source. This is an experiment, not yet an accepted refactor.

### H2 — productive failure remains at the classloader/native-loader boundary

Because both the APK library and expected JNI symbol exist, the next question is not where the file is, but whether this exact native image was loaded into the same Android namespace/classloader as the exact NativeLibraryLoader class.

Candidates include duplicate LiteRT-LM classes, dependency revision skew, native linker failure before JNI registration, ABI/packaging differences, or classloader differences between harness and productive APK.

### H3 — nativeCheckLoaded may be a symptom, not the final failure

LiteRT-LM's own loader calls nativeCheckLoaded() as an already-loaded probe and catches UnsatisfiedLinkError before attempting System.loadLibrary and resource extraction.

Therefore a log line saying No implementation found for nativeCheckLoaded does not by itself prove that the final load() operation failed at that exact call.

The complete log sequence is required to distinguish initial probe miss, successful System.loadLibrary, linker failure, extraction failure, and final loader failure.

## Official-source evidence

LiteRT-LM v0.17.1 is an official release and its release notes describe it as a tool-call integer bug-fix release.

The official NativeLibraryLoader source implements the three-stage already-loaded probe, Android library-path load, and extraction fallback.

The official native source exports Java_com_google_ai_edge_litertlm_NativeLibraryLoader_nativeCheckLoaded.

The official Android Kotlin guide recommends the litertlm-android Maven dependency and background initialization of Engine.

## What is not justified yet

- Do not change the .litertlm model merely to fix Kotlin metadata.
- Do not globally upgrade Kotlin to 2.4.0 without a complete toolchain matrix.
- Do not add arbitrary System.loadLibrary calls to application code.
- Do not add another semantic fallback or parser.
- Do not bypass the production bridge.
- Do not make device-specific JNI decisions.

## Controlled next experiments

### Experiment 1 — dependency boundary

Temporarily move the app direct LiteRT-LM dependency from implementation to runtimeOnly while keeping the bridge compileOnly dependency.

Measure: app CompileClasspath no longer contains kotlin-reflect 2.4.0; app Kotlin compilation; APK still packages liblitertlm_jni.so; runtime class availability; CR-9 instrumentation behavior.

If this passes, the compile blocker is an application dependency-boundary problem, not a reason to upgrade Kotlin globally.

### Experiment 2 — exact APK class/native pairing

Inspect the built APK and dependency packaging for duplicate LiteRT-LM classes or native libraries. Compare the Java class and JNI pair against the exact 0.17.1 AAR contents.

### Experiment 3 — loader sequence evidence

Run CR-9 with LiteRT-LM loader debug logging enabled and capture the complete sequence around nativeCheckLoaded, System.loadLibrary, extraction and engine.initialize.

### Experiment 4 — native linker evidence

If System.loadLibrary fails, capture Android linker output and the exact missing dependency or symbol. Do not infer a missing native dependency from the Java nativeCheckLoaded message alone.

### Experiment 5 — classloader identity

Capture the classloader and code-source location of NativeLibraryLoader and compare it with the validated harness. The loaded native library must correspond to the same dependency revision.

## Refactor decision tree

1. If runtimeOnly fixes compile without changing runtime behavior: keep Kotlin 2.1.21 and repair dependency ownership.
2. If runtimeOnly compiles but runtime still fails: continue JNI/classloader/linker investigation; do not touch the model.
3. If LiteRT-LM 0.17.1 is proven incompatible with the current Android stack: evaluate an adjacent LiteRT-LM version using the same canonical model only if that version supports the same model/runtime contract.
4. Only consider a global Kotlin upgrade after proving the complete plugin/KSP/Compose/Room/toolchain matrix.
5. Only reconsider the model/export after the runtime stack is proven healthy and a model-specific failure remains.

## Current conclusion

The evidence currently points to two separate integration defects: a real compile-classpath dependency-boundary problem caused by LiteRT-LM 0.17.1 bringing Kotlin 2.4.0 into an application compiled with Kotlin 2.1.21; and an unresolved productive Android JNI/classloader/loading problem despite the exact 0.17.1 JNI symbol being present in both AAR and APK.

The model itself is not implicated by the Kotlin metadata error, and CR-8 already proves the canonical model can execute through LiteRT-LM's native path.

No production refactor has been accepted yet. The next checkpoint is controlled evidence for the dependency boundary and the complete native-loader sequence.

## New evidence — controlled dependency-boundary experiment

2026-10-05: the app's direct LiteRT-LM dependency was changed from implementation to runtimeOnly for a controlled experiment. The bridge remains compileOnly(litertlm-android), preserving LiteRT-LM API compilation inside the Java bridge while keeping the app Kotlin CompileClasspath independent from LiteRT-LM's Kotlin metadata.

Results:
- debugCompileClasspath no longer contains kotlin-reflect;
- :app:compileDebugKotlin PASS;
- :app:assembleDebug PASS;
- APK still packages lib/arm64-v8a/liblitertlm_jni.so and lib/x86_64/liblitertlm_jni.so;
- a temporary productive-APK instrumentation probe invoking NativeLibraryLoader.load() by reflection PASSed.

This is strong evidence that the app's compile boundary was incorrectly exposing LiteRT-LM's Kotlin dependency graph. A global Kotlin upgrade is not required to restore compilation.

## Important correction to the JNI diagnosis

The productive log still contains:
No implementation found for void com.google.ai.edge.litertlm.NativeLibraryLoader.nativeCheckLoaded()

This message is emitted by the first already-loaded probe inside LiteRT-LM's own loader. The loader catches UnsatisfiedLinkError and then attempts System.loadLibrary("litertlm_jni"). The dedicated loader probe passed in the productive APK, so this message is not sufficient evidence of a failed JNI load.

This materially changes the diagnosis: the old CR-9 interpretation treated the probe message as the terminal failure. It is actually compatible with a normal first-load sequence.

## What remains open

The productive UI E2E test with a manually installed canonical CR-7.4 model progressed beyond model activation and UI quest generation, but its final memory assertion observed only about 130–134 MB PSS and failed the historical >600 MB assertion. That does not prove native inference failure: the current production composer has admission/resource governance and may reject or fall back before full model residency. No claim of productive semantic ToolCall success is made from this test.

Therefore CR-9 is not closed yet. The remaining question is now narrower: prove that the productive AppGraph actually executes the canonical local inference path and receives the native ToolCall, rather than merely constructing the runtime and/or converging to procedural generation.

## Refactor decision after this checkpoint

The evidence now supports keeping Kotlin 2.1.21 and LiteRT-LM 0.17.1, with the app dependency owned as runtimeOnly and the bridge owning compileOnly access to LiteRT-LM APIs.

A model swap is not indicated. A global Kotlin upgrade is not indicated. A LiteRT-LM version change is not indicated by the current evidence.

The remaining CR-9 work should focus on production inference-path observability and the admission/resource gate, not on JNI symbol discovery or model/Kotlin compatibility.


## Decisive CR-9 finding — LiteRT-LM filename contract — 2026-10-05

The causal failure is now identified and reproduced as a controlled A/B.

### Reproduction

The canonical CR-7.4 artifact is identical in both tests:

- SHA-256: e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb
- Size: 284,692,656 bytes
- LiteRT-LM runtime: 0.17.1
- Same productive APK / same Android runtime / same x86_64 emulator

Only the filesystem filename changed:

1. /data/user/0/com.pinhoquest/files/models/cr74_semantic_isolation/1/model
   - Engine.initialize() failed with:
     LiteRtLmJniException: Failed to create engine: INVALID_ARGUMENT: Unsupported or unknown file format.
   - elapsed inference attempt was approximately 15–20 ms.

2. /data/user/0/com.pinhoquest/files/models/cr74_semantic_isolation/1/model.litertlm
   - the exact same bytes initialized successfully;
   - P3NativeToolCallE2ETest passed with OK (1 test);
   - elapsed test time was approximately 39.7 s;
   - the native ToolCall contract was accepted.

This isolates the failure to the LiteRT-LM model filename/format-detection boundary, not JNI symbol availability, ABI, model bytes, Kotlin metadata, or model semantics.

### Root cause

AndroidModelStore previously persisted every installed model under the extensionless filename model. The LiteRT-LM runtime accepts the canonical .litertlm artifact when its .litertlm filename is preserved, but rejects the same bytes under the extensionless path as an unsupported/unknown file format.

The previous CR-9 JNI interpretation is therefore superseded. NativeLibraryLoader.nativeCheckLoaded() was only the loader's initial already-loaded probe and was not the terminal failure.

### Corrective implementation

AndroidModelStore now:

- persists LiteRT-LM models as model.litertlm;
- derives the filename from manifest.runtimeFormat;
- migrates an existing legacy model file to model.litertlm before returning the active model;
- keeps the migration as a storage-contract migration, not an inference fallback or parser bypass.

Tests now cover canonical filename persistence and legacy filename migration.

### Validation

- :android-data:testDebugUnitTest PASS.
- :quest-core:test PASS.
- :app:compileDebugKotlin PASS.
- :app:assembleDebug PASS.
- :app:assembleDebugAndroidTest PASS.
- Productive CR9ProductionModelE2ETest PASS (OK (1 test)) after the filename correction.
- Direct canonical native ToolCall test with .litertlm path PASS.
- The exact extensionless-vs-.litertlm A/B provides the causal proof.

### Version decision

No model swap.
No global Kotlin upgrade.
No LiteRT-LM version change.
No arbitrary System.loadLibrary.
No semantic fallback or parser bypass.

CR-9 is technically resolved at the identified Android model-storage/runtime boundary, but the production-model E2E gate is not closed.

## Production Model E2E evidence — 2026-10-05

After the filename correction, the canonical model was staged and installed through the productive `AndroidModelStore` contract. The active device state was verified as:

- `files/models/cr74_semantic_isolation/1/model.litertlm`
- SHA-256 `e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb`
- size `284,692,656` bytes
- `runtimeFormat=litertlm`
- `.active` points to version `1`.

A fresh debug APK was then exercised through `CR9ProductionModelE2ETest` with `clearPackageData=false`, so the active external model remained available.

Observed sequence:

1. PinhoQuest started under Android instrumentation.
2. MainActivity launched normally.
3. LiteRT-LM emitted the expected first `nativeCheckLoaded()` probe message; this was followed by successful native environment creation.
4. LiteRT/XNNPACK initialized the incoming `model.litertlm` and created the XNNPACK cache.
5. The productive generation path reached `LiteRtLmRuntime.ensureInitialized()` -> `LiteRtLmRuntime.generate()` -> `AndroidLiteRtLmInferencePort.generate()`.
6. The app then terminated with `Fatal signal 6 (SIGABRT)` in thread `DefaultDispatch`.
7. Tombstone frames point into `liblitertlm_jni.so`; frame #24 is `Java_com_google_ai_edge_litertlm_LiteRtLmJni_nativeCreateEngine+2620`, called from `com.google.ai.edge.litertlm.Engine.initialize()`.
8. The instrumentation result was `shortMsg=Process crashed`.

This is the first fresh evidence of an actual native-engine crash in the productive path. It is materially different from the historical PSS assertion failure and from the earlier `nativeCheckLoaded()` probe message.

### CR-9.1 — Deep native abort diagnosis — 2026-10-05

### Causal finding

The `SIGABRT` observed in the first production-model run was **not caused by the model filename, JNI loading, Engine configuration, or the `.litertlm` artifact itself**. The immediate native failure was storage exhaustion while LiteRT/XNNPACK was building its weight cache.

The decisive log sequence was:

1. `Flatbuffer model initialized directly from incoming litert model.`
2. XNNPACK created the CPU delegate and began writing the weight cache under `cache/litertlm/`.
3. The emulator had only approximately **114–118 MB free** on `/data`.
4. XNNPACK emitted:
   - `XNNPack weight cache: cannot append buffer to cache file`
   - `Inserting data in the cache failed.`
5. Immediately afterwards the process received `Fatal signal 6 (SIGABRT)` in `DefaultDispatch`.
6. The tombstone entered `liblitertlm_jni.so` through `Java_com_google_ai_edge_litertlm_LiteRtLmJni_nativeCreateEngine`.
7. The emulator also reported `tombstoned: failed to create temporary tombstone ... No space left on device`.

The native stack therefore identified the **location of the abort**, but the preceding XNNPACK cache error identifies the **causal trigger**.

### Controlled reproduction / falsification

The active canonical model remained intact:

- path: `files/models/cr74_semantic_isolation/1/model.litertlm`
- SHA-256: `e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb`
- size: `284,692,656` bytes.

The emulator had accidentally accumulated duplicate copies of the 285 MB model during the Gate setup. The active model was retained while the staged/local duplicates were removed and Android cache trimming was run.

Storage changed from approximately **114 MB free / 99% used** to approximately **1.1 GB free / 85% used**.

The exact same debug APK, exact same active model and exact same LiteRT-LM bridge were then exercised again.

### Result after storage recovery

The isolated native production transport test:

`P3NativeToolCallE2ETest#realModelReturnsExactlyOneCanonicalToolCall`

completed with:

`1 tests, 0 failed, 0 ignored`

No `SIGABRT` occurred. The run reached the XNNPACK-delegated model and completed the native tool-call contract. This is the strongest A/B evidence currently available:

`low storage -> XNNPACK cache append failure -> SIGABRT`

versus

`~1.1 GB free -> same model/bridge -> native ToolCall PASS`.

### Production UI path status

The production UI test subsequently ran without the previous native abort, but its current instrumentation assertion timed out while waiting for `COMEÇAR QUEST`. The timeout observed in that run was **15 seconds**, i.e. the pre-existing test artifact was still being executed before the timeout-only test update was rebuilt.

The isolated native test took roughly 16 seconds end-to-end from test start to completion, so a 15-second UI wait is not a valid latency gate for this model. The diagnostic test was therefore not accepted as a production semantic failure.

A test-only timeout extension to 30 seconds was rebuilt into the instrumentation APK and re-run. The productive `CR9ProductionModelE2ETest#productiveUiGeneratesWithCanonicalCr74Model` completed with `1 tests, 0 failed, 0 ignored` and no native abort. The full production path therefore crossed the UI generation boundary successfully after storage recovery.

### CR-9.1 decision

**CR-9.1 = DIAGNOSIS PASS / CAUSE IDENTIFIED.**

The native abort is explained by emulator storage exhaustion during XNNPACK weight-cache construction. No production runtime workaround is warranted from this finding.

### CR-9 closure

CR-9 is now **E2E PASS** on the controlled debug/emulator run.

Observed productive chain:

`model.litertlm -> Engine.initialize -> Conversation -> native ToolCall -> MicroQuestToolCallDecoder -> MicroQuestText -> QuestValidator -> Quest`

The final production UI test completed with `1 tests, 0 failed, 0 ignored` after the emulator was given sufficient storage and the test timeout was raised to 30 seconds to accommodate the observed model initialization/generation latency.

Operational precondition: this 285 MB model plus XNNPACK cache requires substantial free emulator storage; **~1 GB free is the minimum diagnostic baseline used for this Gate, with more headroom preferred**. This is an environment precondition, not a production inference workaround.

P5.5 is now unblocked from the CR-9 dependency.
