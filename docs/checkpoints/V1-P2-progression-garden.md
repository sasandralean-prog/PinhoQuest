# V1-P2 — Progression + Garden Checkpoint

**Status:** validated_bounded  
**Branch:** `feature/p2-progression-garden`  
**P1 fork baseline:** `a1d47d10a98d6b071ecf62fe0645badc838425b5`  
**Validated product HEAD:** `a8516c24ab4c8276f6fadfbd830237846b1b7dd7`  
**Date:** 2026-10-01

## Objective

Close P2 with real persisted progression and a usable Garden:

```text
QuestCompletion
├─ XP ledger / Lifetime / Spendable
├─ Goal evaluation
├─ RewardOpportunity
└─ unique FlowerAcquisition
        ↓
      Garden
```

P2 remains stacked on the open P1 branch. No merge to `main` is part of this checkpoint.

## Implemented authorities

- `XpLedgerSnapshot` derives Lifetime XP and Spendable XP from auditable transactions.
- `XpPolicyV1` owns quest XP awards.
- `LevelPolicyV1` derives level from Lifetime XP only.
- `GoalEvaluator` owns deterministic goal progression.
- `FlowerRarityScale` owns versioned logarithmic population classification.
- `RewardEngine` resolves one unique eligible flower without rarity-weighted drop odds.
- `QuestCompletionService` + `RoomQuestCompletionStore` converge completion atomically.

- Room schema v2 owns live XP, goals, catalogs, discovery, acquisitions, reward opportunities and dataset revision.
- `ProgressionBootstrapper` installs the validated Collection I and goal seed idempotently.
- `FlowerInvestigationService` spends Spendable XP while preserving Lifetime XP.
- `RoomGardenRepository` produces the persisted Garden snapshot consumed by UI.
- UI renders domain state; it does not calculate canonical XP, rarity or ownership.

## P2 commit chain

```text
2da16a8  feat: add XP ledger and level policy
16aced1  feat: add goal progression engine
08589df  feat: add evidence based flower rarity
bc970c0  feat: add garden reward and discovery engine
129ed26  feat: persist progression and garden in Room v2
3471d9e  feat: add atomic quest completion pipeline
6f230e5  feat: add first validated flower and goal catalogs
a8516c2  feat: add XP and Garden experience
```

## Collection I

Collection I contains **24 unique real botanical species**.

Factual evidence and classification decisions are recorded in:

`docs/evidence/collection-i-sources.md`

V1 rarity classification requires a defensible absolute mature-individual point estimate. Range-only, bound-only, or ambiguous counting evidence remains `UNKNOWN`.

The four user-facing bands remain:

```text
Comum → Incomum → Rara → Raríssima
```

They are equal bands on the versioned log10 population axis and are independent of acquisition probability.

Two deliberately conservative examples:

- `Didymocarpus phuquocensis` remains UNKNOWN because the source mixes mature individuals / mature clumps.
- `Keetia susu` remains UNKNOWN because the observation is not promoted as a closed global point estimate.

## Automated gate

Exact HEAD command:

```powershell
.\gradlew.bat --no-daemon "-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8" test lintDebug assembleDebug
```

Result: **BUILD SUCCESSFUL**.

Unique JVM test methods represented by the current reports:

| Module | Tests | Failures | Skipped |
| --- | ---: | ---: | ---: |
| quest-domain | 7 | 0 | 0 |
| quest-core | 40 | 0 | 0 |
| android-data unit | 3 | 0 | 0 |
| **Unique JVM total** | **50** | **0** | **0** |

`android-data:test` executes its unit set in both debug and release variants; both were green.

Lint:
- `android-data:lintDebug`: green
- `app:lintDebug`: green

Build:
- `app:assembleDebug`: green
- Room schemas present: `1.json`, `2.json`

## Connected Android validation

Device:

```text
AVD: Pixel_4_API_33
model: sdk_gphone64_x86_64
Android: 13
SDK: 33
```

Full connected suites on the P2 tree:

- app: **10/10 green**
- android-data: **12/12 green**

Critical checks were rerun on the exact validated HEAD:

1. `ProgressionActivityE2ETest` — **1/1 green**
   - fresh install;
   - onboarding as Rafa;
   - procedural quest;
   - start and complete;
   - +25 XP;
   - exactly one flower;
   - open `Jardim de Rafa`;
   - restart Activity;
   - XP and acquisition still present.

2. `QuestCompletionTransactionTest` — **2/2 green**
   - failure after writes rolls the whole transaction back;
   - retry returns the same completion with no duplicate XP or flower.

3. `ProgressionBootstrapperTest` — **2/2 green**
   - seed is idempotent;
   - investigation spends balance while preserving Lifetime XP.

Together, the exact-HEAD critical runtime rerun is **5/5 green**.

## Runtime invariants demonstrated

- completion retry does not mint duplicate XP;
- completion retry does not acquire a second flower;
- a failed completion transaction leaves no partial canonical state;
- each profile/flower acquisition is unique;
- spending XP changes Spendable XP but not Lifetime XP;
- derived level therefore does not regress after investigation;
- collected flowers remain excluded from future eligible reward pools;
- exhausted eligible catalogs defer rewards instead of silently losing them;
- returning users see an explicit `Preparando seu jardim…` initialization state instead of a false onboarding flash;
- large-font Garden/header flows remain visible without fixed-height text cards.

## Test isolation

AndroidX Test Orchestrator is used only by instrumentation tests with package-data reset between tests.

This is **test infrastructure**, not a product runtime or persistence bypass. Product state still lives exclusively in Room/DataStore according to the architecture.

## APK evidence

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

SHA-256:

`a050cd1e66b4bd9b689e3ff16c5aaa93deee6f8b858ba61731ecb15d81784aba`

## Resource-bound execution note

The workstation had previously been overloaded by concurrent workloads. P2 validation was therefore rerun conservatively with:

- Gradle single-use daemon;
- `-Xmx1024m`;
- Pixel 4 API 33 emulator in headless mode;
- no user-owned unrelated processes terminated.

These are validation-environment constraints only and are not product hardcodes.

## Known bounded frontier

P2 intentionally does **not** implement:

- local LLM / ModelStore / inference admission — P3;
- online game discovery / rotating game catalog — P4;
- online flower research / future dynamic CatalogPacks — P4;
- backup / restore / SAF — P5;
- final visual theming/accessibility polish — P5.

Collection I is embedded and immutable. Future collections must go through the same factual and catalog validation authorities rather than mutating it.

The Garden remains a collection/grid experience in V1; it is not a free-placement spatial simulation.

## Conclusion

P2 is **validated_bounded**.

The progression path now converges through one persisted flow:

```text
ACTIVE QuestSession
→ atomic QuestCompletion
→ XP + Goals + RewardOpportunity
→ unique FlowerAcquisition
→ Garden
```

The next allowed implementation frontier is **P3 — Local LLM runtime** after P2 review/integration handling.
