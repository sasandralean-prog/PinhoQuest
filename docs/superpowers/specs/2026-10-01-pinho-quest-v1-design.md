# Pinho Quest V1 — Design Specification

**Status:** written design awaiting user review  
**Date:** 2026-10-01  
**Repository:** `sasandralean-prog/PinhoQuest`  
**Target:** Android first, Windows-compatible core  
**Design authority:** this document is the canonical V1 product/architecture specification. Supporting files in `Engineering_Genome/` and `architecture/` summarize rules and boundaries but must not contradict this spec.

---

## 1. Purpose

Pinho Quest is a personal anti-boredom micro-adventure generator.

Its job is simple: when the user thinks “não sei o que fazer”, the app should quickly propose something fun, curious, creative, technical, exploratory or game-related that can be done now.

The product is not a productivity tracker and must not optimize for compulsive engagement. The desired loop is:

```text
Tédio → Sortear → Aceitar → Fazer → Concluir → Progresso → Flor → Sair do app
```

Success means:
- one tap can produce a useful quest with minimal friction;
- the app remains useful offline after the local model has been installed;
- personalization is explicit and user-controlled;
- progression feels playful without streaks, FOMO, lootboxes or punitive retention;
- facts about games and real flowers remain traceable to external evidence;
- Android ships first without trapping the domain inside Android-only code;
- the same core can later support a Windows frontend.

---

## 2. Product principles

1. **One canonical flow per responsibility.** No bypasses or parallel runtimes that duplicate authority.
2. **The UI is not an authority.** It requests operations and renders state.
3. **The LLM is a creative composer, not a source of truth.**
4. **Internet supplies current/external facts, not the product brain.**
5. **Room is the sole authority for live persistent domain state.**
6. **Backups are snapshots, never a second live database.**
7. **Humanized UX must remain truthful.** Friendly wording must not hide failure.
8. **Personalization is explicit.** The app does not infer hidden psychological preferences from dwell time or similar surveillance.
9. **Gamification must remain non-coercive.** No streaks, daily rewards, energy, premium currency, leaderboards, timed scarcity or lootbox-style low-probability rewards.
10. **Failures degrade gracefully.** A failed LLM generation does not mean “no quest”; a failed web refresh does not erase the local catalog.

---

## 3. Chosen architecture

V1 uses **pure Kotlin/JVM domain + core modules with platform adapters**.

The app does not begin as a full Kotlin Multiplatform product, and it does not embed the core directly into Android UI classes.

```text
                    PINHO QUEST

             ┌────────────────────┐
             │    quest-domain    │
             │ Quest / Tag / XP   │
             │ Flower / Goal      │
             │ Catalog / Profile  │
             └─────────┬──────────┘
                       │
             ┌─────────▼──────────┐
             │     quest-core     │
             │ QuestEngine        │
             │ QuestPlanner       │
             │ QuestValidator     │
             │ RewardEngine       │
             │ GoalEvaluator      │
             │ CatalogEngine      │
             └─────────┬──────────┘
                       │
      ┌────────────────┼─────────────────┐
      ▼                ▼                 ▼
 ComposerPort   GameResearchPort   FlowerResearchPort
      │
 ┌────┴─────────────┐
 ▼                  ▼
Procedural       Local LLM

──────────────── platform boundary ────────────────

Android V1
├─ Compose UI
├─ Room adapter
├─ DataStore adapter
├─ local inference runtime
├─ web research adapter
├─ SAF backup/export
└─ ModelStore

Windows future
├─ desktop UI
├─ persistence adapter
├─ local inference adapter
└─ filesystem/export adapter
```

The domain and core must not depend on Android SDK types.

---

## 4. Core quest lifecycle

A valid Quest may only be promoted by `QuestValidator`.

```text
QuestRequest
    ↓
QuestPlanner
    ↓
QuestGenerationPlan
    ↓
ComposerPort
    ├─ LocalLlmComposer
    └─ ProceduralComposer
    ↓
QuestDraft
    ↓
QuestValidator
    ↓
Quest
    ↓
ACCEPT / SKIP / REJECT
    ↓
QuestSession
    ↓
COMPLETE / ABANDON
    ↓
QuestCompletion
    ├─ XpEvaluator
    ├─ GoalEvaluator
    └─ RewardOpportunity
```

### 4.1 Quest modes

