# Engineering Genome — 02 Authority Map

This document is a quick operational reference. The full contracts live in the canonical V1 design spec.

## Quest generation

```text
QuestRequest
→ QuestPlanner
→ QuestGenerationPlan
→ ComposerPort
→ QuestDraft
→ QuestValidator
→ Quest
```

Authorities:
- `QuestPlanner`: generation intent and constraints.
- `QuestValidator`: only authority that promotes a draft to a valid Quest.
- `LocalLlmComposer`: creative text composition only.
- `ProceduralComposer`: deterministic/fallback creative composition.
- UI: never creates a Quest directly.

## Local inference

```text
InferenceAdmissionController
→ LocalInferencePort
→ runtime adapter
→ ModelStore
```

Authorities:
- `InferenceAdmissionController`: whether the local model may run now.
- `LocalInferencePort`: runtime abstraction.
- `ModelStore`: installed model artifacts and validated manifest.

Admission considers real current headroom and runtime state, not device-name hardcodes.

## Internet research

```text
Integrated web research
├── GameResearchService
└── FlowerResearchService
```

Authorities:
- external sources provide observations/evidence;
- research services normalize and preserve provenance;
- local validators decide promotion;
- user profile ranking remains local.

## Game data

```text
web discovery
→ candidate extraction
→ fact validation
→ bounded rotating GameDiscoveryCatalog
→ local selection
→ quest composer
```

`GameDiscoveryCatalog` is not user history. Eviction from the catalog must not erase suggestion/play history.

## Flower data

```text
web research
→ flower candidate
→ population evidence
→ rarity scale
→ CatalogPackValidator
→ immutable CatalogPack
```

Flower rarity:
- uses a defensible estimate of absolute global individuals;
- uses a versioned logarithmic scale;
- has user-facing bands Comum, Incomum, Rara, Raríssima;
- remains UNKNOWN when evidence is insufficient;
- never controls drop probability.

## Progression

Authorities:
- `XpLedger`: auditable XP transactions.
- `XpPolicy`: award policy.
- `LevelPolicy`: derives level from Lifetime XP.
- `RewardEngine`: resolves reward opportunities.
- `FlowerDiscovery`: HIDDEN → HINTED → REVEALED → COLLECTED.

Rules:
- Spendable XP may reveal information.
- XP cannot buy a flower.
- Spending XP never reduces Lifetime XP.
- A flower is unique per profile.
- V1 has no empty RNG miss when an eligible unique flower exists.

## Persistence

```text
Room        → live domain truth
DataStore   → UI/runtime preferences
ModelStore  → local model files
Backup      → validated snapshots
```

Room is the only live domain authority.

Backups never participate in bidirectional live synchronization.

## Quest completion transaction

A canonical completion must converge atomically:

```text
ACTIVE session
→ COMPLETED
→ QuestCompletion
→ XP transaction
→ Goal progress
→ RewardOpportunity
→ FlowerAcquisition when possible
→ datasetRevision increment
```

Retries must be idempotent.

## Backup/restore

A backup contains revision, schema/version and integrity metadata.

Restore pipeline:

```text
read
→ integrity validation
→ schema validation
→ migration
→ staged restore
→ user confirmation
→ Room transaction
```

An older backup never overwrites newer local progress silently.

## UI authority boundary

UI may:
- request;
- display;
- confirm;
- translate technical outcomes into human language.

UI may not:
- calculate canonical XP;
- assign rarity;
- fabricate factual game/flower data;
- directly mark a reward owned;
- bypass validators;
- directly become persistence authority.
