# Pinho Quest Progression and Garden Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add auditable XP, derived levels, transactional completion, unique flower rewards, logarithmic real-world rarity, Garden discovery/investigation, and the first embedded real-flower collection.

**Architecture:** Extend the pure Kotlin domain/core first, then migrate Room from v1 to v2 and connect completion atomically. Garden UI consumes persisted catalog/discovery/acquisition state; it never calculates rarity, XP, or ownership.

**Tech Stack:** Existing Kotlin/JVM + Android stack, Room migration tests, kotlinx serialization for embedded catalog assets, Compose UI.

**Spec:** `docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`

## Global Constraints

- Lifetime XP never decreases; Spendable XP is ledger-derived.
- Level is derived from Lifetime XP by a versioned `LevelPolicy`.
- XP may reveal flower information but never purchase a flower.
- Flower states: HIDDEN → HINTED → REVEALED → COLLECTED.
- Flower is unique per profile.
- V1 gives exactly one unique flower for a reward-eligible completion whenever an eligible uncollected flower exists.
- Rarity uses a defensible absolute global population estimate and a versioned logarithmic scale.
- User classes: Comum, Incomum, Rara, Raríssima; insufficient evidence stays UNKNOWN.
- Rarity never changes drop probability.
- Quest completion is transactional and idempotent.

## Review Focus

- Double completion/retry after process death must not duplicate XP or flowers.
- Spending exactly the remaining Spendable XP must succeed without changing Lifetime XP/level.
- Zero/negative/missing population estimates must never enter `log10` classification as valid rarity.
- An exhausted eligible catalog must create a durable deferred reward instead of losing the reward.
- Room v1→v2 migration must preserve profile, tags, quests, and active-session state.

---

### Task 1: XP ledger and derived level

**Files:**
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/progression/XpModels.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/progression/XpPolicy.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/progression/LevelPolicy.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/progression/XpPolicyTest.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/progression/LevelPolicyTest.kt`

**Interfaces:**
- `data class XpTransaction(...)`
- `enum class XpTransactionType { QUEST_REWARD, FLOWER_RESEARCH, ADJUSTMENT }`
- `XpLedgerSnapshot.lifetimeXp(): Int`
- `XpLedgerSnapshot.spendableXp(): Int`
- `XpPolicy.awardFor(completion: QuestCompletionFacts): Int`
- `LevelPolicy.levelFor(lifetimeXp: Int): Int`

- [ ] **Step 1: Write failing tests** for earn/spend separation, bounded objective bonus, and unchanged level after Spendable XP is spent.
- [ ] **Step 2: Run** `./gradlew :quest-core:test`. **Expected:** FAIL.
- [ ] **Step 3: Implement** ledger calculations and versioned policies; keep numeric policy values centralized.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add XP ledger and level policy`.

### Task 2: Goal definitions and evaluation

**Files:**
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/progression/GoalModels.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/progression/GoalEvaluator.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/progression/GoalEvaluatorTest.kt`

**Interfaces:**
- `data class GoalDefinition(...)`, `data class GoalProgress(...)`
- `GoalEvaluator.evaluate(completion: QuestCompletionFacts, current: List<GoalProgress>): GoalEvaluationResult`

- [ ] **Step 1: Write failing tests** for category, difficulty, completion-count, bonus-objective, and XP-milestone goals; repeated evaluation of the same completion must be idempotent.
- [ ] **Step 2: Run** core tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** goal evaluation as a pure deterministic policy; it may create reward eligibility but never directly acquire a flower.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add goal progression engine`.

### Task 3: Population evidence and logarithmic rarity

**Files:**
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/garden/FlowerModels.kt`
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/garden/PopulationEvidence.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/garden/FlowerRarityScale.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/garden/FlowerRarityScaleTest.kt`

**Interfaces:**
- `enum class FlowerRarity { COMMON, UNCOMMON, RARE, RAREST, UNKNOWN }`
- `enum class CountingBasis { INDIVIDUALS, MATURE_INDIVIDUALS }`
- `data class GlobalPopulationEstimate(...)`
- `FlowerRarityScale.classify(estimate: GlobalPopulationEstimate): FlowerRarity`

- [ ] **Step 1: Write failing tests** for all four equal log bands, exact boundaries, non-positive values, missing evidence metadata, and incompatible counting-basis rejection.
- [ ] **Step 2: Run** core tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** the versioned logarithmic classifier; invalid/non-positive input never reaches `log10`.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add evidence based flower rarity`.

### Task 4: Catalog, discovery, reward and investigation contracts

**Files:**
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/garden/CatalogModels.kt`
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/garden/FlowerDiscovery.kt`
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/reward/RewardModels.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/reward/RewardEngine.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/garden/FlowerInvestigationService.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/reward/RewardEngineTest.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/garden/FlowerInvestigationServiceTest.kt`

**Interfaces:**
- `enum class FlowerDiscoveryState { HIDDEN, HINTED, REVEALED, COLLECTED }`
- `data class CatalogPack(...)`
- `data class RewardOpportunity(...)`
- `RewardEngine.resolve(opportunity, catalog, inventory): RewardResolution`
- `FlowerInvestigationService.investigate(profileId, flowerId): InvestigationResult`