V1 has three user-visible modes implemented as selection policies over the same engine:

- `NORMAL` — “Sortear Quest”; uses profile, history and optional session filters.
- `GAME` — “Quest de Jogo”; requires a validated game candidate from the local game catalog.
- `RANDOM` — “Quest Aleatória”; reduces personalization weight and increases novelty.

There are not three independent quest runtimes.

### 4.2 Quest states

```text
GENERATED
ACCEPTED
ACTIVE
COMPLETED
ABANDONED
REJECTED
```

“Outra” is a skip, not a rejection. “Não quero quests assim” is explicit negative preference feedback.

### 4.3 Quest content

A Quest may contain:
- title;
- friendly description;
- one or more main objectives;
- optional bonus objectives;
- category;
- target environment/platform;
- estimated time;
- estimated difficulty;
- structured source/context metadata when relevant.

Difficulty and time are estimates, not surveillance targets.

---

## 5. Profile and tags

The user profile includes a garden owner name and personalization data.

```text
UserProfile
├── profileId
├── gardenOwnerName
├── createdAt
└── domain metadata
```

### 5.1 Garden owner name

- 1 to 20 characters after trimming;
- editable later;
- stored in Room;
- included in backup/restore;
- rendered as “Jardim de <nome>”.

### 5.2 Tag sources

```text
TagSource
├── SYSTEM
├── USER
└── DERIVED
```

- SYSTEM: built-in categories and known genres.
- USER: directly created/selected by the user.
- DERIVED: suggested from user-written natural-language context and only activated after explicit confirmation.

A local model may suggest tags, but may not silently rewrite USER tags.

### 5.3 Session filters

“Estou com vontade de…” produces temporary constraints such as:
- available time;
- Android / Windows / anywhere / outdoor;
- calm / creative / learning / gaming / challenge / surprise.

Session filters do not permanently alter profile affinity.

---

## 6. Local LLM subsystem

The LLM runs locally on the Android device when admitted by the runtime.

It is a creative composer, not the canonical Quest authority.

```text
QuestEngine
    ↓
InferenceAdmissionController
    ├─ admitted → LocalLlmComposer
    └─ not admitted → ProceduralComposer
    ↓
QuestDraft
    ↓
QuestValidator
```

### 6.1 Runtime abstraction

`LocalInferencePort` isolates the core from a specific inference library or model.

The initial Android implementation may use LiteRT-LM or another suitable on-device runtime selected during implementation benchmarking. The domain must not depend on the chosen runtime.

### 6.2 ModelStore

The model is not required to be bundled inside the APK.

```text
ModelDownload
    ↓
.partial file
    ↓
manifest + SHA-256 validation
    ↓
atomic promotion
    ↓
private ModelStore
```

A model manifest records:
- model ID;
- version;
- format/runtime compatibility;
- byte size;
- SHA-256;
- license/source metadata;
- context capability;
- supported backends.

### 6.3 Inference admission

The model is only invoked when the device has adequate current headroom.

The admission controller considers:
- Android low-memory state and available memory;
- thermal/headroom state;
- model/runtime readiness;
- current app workload;
- inference concurrency;
- locally learned execution history for this device/model.

V1 must **not** invent a universal “CPU free percentage” threshold.

If inference is not admitted, generation falls back to the procedural composer without presenting a technical error to the user.

### 6.4 Local execution profile

The app may persist local-only telemetry such as:
- model ID/version;
- backend used;
- load duration;
- generation duration;
- approximate output length;
- thermal state before/after;
- outcome.

This exists to improve local admission decisions and is not remote analytics.

---

## 7. Internet research and provenance

Internet access exists in V1 for:
- discovery/refresh of free game candidates;
- research and expansion of real flower catalogs;
- external resource/model updates.

Ordinary quest generation does not require a server after the local model is installed.

All promoted external facts retain provenance.

```text
ObservedFact<T>
├── value
├── source/provider
├── sourceUri
├── observedAt
├── fetchedAt
└── external identity when available
```

The full personal profile is not sent to external research sources. Personal ranking happens locally after candidate retrieval.

---

## 8. Game discovery catalog

Game discovery uses integrated web research, prioritizing reputable gaming publications/editorial sources to find candidates. A publication is a discovery source, not a permanent truth authority.

The process is:

