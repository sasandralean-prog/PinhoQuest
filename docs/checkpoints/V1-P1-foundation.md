# V1-P1 — Foundation Checkpoint

**Status:** validated_bounded  
**Date:** 2026-10-01  
**Branch:** `feature/p1-foundation`  
**Validated product baseline:** `ebc4d02d99ae266c500b3db51377f35a9e91f0e3`

## Objective

Validate the first usable Android vertical slice without crossing into P2+ responsibilities:

- pure Kotlin/JVM domain and core;
- Android adapter boundary;
- Room v1 and DataStore;
- onboarding and `Jardim de <nome>` identity;
- system/user tags baseline;
- one canonical procedural Quest flow;
- session start/abandon persistence;
- Quests / Tags / Jardim / Configurações shell;
- humanized user-facing failure copy.

## Automated evidence

Final consolidated command:

```powershell
.\gradlew.bat test lintDebug assembleDebug :android-data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Result: **BUILD SUCCESSFUL**.

Test inventory at the final gate:

| Suite | Tests | Failures | Skipped |
| --- | ---: | ---: | ---: |
| `quest-domain` JVM | 7 | 0 | 0 |
| `quest-core` JVM | 16 | 0 | 0 |
| `app` connected | 6 | 0 | 0 |
| `android-data` connected | 5 | 0 | 0 |
| **Total** | **34** | **0** | **0** |

Lint: green for `app` and `android-data`.  
Debug APK: built successfully.

APK SHA-256:

```text
305f11f70ae13b8b8e124581b7867704fa9e440150028ad7754339316bc53f55
```

## Runtime device

```text
AVD: Pixel_4_API_33
Model: sdk_gphone64_x86_64
Android: 13
SDK: 33
Physical size: 1080x2280
Physical density: 440
Final font scale: 1.0
```

## Real activity E2E

`FoundationActivityE2ETest` executes the real `MainActivity`, Room and DataStore rather than an isolated composable.

Validated flow:

```text
fresh state
→ onboarding
→ owner name "Rafa"
→ select Programação tag
→ Quests home
→ Tags
→ Sortear Quest
→ Frankenstein Digital
→ Começar Quest
→ active session
→ abandon
→ close activity
→ relaunch
→ profile restored from Room
→ onboarding does not repeat
```

The same E2E passed with Android system `font_scale=1.4`. The test explicitly scrolls to actions that naturally move below the viewport; no text-bearing card relies on a fixed height.

A separate Compose regression test also renders the Quest home at `fontScale=2.0`.

The AVD was restored to `font_scale=1.0` after the large-font gate.

## TDD / regression evidence

Important RED observations captured during P1:

- architecture smoke test failed before the Gradle/module scaffold existed;
- domain contract tests failed before Quest/Profile/Tag types existed;
- Quest engine tests failed before canonical planner/composer/validator existed;
- Room instrumentation failed before persistence entities/repositories existed;
- Task 5 failed on missing session service/ports before the application service was implemented;
- Task 6 UI tests failed because the Compose/navigation surface did not exist;
- activity E2E initially failed to compile because its harness used unsupported test helpers;
- large-font E2E initially timed out because the test did not scroll to below-fold controls; the product already exposed a scrollable layout, so the harness was corrected to model real interaction.

## Architecture rulings made during P1

1. `QuestSessionService` returns semantic `SessionCommandResult<T>` outcomes instead of throwing or returning raw entities for illegal transitions.
2. Room repositories implement core ports; core/domain remain Android-free.
3. Room/DataStore construction stays inside `android-data` via `AndroidDataGraph`; the app does not promote those implementation dependencies into its public architecture.
4. NORMAL/GAME/RANDOM remain policies of the same engine. In P1, GAME has no researched candidate source yet and therefore returns a friendly unavailable result rather than silently becoming NORMAL.
5. Large text uses scroll/adaptive content instead of clipping via fixed card heights.
6. System tag definitions and tag→QuestCategory affinity mapping live in `quest-core`; UI renders the catalog but is not its authority.
7. Garden profiles receive generated UUID identities persisted in Room; installations no longer share a hardcoded profile ID.

## Native final-review hardening

The final whole-branch Native review identified four Important issues and fixed each with a RED→GREEN regression:

- concurrent `accept()` calls could race into different session identities; mutating session commands are now serialized and the concurrency test proves one canonical session;
- system-tag affinity policy lived under `ui.tags`; it moved to `quest-core` as `SystemTagCatalog`;
- onboarding's visual 20-character check counted UTF-16 code units while the domain counted Unicode code points; a 20-emoji regression now passes consistently;
- `profileId = "local-profile"` would make unrelated gardens indistinguishable during future backup/restore; profile IDs are now generated UUIDs and the current profile is recovered canonically from Room.

Post-review consolidated gate:

```powershell
.\gradlew.bat test lintDebug assembleDebug :android-data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Result: **BUILD SUCCESSFUL**, 34 tests, 0 failures, 0 skipped.

## Known bounded limitations

These are intentional frontier boundaries, not hidden P1 failures:

- **P2:** no XP ledger, Goals, flower rewards, real Garden catalog or completion/reward transaction yet.
- **P3:** no local LLM model/runtime/admission controller yet; procedural composer is the only composer.
- **P4:** no integrated game/flower web research yet; Game Quest has no candidate source in P1.
- **P5:** no public/private backup workflow, restore pipeline or final appearance/accessibility polish yet.
- The Garden tab is intentionally a humanized placeholder until P2.
- Settings expose only the baseline theme/font controls required to establish DataStore ownership.

## Conclusion

P1 is **validated_bounded** against its approved implementation plan. The Foundation can now support P2 without moving Quest, persistence or UI authority into parallel flows.
