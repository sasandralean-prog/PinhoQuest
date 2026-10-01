# Pinho Quest Web Research Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add V1 integrated web research for free game discovery and real-flower catalog expansion, with provenance, validation, bounded local game rotation, offline reuse, and deferred-reward convergence.

**Architecture:** External pages and search results are observations, never domain truth. A web research port fetches material; source adapters extract candidates; validators normalize and promote them; personal selection remains local.

**Tech Stack:** Kotlin coroutines, Android HTTP client, HTML/structured-data parsing, Room v3, WorkManager, recorded parser fixtures.

**Spec:** `docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`

## Global Constraints

- Internet exists in V1 for game discovery/refresh and flower research/expansion.
- No privileged platform API secret is shipped inside the app.
- Gaming publications/editorial sources discover candidates; they are not permanent truth authorities.
- Every promoted external fact retains source URI and observation timestamp.
- Personal profile/tags stay local for ranking.
- Game catalog is bounded and rotating; eviction never deletes history.
- Existing catalog packs remain immutable.
- Flower identity/population evidence must be defensible; unknown is preferable to fabricated certainty.

## Review Focus

- Source markup changes must yield parser/source failure, not malformed promoted candidates.
- Old “free game” claims must not be presented as freshly verified without timestamp/state.
- Offline Game Quest uses viable stored candidates and does not hide a network-only fallback.
- Cross-platform duplicate titles merge by canonical identity without losing platform observations.
- Flower candidates without defensible population evidence are skipped/rejected rather than assigned invented rarity.

---

### Task 1: Web research transport and semantic outcomes

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/research/WebResearchPort.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/research/ResearchOutcome.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/research/AndroidWebResearchClient.kt`
- Test: `app/src/test/kotlin/com/pinhoquest/research/AndroidWebResearchClientTest.kt`

**Interfaces:**
- `WebResearchPort.search(query: WebSearchQuery): ResearchOutcome<SearchResultPage>`
- `WebResearchPort.fetch(uri: String): ResearchOutcome<WebDocument>`
- Outcomes: `Success`, `Unavailable`, `RateLimited`, `InvalidResponse`, `UnsupportedSource`, `TechnicalFailure`.

- [ ] **Step 1: Write tests** using local fixtures for successful HTML, timeout, rate limit, invalid response/body, and unsupported URI scheme.
- [ ] **Step 2: Run** tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** bounded-timeout transport, explicit user-agent, no privileged embedded credentials, and semantic failure mapping.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add web research transport`.

### Task 2: Editorial game candidate discovery

**Files:**
- Create: `quest-domain/src/main/kotlin/com/pinhoquest/domain/game/GameModels.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/game/GameEditorialSource.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/game/GameResearchService.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/research/game/EditorialSourceRegistry.kt`
- Create: `app/src/test/resources/game-pages/`
- Test: `app/src/test/kotlin/com/pinhoquest/research/game/EditorialSourceParsingTest.kt`

**Interfaces:**
- `data class GameCandidate(canonicalName, platforms, genres, freeStatus, sources, observedAt, verificationState)`
- `GameResearchService.discover(request: GameResearchRequest): GameResearchResult`
- Source registry is configurable and seeded with at least two reputable editorial gaming sources covering desktop and mobile.

- [ ] **Step 1: Capture** public recorded fixtures for the initial editorial sources; write parser tests for title/platform/free-status extraction and markup drift.
- [ ] **Step 2: Run** parser tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** source adapters behind the registry; provider-specific selectors stay inside adapters, not core.
- [ ] **Step 4: Add tests** for normalization/dedup across spelling, case, and platform variants.
- [ ] **Step 5: Run** tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: discover game candidates from editorial web sources`.

### Task 3: Room v3 rotating GameDiscoveryCatalog

**Files:**
- Modify: `android-data/src/main/kotlin/com/pinhoquest/data/db/PinhoQuestDatabase.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/db/entity/GameCatalogEntryEntity.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/db/entity/GameObservationEntity.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/db/entity/GameSuggestionHistoryEntity.kt`
- Create: `android-data/src/main/kotlin/com/pinhoquest/data/db/migration/Migration2To3.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/game/GameCatalogRotationPolicy.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/game/GameCatalogRotationPolicyTest.kt`
- Test: `android-data/src/androidTest/kotlin/com/pinhoquest/data/db/Migration2To3Test.kt`

**Interfaces:**
- Produces bounded catalog with configurable per-platform/genre limits and separate durable suggestion history.

- [ ] **Step 1: Write rotation tests** for new candidate, refresh existing, stale/repeated eviction, coverage, and eviction-not-erasing-history.
- [ ] **Step 2: Write v2→v3 migration test** preserving all previous data.
- [ ] **Step 3: Run** tests. **Expected:** FAIL.
- [ ] **Step 4: Implement** entities, migration, repository, and rotation policy.
- [ ] **Step 5: Export** schema v3 and run tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: add bounded rotating game catalog`.