```text
WebGameResearch
    ↓
CandidateExtractor
    ↓
GameFactValidator
    ↓
GameDiscoveryCatalog (Room)
    ↓
GameSelector (local personalization)
    ↓
LocalLlmComposer
    ↓
QuestDraft
```

### 8.1 Candidate data

A normalized game candidate can contain:
- canonical name;
- supported platforms;
- genres;
- free/free-to-play observation;
- discovery source;
- source URI;
- observation date;
- verification state;
- last suggested time;
- suggestion count.

A single canonical game may reference multiple platforms.

### 8.2 Local catalog is a live bounded catalog, not a disposable HTTP cache

The app retains a small rotating set of game candidates in Room.

Rotation considers:
- platform;
- genre coverage;
- freshness;
- whether the title was already suggested;
- time since last suggestion;
- repetition penalty;
- user affinity.

Exact capacity is configuration, not domain truth. Implementation must enforce a finite bounded catalog and preserve historical suggestion records separately.

Evicting a discovery candidate never deletes user history.

### 8.3 Offline behavior

- If offline with usable catalog entries, Game Quest still works.
- If online, the app may refresh/rotate candidates.
- If offline with no viable candidate, UI explains that new game options require connection and offers another quest type.
- Stale observations remain labeled internally with observation time; the app must not silently present old availability as freshly verified.

---

## 9. Flower research and catalog expansion

Flowers in the garden are real botanical species.

External research supplies factual identity and population evidence. A local LLM may produce friendly lore or thematic association only after facts are validated.

```text
CatalogExpansionRequested
    ↓
FlowerWebResearch
    ↓
FlowerCandidate[]
    ↓
FlowerFactValidator
    ↓
Personalized local selection
    ↓
Local LLM lore/theme
    ↓
CatalogPackValidator
    ↓
immutable CatalogPack
    ↓
Room
```

### 9.1 Catalog packs

A generated collection becomes immutable after validation.

```text
CatalogPack
├── packId
├── version
├── generatedAt
├── entries
├── research sources
├── personalization fingerprint
└── integrity hash
```

Future research creates a new pack instead of rewriting an old one.

The first collection is shipped with the app. Completing a collection triggers or offers generation of the next pack, typically 20–30 additional flowers.

If offline, expansion remains pending and is retried when research becomes possible.

---

## 10. Flower rarity model

Flower rarity is based on the **estimated absolute number of individuals of that species in the world**, not on game drop probability.

### 10.1 Population evidence

```text
GlobalPopulationEstimate
├── estimatedIndividuals
├── lowerBound?
├── upperBound?
├── estimateDate
├── evidenceSources[]
├── confidence
└── countingBasis
    ├── INDIVIDUALS
    └── MATURE_INDIVIDUALS
```

The system must not silently compare incompatible counting bases.

If no defensible global estimate exists, the app must not fabricate one. The species remains `RARITY_UNKNOWN` or is excluded from a pack that requires a classified rarity.

### 10.2 Logarithmic classification

A versioned `FlowerRarityScale` converts population magnitude into four user-facing classes:

- **Comum**
- **Incomum**
- **Rara**
- **Raríssima**

The scale is computed on a logarithmic population axis.

For scale version `v`:

```text
position =
    (log10(Nmax) - log10(Nspecies))
    /
    (log10(Nmax) - log10(Nmin))
```

The V1 scale divides the normalized log interval into four equal bands.

- first quarter → Comum;
- second quarter → Incomum;
- third quarter → Rara;
- fourth quarter → Raríssima.

`Nmin`, `Nmax`, methodology and counting basis rules belong to the versioned scale definition, not scattered constants.

A flower acquisition records the scale version used so future scientific recalibration does not silently rewrite historical garden metadata.

### 10.3 Rarity is independent of drop probability

A rarer real-world species is **not** made artificially harder to obtain merely because it is rare in nature.

Among currently eligible uncollected flowers, V1 uses a uniform selection policy.

---

## 11. XP, levels and discovery

XP has two derived views over a transaction ledger:

- **Lifetime XP** — all XP earned historically; never decreases when XP is spent.
- **Spendable XP** — current spendable balance.

A level is derived from Lifetime XP through a versioned `LevelPolicy`; level is not an independent stored truth.

### 11.1 XP ledger

```text
XpTransaction
├── transactionId
├── amount
├── type
├── questId?
├── flowerId?
└── createdAt
```

