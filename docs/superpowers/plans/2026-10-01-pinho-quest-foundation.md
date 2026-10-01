# Pinho Quest Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first usable Android vertical slice: onboarding, profile/tags, procedural quest generation, quest lifecycle, four-tab navigation, Room/DataStore persistence, and humanized UI without LLM or external research yet.

**Architecture:** Create pure Kotlin/JVM `quest-domain` and `quest-core` modules, an Android persistence adapter module, and a Compose `app` module. All UI actions flow through application/core services; Room is the first live persistence authority.

**Tech Stack:** Kotlin/JVM, Android Gradle Plugin, Jetpack Compose Material 3, Room, DataStore Preferences, kotlinx-coroutines/Flow, JUnit, AndroidX test, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`

## Global Constraints

- Android first; core/domain must not import Android SDK classes.
- Package root: `com.pinhoquest`.
- Main tabs: Quests, Tags, Jardim, Configurações; Quests is default.
- Garden owner name is trimmed and 1–20 characters.
- NORMAL/GAME/RANDOM are policies over one Quest flow, not separate runtimes.
- Quest is promoted only by `QuestValidator`.
- Procedural composer is always available.
- Room is the only live domain-state authority; DataStore stores UI/runtime preferences only.
- Human-facing copy must not expose technical internal terms.

## Review Focus

- Empty/whitespace and >20-character garden names are rejected without corrupting onboarding state.
- Process recreation during an active quest restores the same canonical session instead of creating a second one.
- Repeated taps on Start/Abandon are idempotent at the application boundary.
- RANDOM reduces preference weighting but still respects hard platform/time constraints.
- Accessibility-large font/display scaling does not clip tab labels, quest objectives, or primary buttons.

---

### Task 1: Repository and module scaffold

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `.gitignore`
- Create: `quest-domain/build.gradle.kts`, `quest-core/build.gradle.kts`, `android-data/build.gradle.kts`, `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`, `.github/workflows/android-ci.yml`
- Test: `quest-domain/src/test/kotlin/com/pinhoquest/domain/ArchitectureSmokeTest.kt`

**Interfaces:**
- Produces Gradle modules `:quest-domain`, `:quest-core`, `:android-data`, `:app` and package root `com.pinhoquest`.

- [ ] **Step 1: Write the failing test** `ArchitectureSmokeTest.domainModuleLoads()` asserting `DomainModuleMarker.id == "pinho-quest-domain"`.
- [ ] **Step 2: Run** `./gradlew :quest-domain:test`. **Expected:** FAIL because `DomainModuleMarker` does not exist.
- [ ] **Step 3: Add** the four-module build and `quest-domain/src/main/kotlin/com/pinhoquest/domain/DomainModuleMarker.kt`.
- [ ] **Step 4: Run** `./gradlew :quest-domain:test :quest-core:test :android-data:testDebugUnitTest :app:assembleDebug`. **Expected:** PASS/build success.
- [ ] **Step 5: Add CI** for JVM tests, Android unit tests, lint, and `assembleDebug`; run the same commands locally.
- [ ] **Step 6: Commit** `build: scaffold Pinho Quest Android architecture`.

### Task 2: Quest and profile domain contracts

**Files:**
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/quest/QuestModels.kt`
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/profile/ProfileModels.kt`
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/profile/GardenOwnerName.kt`
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/tag/TagModels.kt`
- Test: `quest-domain/src/test/kotlin/com/pinhoquest/domain/quest/QuestModelsTest.kt`
- Test: `quest-domain/src/test/kotlin/com/pinhoquest/domain/profile/GardenOwnerNameTest.kt`

**Interfaces:**
- `enum class QuestMode { NORMAL, GAME, RANDOM }`
- `enum class QuestState { GENERATED, ACCEPTED, ACTIVE, COMPLETED, ABANDONED, REJECTED }`
- `data class QuestRequest(val mode: QuestMode, val filters: QuestSessionFilters)`
- `@JvmInline value class GardenOwnerName`; `GardenOwnerName.create(raw: String): Result<GardenOwnerName>`
- `enum class TagSource { SYSTEM, USER, DERIVED }` and `data class Tag(...)`

- [ ] **Step 1: Write tests** for enum values, trimming, blank-name rejection, 20-character acceptance, and 21-character rejection.
- [ ] **Step 2: Run** `./gradlew :quest-domain:test`. **Expected:** FAIL.
- [ ] **Step 3: Implement** the domain types and the 1–20 trimmed-character owner-name rule.
- [ ] **Step 4: Run** `./gradlew :quest-domain:test`. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add quest profile and tag domain contracts`.