### Task 4: Game Quest online/offline integration

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/game/GameSelector.kt`
- Modify: `quest-core/src/main/kotlin/com/pinhoquest/core/quest/QuestPlanner.kt`
- Modify: `app/src/main/kotlin/com/pinhoquest/ui/quests/QuestScreen.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/game/GameSelectorTest.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/ui/GameQuestFlowTest.kt`

**Interfaces:**
- Local selector uses platform/genre affinity, freshness, never-suggested preference, last-suggested time, and repetition penalty.

- [ ] **Step 1: Write tests** proving only currently eligible free/free-to-play candidates are selectable, offline viable catalog works, online refresh can rotate, and offline-empty catalog returns humanized “sem opções novas” instead of NORMAL.
- [ ] **Step 2: Run** tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** selector and GAME planner integration; composer receives validated game facts only.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: add online and offline Game Quest`.

### Task 5: Flower web research and evidence promotion

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/garden/FlowerResearchService.kt`
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/garden/FlowerFactValidator.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/research/flower/FlowerIdentitySource.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/research/flower/PopulationEvidenceSource.kt`
- Create: `app/src/test/resources/flower-pages/`
- Test: `app/src/test/kotlin/com/pinhoquest/research/flower/FlowerResearchParsingTest.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/garden/FlowerFactValidatorTest.kt`

**Interfaces:**
- Produces validated `FlowerCandidate` only when identity and population evidence satisfy the spec; otherwise explicit rejection or UNKNOWN.

- [ ] **Step 1: Write fixture tests** for accepted species, missing population count, ambiguous counting basis, contradictory evidence, and undated evidence.
- [ ] **Step 2: Run** tests. **Expected:** FAIL.
- [ ] **Step 3: Implement** identity/evidence adapters and validator; preserve accepted source URI, date, and basis.
- [ ] **Step 4: Run** tests. **Expected:** PASS.
- [ ] **Step 5: Commit** `feat: research validated flower evidence`.

### Task 6: Dynamic CatalogPack expansion

**Files:**
- Create: `quest-core/src/main/kotlin/com/pinhoquest/core/garden/CatalogExpansionService.kt`
- Create: `app/src/main/kotlin/com/pinhoquest/garden/CatalogExpansionWorker.kt`
- Create/modify: `android-data/src/main/kotlin/com/pinhoquest/data/catalog/RoomCatalogRepository.kt`
- Test: `quest-core/src/test/kotlin/com/pinhoquest/core/garden/CatalogExpansionServiceTest.kt`
- Test: `app/src/androidTest/kotlin/com/pinhoquest/garden/CatalogExpansionFlowTest.kt`

**Interfaces:**
- Produces a validated immutable next `CatalogPack` containing 20–30 entries and personalized locally.
- Expansion states: `NOT_REQUIRED`, `PENDING`, `RESEARCHING`, `READY`, `FAILED_RETRYABLE`.

- [ ] **Step 1: Write tests** for completed Collection I, offline pending, 20–30 accepted entries, duplicate exclusion, and immutable prior pack.
- [ ] **Step 2: Write test** proving deferred reward opportunities resolve exactly once after a new pack installs.
- [ ] **Step 3: Run** tests. **Expected:** FAIL.
- [ ] **Step 4: Implement** service/worker and local personalization; LLM may write lore only after factual validation.
- [ ] **Step 5: Run** tests. **Expected:** PASS.
- [ ] **Step 6: Commit** `feat: expand Garden catalogs from web research`.

### Task 7: Web research runtime checkpoint

**Files:**
- Create: `docs/checkpoints/V1-P4-web-research.md`

- [ ] **Step 1: Run** the full test/lint/build suite.
- [ ] **Step 2: Online**, refresh game catalog and verify multiple genres/platforms plus provenance timestamps.
- [ ] **Step 3: Offline**, create Game Quest from stored catalog; then test empty-catalog humanized fallback.
- [ ] **Step 4: Complete or seed Collection I**, trigger flower research, verify frozen next pack and one deferred-reward convergence case.
- [ ] **Step 5: Record** source/parser limitations and commit `docs: validate web research and catalog rotation`.