Representative transaction types:
- `QUEST_REWARD`;
- `FLOWER_RESEARCH`;
- controlled adjustment/migration types.

The exact numeric XP schedule belongs to a versioned `XpPolicy`. Difficulty is the main base input; optional objectives may produce a bounded bonus.

The LLM never decides XP.

### 11.2 Flower discovery states

```text
HIDDEN → HINTED → REVEALED → COLLECTED
```

XP may be spent to reveal knowledge, not to buy the flower.

Level milestones may also reveal partial information automatically.

Spending XP must never reduce Lifetime XP or reverse already reached level milestones.

---

## 12. Reward and drop semantics

Flowers are unique per profile.

```text
UNIQUE(profileId, flowerId)
```

Collected flowers are permanently excluded from the eligible pool.

V1 intentionally avoids “miss” rolls: **each completed reward-eligible quest awards exactly one unique flower whenever at least one eligible uncollected flower exists in the active catalog.**

If no eligible flower is available because a catalog expansion is pending, the completion creates a durable deferred `RewardOpportunity`. It resolves when a valid catalog becomes available.

This gives the user a meaningful reward without casino-style repeated failure rolls.

```text
QuestCompletion
    ↓
RewardOpportunity
    ↓
EligibleFlowerResolver
    ↓
UniformDropPolicy
    ↓
FlowerAcquisition
    ↓
Garden
```

Rarity metadata does not alter this selection probability.

---

## 13. Garden UI model

The garden is a grid/collection, not a free-placement spatial simulation in V1.

Each slot renders a combination of:
- flower definition;
- discovery state;
- acquisition state.

### 13.1 Collected flower detail

Shows:
- common name;
- scientific name when available;
- rarity class;
- world population estimate/evidence summary when appropriate;
- XP awarded by the associated quest;
- quest title that unlocked it;
- acquisition date;
- friendly description/lore;
- factual “about this flower” content from validated catalog data.

### 13.2 Uncollected flower detail

Depending on discovery state, information may be fully hidden, partially hinted or fully identified.

Users may spend Spendable XP to advance discovery state. The UI explains that total progress does not decrease.

---

## 14. Persistence architecture

Room is the single authority for live domain state.

```text
                         Room
                    canonical state
                           │
         ┌─────────────────┼────────────────┐
         ▼                 ▼                ▼
      UI/core        private backup     SAF backup
```

Representative domain persistence includes:
- profiles;
- quests/sessions/completions;
- tags;
- XP ledger;
- goals/progress;
- catalog packs/entries;
- flower facts/population evidence;
- flower discovery/acquisition;
- reward opportunities;
- game discovery catalog;
- game observations/history;
- dataset metadata.

DataStore stores UI/runtime preferences such as theme, accent, font scale and model-related preferences.

ModelStore stores model files separately.

---

## 15. Transactions and idempotency

Quest completion is an application-level transaction.

A single completion transaction must:
1. promote the active session to completed;
2. create exactly one canonical completion record;
3. create XP transaction(s);
4. update goal progress;
5. create a reward opportunity;
6. resolve the flower reward when possible;
7. increment dataset revision;
8. commit atomically.

Idempotency constraints must prevent duplicated XP or flowers if a callback, UI action or worker retries.

Representative uniqueness constraints include:
- one canonical completion per session;
- unique completion ID;
- unique flower per profile;
- unique reward opportunity identity.

---

## 16. Dataset revision

Each meaningful domain mutation increments a monotonic `datasetRevision`.

```text
DatasetMetadata
├── profileId
├── datasetRevision
├── schemaVersion
└── lastModifiedAt
```

Revision exists to reason about backup freshness and restore safety. It is not shown as a user-facing score.

---

## 17. Backup and restore

### 17.1 Private backup

The app maintains bounded private recovery snapshots. Backup work must not block quest completion.

### 17.2 Public backup via SAF

The user may choose a public backup directory using Storage Access Framework and persist the granted URI permission.

Public backups are versioned snapshots such as:

```text
PinhoQuest-backup-r107.pqbackup
```

They are not a second writable database.

### 17.3 Portable format

`.pqbackup` is an app-owned, versioned portable container containing domain data and integrity metadata rather than a raw SQLite database dump.

Logical contents include:
- manifest;
- profile;
- quests/history;
- tags;
- XP ledger;
- goals;
- garden;
- catalog packs;
- game catalog/history;
- preferences;
- checksums.