### Task 3: Canonical procedural Quest engine

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/quest/QuestPlanner.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/quest/ComposerPort.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/quest/ProceduralComposer.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/quest/QuestValidator.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/quest/QuestEngine.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/quest/QuestEngineTest.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/quest/QuestValidatorTest.kt`

**Interfaces:**
- `fun interface ComposerPort { suspend fun compose(plan: QuestGenerationPlan): QuestDraft }`
- `QuestPlanner.plan(request: QuestRequest, context: QuestContext): QuestGenerationPlan`
- `QuestValidator.validate(draft: QuestDraft): QuestValidationResult`
- `QuestEngine.generate(request: QuestRequest, context: QuestContext): QuestGenerationResult`

- [ ] **Step 1: Write tests** proving invalid title/objective/time/difficulty drafts never become `Quest` and GAME without a candidate does not silently become NORMAL.
- [ ] **Step 2: Add test** proving RANDOM lowers affinity weighting but preserves hard filters.
- [ ] **Step 3: Run** `./gradlew :quest-core:test`. **Expected:** FAIL.
- [ ] **Step 4: Implement** planner, procedural composer, validator, and engine through one canonical generation path.
- [ ] **Step 5: Run** tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add canonical procedural quest engine`.

### Task 4: Room v1 and DataStore boundaries

**Files:**
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/db/PinhoQuestDatabase.kt`
- Create entities/DAOs for `Profile`, `Tag`, `Quest`, `QuestSession` under `android-data/src/main/kotlin/com/pinhoquest/data/db/`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/repository/RoomProfileRepository.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/repository/RoomQuestRepository.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/settings/AppPreferencesStore.kt`
- Test: `android-data/src/androidTest/kotlin/com/pinhoquest/data/db/PinhoQuestDatabaseTest.kt`

**Interfaces:**
- Produces `ProfileRepository`, `TagRepository`, `QuestRepository`, `QuestSessionRepository`, `AppPreferencesStore`.
- Room starts at schema version `1`.

- [ ] **Step 1: Write instrumentation tests** for profile/tag/quest round trips and one-active-session restoration after database reopen.
- [ ] **Step 2: Run** `./gradlew :android-data:connectedDebugAndroidTest`. **Expected:** FAIL.
- [ ] **Step 3: Implement** Room entities/DAOs/repositories and DataStore preferences; keep theme/font/runtime settings out of Room.
- [ ] **Step 4: Export** Room schema v1 to `android-data/schemas/` and enable schema verification.
- [ ] **Step 5: Run** persistence tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add Room v1 and preference persistence`.

### Task 5: Quest-session application service

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/session/QuestSessionService.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/session/QuestSessionRepositories.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/session/QuestSessionServiceTest.kt`

**Interfaces:**
- `generate(request: QuestRequest): Quest`
- `accept(questId: QuestId): QuestSession`
- `start(sessionId: QuestSessionId): QuestSession`
- `abandon(sessionId: QuestSessionId): QuestSession`
- `reject(questId: QuestId, reason: RejectReason?): Unit`

- [ ] **Step 1: Write tests** for duplicate Start/Abandon calls and repository reload between calls.
- [ ] **Step 2: Run** core tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** idempotent transitions; illegal regressions return semantic failures instead of mutating state.
- [ ] **Step 4: Run** core tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add quest session application service`.

### Task 6: Compose onboarding, navigation, Quests and Tags

**Files:**
- Create: `app/src/main/kotlin/com/pinhoquest/PinhoQuestApplication.kt`, `MainActivity.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/navigation/PinhoQuestNav.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/onboarding/OnboardingScreen.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/quests/QuestScreen.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/tags/TagsScreen.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/garden/GardenPlaceholderScreen.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/copy/UserFacingCopy.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/ui/OnboardingQuestFlowTest.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/ui/LargeFontLayoutTest.kt`

**Interfaces:**
- Produces the four-tab shell, onboarding, procedural quest flow, editable tags, garden placeholder, and baseline settings.

- [ ] **Step 1: Write UI tests** for garden-name validation, first-run onboarding, default Quests tab, Sortear Quest, Quest Aleatória, and accessibility-large font.
- [ ] **Step 2: Run** connected UI tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** Material 3 screens with adaptive content height; no fixed-height text-bearing cards.
- [ ] **Step 4: Add** semantic error-to-human-copy mapping; never render exception/class names directly.
- [ ] **Step 5: Run** UI tests, lint, and `assembleDebug`. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add onboarding quest and tag UI`.

### Task 7: Foundation E2E gate

**Files:**
- Create: `docs/checkpoints/V1-P1-foundation.md`

- [ ] **Step 1: Run** `./gradlew test lintDebug assembleDebug`. **Expected:** all green.
- [ ] **Step 2: Verify on Android** fresh install → onboarding → tags → Sortear Quest → start → abandon → relaunch with coherent state.
- [ ] **Step 3: Repeat** under large font/display scaling and fix clipping before closing the checkpoint.
- [ ] **Step 4: Record** commit SHA, device/API, commands, test counts, and known limitations.
- [ ] **Step 5: Commit** `docs: validate Pinho Quest foundation`.