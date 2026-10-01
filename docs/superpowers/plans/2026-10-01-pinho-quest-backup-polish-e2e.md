# Pinho Quest Backup, Polish and V1 E2E Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish V1 with portable/private backups, safe restore, appearance/accessibility settings, humanized error states, and complete end-to-end validation.

**Architecture:** Backup is a validated snapshot derived from Room, never a second live database. Restore stages and validates before a single Room transaction; dataset revision prevents silent rollback. UI polish consumes domain/runtime outcomes without changing authorities.

**Tech Stack:** Kotlin serialization, ZIP-compatible `.pqbackup` container, WorkManager, SAF persistable URI permissions, Room, DataStore, Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`

## Global Constraints

- Room remains the only live domain truth.
- `.pqbackup` is portable/versioned domain data, not a raw SQLite dump.
- Model binary is excluded from backup.
- Catalog packs and evidence are included so a restored personalized garden remains the same garden.
- Older backup revision never overwrites newer local progress silently.
- V1 does not auto-merge divergent histories.
- Public backup uses SAF persistable URI permission.
- Theme supports System/Light/Dark; text/accent customization must preserve readability.
- User-facing failures remain warm and truthful.

## Review Focus

- Corrupt/truncated backup must fail before any local mutation.
- Same-profile older revision must default to cancel and require explicit rollback confirmation.
- Restore interrupted before commit must leave the old Room dataset intact.
- Revoked/unavailable SAF permission must not affect private Room state or claim backup success.
- Restored CatalogPacks must match original IDs/entries/evidence exactly and must not be regenerated from the internet.

---

### Task 1: Portable backup contracts

**Files:**
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/backup/BackupModels.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/backup/BackupCodec.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/backup/BackupCodecTest.kt`

**Interfaces:**
- `data class BackupManifest(formatVersion, schemaVersion, profileId, datasetRevision, createdAt, appVersion, catalogVersions, integrityHash)`
- `BackupCodec.encode(snapshot: BackupSnapshot): ByteArray`
- `BackupCodec.decode(bytes: ByteArray): BackupReadResult`

- [ ] **Step 1: Write failing round-trip tests** covering profile, quests/history, tags, XP, goals, catalogs/evidence, game catalog/history, preferences, and checksum metadata.
- [ ] **Step 2: Add tests** for corrupt, truncated, and checksum-mismatch inputs yielding `InvalidBackup` without partial data.
- [ ] **Step 3: Run** tests. **Expected:** FAIL.
- [ ] **Step 4: Implement** versioned container codec; model binaries are never serialized.
- [ ] **Step 5: Run** tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add portable backup format`.

### Task 2: Snapshot builder and private backup rotation

**Files:**
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/backup/RoomBackupSnapshotBuilder.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/backup/PrivateBackupTarget.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/backup/BackupCoordinator.kt`
- Test: `app/src/test/kotlin/com/pinhoquest/backup/BackupCoordinatorTest.kt`

**Interfaces:**
- `BackupCoordinator.createSnapshot(targets: Set<BackupTarget>): BackupResult`
- Private snapshot rotation is bounded and uses atomic temp→final promotion.

- [ ] **Step 1: Write tests** for unchanged-dataset no-op, new-revision snapshot, bounded rotation, failed private write, and non-blocking quest completion.
- [ ] **Step 2: Run** tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** snapshot builder/coordinator and private target.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add private recovery snapshots`.

### Task 3: SAF public backup target

**Files:**
- Create: `app/src/main/kotlin/com/pinhoquest/backup/SafBackupTarget.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/backup/PersistableBackupPermissionStore.kt`
- Modify: `app/src/main/kotlin/com/pinhoquest/ui/settings/SettingsScreen.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/backup/SafBackupFlowTest.kt`

**Interfaces:**
- Produces public-folder selection, persisted URI grant, and `PinhoQuest-backup-r<revision>.pqbackup` export.

- [ ] **Step 1: Write tests** for folder selection, persisted grant, revoked grant, unavailable document provider, and successful export naming.
- [ ] **Step 2: Run** tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** SAF target/settings; failure copy states that the local garden remains saved.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add SAF public backup target`.