The local LLM model file is not included.

### 17.4 Restore pipeline

```text
.pqbackup
    ↓
read
    ↓
integrity validation
    ↓
schema validation
    ↓
migration pipeline
    ↓
staged restore
    ↓
restore plan
    ↓
user confirmation
    ↓
single Room transaction
```

An older backup must never overwrite a newer local dataset silently.

If the backup belongs to the same profile and has a lower revision, the default action is cancel. A deliberate rollback may exist behind explicit confirmation.

V1 does not attempt automatic bidirectional merge of divergent garden histories.

Catalog packs are restored exactly as captured; restoring a personalized collection must not research a replacement collection.

---

## 18. Navigation and UX

V1 has four main destinations:

1. **Quests**
2. **Tags**
3. **Jardim**
4. **Configurações**

Quests is the default opening screen.

### 18.1 Home / Quests

Primary actions:
- **Sortear Quest**
- **Quest de Jogo**
- **Quest Aleatória**
- optional “Estou com vontade de…” session filters.

There is no infinite feed.

The primary experience is one action → one proposal.

### 18.2 Onboarding

A short onboarding:
1. welcome/purpose;
2. garden owner name;
3. interests and game genres;
4. optional free-form “coisas que eu gosto/tenho disponível” text, converted into user-confirmed derived tags;
5. optional download of the local “cérebro criativo”.

The app remains usable with procedural generation before the model is installed.

### 18.3 Tags

Tags are presented in human terms. Internal affinity numbers are not exposed.

Users can:
- enable/disable;
- explicitly increase/decrease preference;
- add custom tags;
- provide natural-language context;
- review derived suggestions.

### 18.4 Garden

Header example:

```text
🌷 Jardim de Rafa
425 XP
Nível 4
12 / 24 flores
```

Grid adapts to available width and user font scale. Fixed card heights must not clip text.

### 18.5 Settings

V1 settings include:
- System / Light / Dark theme;
- accent/highlight color;
- safe text color controls;
- font size;
- local creativity/model preferences;
- game catalog refresh preference;
- garden expansion preference;
- backup/public folder/export/restore;
- garden owner name;
- About / privacy / sources.

Custom colors must preserve readable contrast. The UI may adjust an unsafe choice and explain this in friendly language.

---

## 19. Voice and user-facing language

The product voice is:
- warm;
- optimistic;
- simple;
- lightly playful;
- never infantilizing;
- never punitive.

Technical internal vocabulary must not leak into normal dialogs.

Examples:

Instead of:
> InferenceUnavailable

Use procedural fallback silently.

Instead of:
> Provider error

Use:
> Não consegui procurar novidades agora. As opções que já conheço continuam disponíveis.

Instead of:
> Backup failed

Use:
> Seu jardim continua salvo neste aparelho. Não consegui atualizar a cópia na pasta escolhida desta vez.

Friendly wording must never claim success when an operation failed.

Technical detail may exist behind an explicit “Ver detalhes” diagnostic path.

---

## 20. Offline behavior

After the local model is installed:
- ordinary quests can be generated offline;
- procedural fallback always remains available;
- cached/rotating game candidates can support Game Quest offline;
- existing flower catalog packs remain fully usable;
- flower expansion waits for network;
- unresolved rewards remain durable until expansion succeeds;
- backups to an unavailable public target fail safely without compromising Room state.

---

## 21. Error contracts

Core operations return explicit semantic outcomes rather than silent empty results.

Representative research outcomes:
- Success;
- Unavailable;
- RateLimited;
- InvalidResponse;
- UnsupportedSource;
- TechnicalFailure.

Representative inference outcomes:
- Success;
- ModelUnavailable;
- InsufficientResources;
- RuntimeUnavailable;
- InvalidOutput;
- TechnicalFailure.

The UI maps these to human language. Core logic retains the precise semantic result for tests and diagnostics.

---

## 22. Testing strategy

Testing protects behavior and boundaries rather than implementation accidents.

### 22.1 Domain/unit tests

Cover:
- Quest validation and state transitions;
- NORMAL/GAME/RANDOM policy behavior;
- tag-source rules;
- XP ledger calculations;
- Lifetime vs Spendable XP;
- level derivation;
- reward uniqueness;
- uniform eligible-flower selection;
- deferred reward convergence;
- log rarity classification;
- incompatible population evidence rejection;
- catalog immutability;
- game catalog rotation;
- restore revision policy.

