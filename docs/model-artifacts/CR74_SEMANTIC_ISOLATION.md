# CR-7.4 semantic-isolation LiteRT-LM artifact

This file is the provenance and recovery record for the only model package that
the current PinhoQuest production catalog may install. It is not committed to
Git and is intentionally not packaged into the normal APK.

## Immutable artifact identity

| Field | Value |
| --- | --- |
| Model id / version | `cr74_semantic_isolation` / `1` |
| Runtime format | `litertlm` |
| Distribution | GitHub Release tag `cr74-semantic-isolation-v1`, asset `model.litertlm` |
| Release URL | `https://github.com/sasandralean-prog/PinhoQuest/releases/download/cr74-semantic-isolation-v1/model.litertlm` |
| Size | `284692656` bytes |
| SHA-256 | `e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb` |
| Backend | CPU |
| Context limit | 1280 |
| Canonical installed filename | `files/models/cr74_semantic_isolation/1/model.litertlm` |

The release asset was recovered byte-for-byte from the PinhoQuest-owned release
`p6-local-debug-bundled-cr74-2026-10-06` (`app-debug.apk`). That source APK is
303410971 bytes with SHA-256
`afb8a489bc34306c95765dcbb687ab0ef0de96c3531e69eede89c20b337e639d`; its
`assets/model.litertlm` entry has the identity recorded above. The original
workstation path referenced by historical CR-7.4 evidence is not required for
a clean checkout and was not available during this recovery.

## Contents and required components

`model.litertlm` is the self-contained LiteRT-LM bundle consumed by the
runtime; the implementation does not reference a separate model shard,
tokenizer, vocabulary, prompt-template file, or native library beside it.

The Android runtime is resolved from Maven by the existing Gradle catalog as
`com.google.ai.edge.litertlm:litertlm-android:0.16.1`. The app owns it as a
`runtimeOnly` dependency, while `:litertlm-bridge` is the sole module importing
its API at compile time. The normal debug APK contains its JNI libraries but
does **not** contain the model package.

The app presents an explicit confirmation before it schedules the download.
`ModelDownloadWorker` writes a resumable temporary file, validates the expected
byte count, and delegates SHA-256 validation and atomic promotion to
`AndroidModelStore`. A failed, interrupted, incompatible, or hash-mismatched
package is never made active.

## License and provenance conditions

CR-7.4 is documented as a PinhoQuest fine-tune/export based on
`google/functiongemma-270m-it`. FunctionGemma is a Gemma model derivative under
the [Gemma Terms of Use](https://ai.google.dev/gemma/terms). The base terms
permit distribution of model derivatives only with their use restrictions,
a copy of the terms, notices for modified files, and the required Notice text.

The exact required Notice text is included in
[`GEMMA_NOTICE.txt`](GEMMA_NOTICE.txt), and the release contains the same file.
Anyone distributing this artifact must provide the current Gemma Terms of Use
and the corresponding use restrictions to recipients. The repository records
the base-model provenance and binary integrity, but does not independently
audit rights for data used to fine-tune CR-7.4; downstream distributors must
ensure their own use and redistribution remain compliant.

## Clean-checkout recovery and verification

1. Download the exact release asset above. Do not use an APK, a cache, or a
   similarly named model as a substitute.
2. Verify it before use:

   ```powershell
   ./tools/verify_cr74_model.ps1 -Path ./model.litertlm
   ```

3. Build the application normally. The model is installed only after a user
   explicitly accepts the model notice in **Configurações** and starts the
   download. No model is silently fetched at app startup.
4. To inspect a built APK, confirm it packages LiteRT-LM JNI libraries and does
   not package `assets/model.litertlm` unless a distribution build intentionally
   changes that policy.

The installer uses the pinned release URL and rejects bytes that do not match
this document. A release-asset download is chosen instead of Git LFS because
the model is 284.7 MB, exceeds normal GitHub Git blob limits, and needs a
versioned distribution endpoint rather than checkout-time weight retrieval.

## Validation boundary

Historical CR-9 evidence records device-native ToolCall and productive UI
success with this exact hash after sufficient emulator storage was available.
That is prior evidence, not a claim that the current checkout has executed
inference. The recovery validates release identity, binary integrity, catalog
metadata, Gradle packaging, and unit/build gates separately; run the existing
instrumentation tests on an online device/emulator with adequate free storage
before asserting a fresh inference result.