### Task 4: Restore planner and staged transaction

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/backup/RestorePlanner.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/backup/RoomRestoreService.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/settings/RestoreBackupDialog.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/backup/RestorePlannerTest.kt`
- Test: `android-data/src/androidTest/kotlin/com/pinhoquest/data/backup/RoomRestoreServiceTest.kt`

**Interfaces:**
- `RestorePlanner.plan(local: DatasetMetadata?, incoming: BackupManifest): RestorePlan`
- Plans: `FreshInstall`, `NewerBackup`, `OlderSameProfileRequiresRollbackConfirmation`, `DifferentProfileRequiresConfirmation`, `RejectInvalid`.
- `RoomRestoreService.apply(staged: StagedRestore): RestoreResult`

- [ ] **Step 1: Write planner tests** for empty local state, newer/older same profile, different profile, identical revision, invalid schema.
- [ ] **Step 2: Write transaction test** injecting failure before commit and proving the original database remains intact.
- [ ] **Step 3: Write exact-catalog restore test** asserting IDs/entries/evidence restore without web regeneration.
- [ ] **Step 4: Run** tests. **Expected:** FAIL.
- [ ] **Step 5: Implement** staged validation/migration and one-transaction restore.
- [ ] **Step 6: Run** tests. **Expected:** PASS.
- [ ] **Step 7: Commit** `feat: add safe staged backup restore`.

### Task 5: Appearance, accessibility and settings polish

**Files:**
- Create: `app/src/main/kotlin/com/pinhoquest/ui/theme/PinhoQuestTheme.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/theme/ReadableColorPolicy.kt`
- Modify: `app/src/main/kotlin/com/pinhoquest/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/kotlin/com/pinhoquest/ui/navigation/PinhoQuestNav.kt`
- Test: `app/src/test/kotlin/com/pinhoquest/ui/theme/ReadableColorPolicyTest.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/ui/AccessibilityRegressionTest.kt`

**Interfaces:**
- Produces System/Light/Dark, accent controls, safe text-color handling, font-size control, garden-name edit, research/model preferences.

- [ ] **Step 1: Write tests** for unsafe contrast adjustment and persistence of theme/font/accent preferences.
- [ ] **Step 2: Write UI tests** at large font/display scale for every main screen and bottom navigation.
- [ ] **Step 3: Run** tests. **Expected:** FAIL.
- [ ] **Step 4: Implement** theme/settings policy and remove any remaining fixed-height text containers.
- [ ] **Step 5: Run** tests/lint. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: polish appearance and accessibility`.

### Task 6: Humanized error and empty-state audit

**Files:**
- Modify: `app/src/main/kotlin/com/pinhoquest/ui/copy/UserFacingCopy.kt`
- Create: `app/src/test/kotlin/com/pinhoquest/ui/copy/UserFacingCopyTest.kt`
- Create: `docs/evidence/v1-copy-audit.md`

**Interfaces:**
- Maps semantic quest/inference/research/backup outcomes to user-facing language; technical details remain optional diagnostics.

- [ ] **Step 1: Write tests** asserting primary UI strings do not contain internal class names, exception names, raw status-only errors, `provider`, `Room`, or `inference`.
- [ ] **Step 2: Add mappings** for offline game-empty, research failure with stored options, model fallback, public-backup failure, invalid restore, and abandoned quest.
- [ ] **Step 3: Run** tests. **Expected:** PASS after mappings are complete.
- [ ] **Step 4: Document** the copy audit and commit `docs: audit V1 user-facing language`.

### Task 7: Full V1 automated gate

**Files:**
- Modify: `.github/workflows/android-ci.yml`
- Create: `docs/checkpoints/V1-final-automated.md`

- [ ] **Step 1: Run** `./gradlew clean test lintDebug assembleDebug` plus all connected instrumentation suites. **Expected:** all green.
- [ ] **Step 2: Verify CI** covers JVM tests, Android tests, Room migrations, parser fixtures, lint, build, and feasible emulator suites.
- [ ] **Step 3: Record** test counts, schemas, selected model/runtime, and bounded external-source limitations.
- [ ] **Step 4: Commit** `docs: record final automated validation`.

### Task 8: Full V1 Android E2E

**Files:**
- Create: `docs/checkpoints/V1-final-e2e.md`
- Modify: `README.md`
- Modify: `Engineering_Genome/00_START_HERE.md` only to update frontier/status.

- [ ] **Step 1: Fresh install** → onboarding → `Jardim de Rafa` → tags → procedural quest → completion → XP → unique flower.
- [ ] **Step 2: Install local model** → healthy-resource local generation; then pressure case → silent procedural fallback.
- [ ] **Step 3: Online game research** → bounded catalog → Game Quest; disable network → another Game Quest from stored catalog.
- [ ] **Step 4: Complete/seed first collection** in a controlled profile → research expansion → immutable next pack → deferred reward resolution.
- [ ] **Step 5: Configure/export public backup** → fresh data → restore → verify exact profile, owner name, XP, tags, histories, catalogs, acquisitions, preferences.
- [ ] **Step 6: Attempt older same-profile restore** and verify default cancellation plus explicit rollback warning.
- [ ] **Step 7: Repeat primary flows** under large font/display scale and light/dark themes.
- [ ] **Step 8: Record** device/API, commit SHA, APK SHA-256, model ID/version, network transitions, backup hashes, and deviations.
- [ ] **Step 9: Update README/Genome status** only after every mandatory E2E step passes or is explicitly bounded with evidence.
- [ ] **Step 10: Commit** `docs: validate Pinho Quest V1 end to end`.