### 22.2 Adapter/integration tests

Cover:
- Room transactions/idempotency;
- Room migrations;
- DataStore mapping;
- SAF permission persistence;
- `.pqbackup` export/import round trip;
- corrupted backup rejection;
- ModelStore hash validation and atomic promotion;
- inference admission decisions;
- web research parsing/normalization with recorded fixtures;
- offline/stale game catalog behavior.

### 22.3 UI tests

Cover:
- onboarding;
- garden name validation at 1–20 characters;
- dynamic font/card layout without clipping;
- quest start/complete/abandon flows;
- tag confirmation;
- garden discovery states;
- XP investigation confirmation;
- backup/restore dialogs;
- humanized offline/error states.

### 22.4 E2E gates

Representative E2E flows:
1. fresh install → onboarding → procedural quest → completion → XP → unique flower;
2. install local model → admitted local generation → valid quest;
3. device under pressure → procedural fallback with no user-facing technical failure;
4. online game refresh → catalog rotation → offline Game Quest;
5. complete first garden collection → online flower research → validated immutable Collection II;
6. collection completion while offline → pending expansion → later research → deferred reward resolution;
7. export backup → reinstall/fresh state → restore → exact garden/catalog history preserved.

---

## 23. Explicit non-goals for V1

V1 does not include:
- Windows UI/client;
- cloud account sync;
- remote/cloud LLM as the normal composer;
- social feed;
- leaderboard;
- streaks;
- daily-login rewards;
- lootboxes or paid/random rewards;
- premium currency;
- surveillance-based completion verification;
- automatic merge of divergent backups;
- free spatial garden placement.

The architecture must leave room for a future Windows adapter without prebuilding the Windows product now.

---

## 24. Canonical authority map

```text
QuestPlanner
→ decides generation plan

QuestValidator
→ promotes QuestDraft to Quest

InferenceAdmissionController
→ decides whether local LLM may run now

LocalInferenceRuntime
→ executes the installed local model

ModelStore
→ authority for installed model artifacts

GameResearchService
→ authority for normalized external game candidates

GameDiscoveryCatalog
→ bounded local working set of game candidates

FlowerResearchService
→ authority for researched flower candidates/evidence

FlowerRarityScale
→ versioned mapping from global population estimate to rarity class

CatalogPackValidator
→ promotes a generated flower collection

XpLedger / XpPolicy
→ authority for XP accounting and award policy

LevelPolicy
→ derives level from Lifetime XP

RewardEngine
→ resolves reward opportunities

Room
→ authority for live persistent domain state

DataStore
→ authority for UI/runtime preferences

BackupCoordinator
→ creates/restores validated snapshots, never live truth
```

---

## 25. Engineering invariants

The following are hard constraints for V1:

1. A Quest is never created directly by UI, LLM or web research.
2. The LLM never decides factual game availability, botanical identity, rarity, XP or reward ownership.
3. A failed LLM call cannot make the app unable to generate an ordinary quest.
4. Internet refreshes knowledge; it is not required for the app brain.
5. Game history and rotating game catalog are separate data concerns.
6. A real-world flower rarity class must be backed by a defensible global population estimate and a versioned logarithmic scale.
7. Flower rarity does not reduce drop probability.
8. A flower can be collected only once per profile.
9. XP can reveal information but cannot purchase a flower.
10. Spending XP never lowers Lifetime XP or level progress.
11. Room is the only live domain authority.
12. Backups are immutable snapshots with revision/integrity metadata.
13. Older backups never overwrite newer local progress silently.
14. Quest completion is transactional and idempotent.
15. User-facing text remains human and truthful.
16. No implementation may introduce a hardcoded device whitelist, provider-specific bypass or duplicate canonical flow to “make it work”.

---

## 26. Implementation boundary after spec approval

This specification defines the product and architecture, but does not authorize implementation yet.

After user review/approval of this written spec, the next step is to write a detailed implementation plan. That plan will define:
- module/file layout;
- dependency graph;
- Room schema/migrations;
- concrete runtime/model benchmark procedure;
- web research adapter strategy;
- test order and gates;
- staged Android delivery checkpoints.

No product code should be scaffolded before that plan is reviewed according to the project workflow.
