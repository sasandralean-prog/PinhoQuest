# Pinho Quest — Architecture Map

**Canonical product/design spec:** `docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`

This file is a compact architecture map. If any summary here ever conflicts with the design spec, the design spec wins until an explicit architecture change updates both.

## 1. Top-level structure

```text
quest-domain
├── Quest / QuestSession / QuestCompletion
├── UserProfile / Tag
├── XP / Level
├── Goal
├── Flower / FlowerDiscovery / FlowerAcquisition
├── CatalogPack
└── GameCandidate

quest-core
├── QuestEngine
├── QuestPlanner
├── QuestValidator
├── InferenceAdmissionController
├── RewardEngine
├── GoalEvaluator
├── CatalogEngine
├── GameResearchService
└── FlowerResearchService

ports
├── ComposerPort
├── LocalInferencePort
├── GameResearchPort
├── FlowerResearchPort
├── PersistencePort
├── PreferencesPort
├── BackupPort
└── ModelStorePort

android adapters
├── Compose UI
├── Room
├── DataStore
├── SAF
├── ModelStore
├── local inference runtime
└── integrated web research
```

The domain/core must remain free of Android SDK types.

## 2. Canonical quest flow

```text
QuestRequest
→ QuestPlanner
→ QuestGenerationPlan
→ ComposerPort
→ QuestDraft
→ QuestValidator
→ Quest
→ QuestSession
→ QuestCompletion
→ XP / Goals / RewardOpportunity
→ FlowerAcquisition
```

Only `QuestValidator` promotes a draft into a valid Quest.

NORMAL, GAME and RANDOM are policies over this same flow; they are not separate runtimes.

## 3. Local generation

```text
Quest generation request
→ InferenceAdmissionController
   ├─ resources adequate → LocalLlmComposer
   └─ not adequate       → ProceduralComposer
→ QuestValidator
```

The local model runs on-device when admitted. Admission considers current memory pressure, thermal state, runtime readiness, workload, concurrency and local execution history. No device-name whitelist and no invented universal CPU-free threshold.

## 4. Internet boundary

Internet is part of V1 but is not the brain of the app.

```text
Integrated Web Research
├── Game discovery
│   └── editorial/reputable gaming sources
└── Flower research
    └── real species + population evidence
```

External facts carry provenance. Personal ranking remains local.

## 5. Game catalog

The game list in Room is a **bounded rotating local catalog**, not disposable HTTP cache.

It preserves:
- canonical title;
- platform(s);
- genres;
- free/free-to-play observation;
- source and observation time;
- last suggestion;
- suggestion count;
- verification state.

Eviction from the working catalog never erases user history.

## 6. Garden and rarity

Flowers are real species.

Rarity is computed from a defensible estimate of the absolute number of individuals worldwide, using a versioned logarithmic scale:

```text
Comum → Incomum → Rara → Raríssima
```

If the population estimate is not defensible, rarity is unknown or the species is excluded from a classified pack.

Rarity does not make a flower harder to drop. Among eligible uncollected flowers, V1 selection is uniform.

A flower is unique per profile.

## 7. XP and discovery

```text
QuestCompletion
├── Lifetime XP
├── Spendable XP
├── Level (derived)
└── RewardOpportunity
```

Spendable XP may reveal information about hidden flowers. It cannot buy flowers. Spending XP does not reduce Lifetime XP or derived level.

Flower knowledge states:

```text
HIDDEN → HINTED → REVEALED → COLLECTED
```

## 8. Persistence authorities

```text
Room
→ canonical live domain state

DataStore
→ UI/runtime preferences

ModelStore
→ local model artifacts

Private backup / SAF backup
→ derived snapshots only
```

Quest completion is transactional and idempotent.

Each meaningful domain mutation increments `datasetRevision`.

A restore is always staged and validated. Older backup revisions never overwrite newer local progress silently.

## 9. UI destinations

```text
🎲 Quests
🏷 Tags
🌷 Jardim
⚙ Configurações
```

The app defaults to Quests.

The UX voice is warm, optimistic, simple, truthful and lightly playful. Technical internal language must stay behind diagnostics.

The garden owner name is 1–20 trimmed characters and renders as `Jardim de <nome>`.

## 10. Future Windows boundary

Windows is not a V1 client, but the architecture must allow a future frontend to reuse:
- quest-domain;
- quest-core;
- policy logic;
- portable `.pqbackup` contract.

Windows supplies its own UI, persistence, inference and filesystem adapters.