- [ ] **Step 1: Write tests** proving collected flowers are excluded, eligible flowers are uniformly selectable with injected RNG, and exhausted catalogs yield `Deferred` rather than loss.
- [ ] **Step 2: Write tests** for HIDDEN→HINTED→REVEALED and insufficient Spendable XP.
- [ ] **Step 3: Run** core tests. **Expected:** FAIL.
- [ ] **Step 4: Implement** reward/investigation services with deterministic injectable randomness for tests.
- [ ] **Step 5: Run** tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add garden reward and discovery engine`.

### Task 5: Room v2 migration and progression persistence

**Files:**
- Modify: `android-data/src/main/kotlin/com/pinhoquest/data/db/PinhoQuestDatabase.kt`
- Create entities for XP transactions, goal definitions/progress, catalog packs/entries, flower discovery/acquisition, reward opportunities, dataset metadata under `android-data/src/main/kotlin/com/pinhoquest/data/db/entity/`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/db/migration/Migration1To2.kt`
- Test: `android-data/src/androidTest/kotlin/com/pinhoquest/data/db/Migration1To2Test.kt`

**Interfaces:**
- Produces Room schema v2, unique `(profileId, flowerId)` acquisition, persisted rarity-scale version/evidence reference, durable reward opportunities, monotonic dataset revision.

- [ ] **Step 1: Write a v1 fixture/migration test** asserting profile, tags, quests, and active session survive v1→v2.
- [ ] **Step 2: Add failing tests** for duplicate flower acquisition and duplicate reward-opportunity identity.
- [ ] **Step 3: Run** migration tests. **Expected:** FAIL.
- [ ] **Step 4: Implement** entities/DAOs/migration and export Room schema v2.
- [ ] **Step 5: Run** tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: persist progression and garden in Room v2`.

### Task 6: Transactional quest completion

**Files:**
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/completion/RoomQuestCompletionStore.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/completion/QuestCompletionService.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/completion/QuestCompletionServiceTest.kt`
- Test: `android-data/src/androidTest/kotlin/com/pinhoquest/data/completion/QuestCompletionTransactionTest.kt`

**Interfaces:**
- `QuestCompletionService.complete(sessionId: QuestSessionId, completedObjectives: Set<ObjectiveId>): CompletionResult`
- One atomic store operation covers session completion, XP, goal progress, reward opportunity/resolution, and dataset revision.

- [ ] **Step 1: Write tests** asserting repeated completion returns the original completion and never adds duplicate XP/reward.
- [ ] **Step 2: Write an instrumentation test** injecting failure before commit and proving no partial XP/acquisition remains.
- [ ] **Step 3: Run** tests. **Expected:** FAIL.
- [ ] **Step 4: Implement** transaction orchestration and idempotency keys.
- [ ] **Step 5: Run** tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add atomic quest completion pipeline`.

### Task 7: Embedded Collection I

**Files:**
- Create: `app/src/main/assets/catalog/collection_i.json`
- Create: `app/src/main/assets/goals/v1_goals.json`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/catalog/EmbeddedCatalogLoader.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/progression/EmbeddedGoalLoader.kt`
- Test: `android-data/src/test/kotlin/com/pinhoquest/data/catalog/EmbeddedCatalogLoaderTest.kt`
- Test: `android-data/src/test/kotlin/com/pinhoquest/data/progression/EmbeddedGoalLoaderTest.kt`
- Create: `docs/evidence/collection-i-sources.md`

**Interfaces:**
- Produces immutable first `CatalogPack` with exactly 24 real species and a versioned initial goal set on fresh install.

- [ ] **Step 1: Write a failing loader test** requiring 24 unique scientific names, stable IDs, source URIs, population evidence, counting basis, estimate date, and rarity-scale version for every classified entry.
- [ ] **Step 2: Research** candidate species and record evidence; exclude any species without a defensible absolute global estimate instead of guessing.
- [ ] **Step 3: Create** the 24-entry catalog and pass it through the same catalog validator used by future packs.
- [ ] **Step 4: Create** the initial versioned goal set covering completion count, category, difficulty, bonus-objective, and Lifetime-XP milestones; load it through `EmbeddedGoalLoader`.
- [ ] **Step 5: Run** loader/domain tests. **Expected:** PASS with zero duplicate IDs, zero invalid rarity inputs, and all goal definitions parseable.
- [ ] **Step 6: Commit** `feat: add first validated flower and goal catalogs`.

### Task 8: Garden and completion UI

**Files:**
- Replace: `app/src/main/kotlin/com/pinhoquest/ui/garden/GardenPlaceholderScreen.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/garden/GardenScreen.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/garden/FlowerDetailScreen.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/ui/quests/QuestCompletionDialog.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/ui/GardenFlowTest.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/ui/GardenLargeFontTest.kt`

**Interfaces:**
- Produces `Jardim de <nome>`, XP/level summary, adaptive grid, hidden/hinted/revealed/collected detail, investigation confirmation, completion reward dialog.

- [ ] **Step 1: Write UI tests** for all discovery states, `Jardim de Rafa`, collected detail, XP investigation, insufficient XP, and large-font adaptive grid.
- [ ] **Step 2: Run** UI tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** screens/dialogs with humanized copy and no fixed-height text cards.
- [ ] **Step 4: Run** UI tests, lint, assemble. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add XP and Garden experience`.

### Task 9: Progression runtime checkpoint

**Files:**
- Create: `docs/checkpoints/V1-P2-progression-garden.md`

- [ ] **Step 1: Run** `./gradlew test lintDebug assembleDebug`. **Expected:** all green.
- [ ] **Step 2: Fresh install** → onboarding → procedural quest → complete → verify XP, level, one unique flower, and persistence after restart.
- [ ] **Step 3: Re-trigger completion** and verify no extra XP/flower.
- [ ] **Step 4: Spend XP** on an undiscovered flower and verify Lifetime XP/level remain unchanged.
- [ ] **Step 5: Record evidence and commit** `docs: validate progression and Garden`.