# PinhoQuest — P6 UI/UX Interaction Contract

**Status:** canonical product/interaction contract for the current P6 refactor  
**Scope:** Home/Quest, Themes/Tags, Profile, Garden collections and flower discovery

This document records the decisions made during the P6 UI/UX reconstruction so visual implementation does not silently redefine domain behavior.

## 1. Home / Quest actions

The Home screen keeps exactly three quest actions:

1. **SORTEAR QUEST**
2. **Quest de Jogo**
3. **Quest Aleatória**

### 1.1 SORTEAR QUEST

Opens the category/theme selection experience.

The user may select any combination of available themes, including all themes simultaneously.

The app remembers the last selection and reuses it for later **SORTEAR QUEST** actions until the user changes it.

### 1.2 Quest Aleatória

The quest composer receives the currently enabled themes/tags and has maximum creative freedom within the governed quest-generation contract.

The selected themes and their enabled tags are context, not a hardcoded quest template.

### 1.3 Quest de Jogo

The game catalog supplies the game context. The activity itself is generated dynamically.

The selected game must be explicitly represented in the resulting quest.

The game catalog is dynamically cached; the quest generator does not replace that catalog with a second ad-hoc source of game facts.

## 2. Themes and tags

Themes and tags are separate concepts.

### Theme

A selectable product-level category such as:

- Jogos
- Criatividade
- Aprender
- Música
- Fotografia
- Natureza
- Tecnologia
- Animais
- Aventuras
- Relaxar
- Criar
- Fantasia

The exact canonical set is owned by the theme catalog, not by the UI.

### Tag

A finer-grained semantic input used by quest generation.

A tag may belong to **multiple themes**.

Example:

`fotografar pássaros`

may belong to:

- Fotografia
- Animais

When a theme is enabled, its associated tags become eligible generation context.

### Custom tags

There may be an arbitrary number of persistent custom tags.

A custom tag:

- is stored as user data;
- belongs to one or more themes;
- participates in future quest generation while enabled;
- does not create a new top-level navigation destination;
- is not a replacement for the theme selector.

The free-form preference field is therefore a semantic input surface whose normalized result becomes one or more governed custom tags/theme associations. Raw free-form text must not be passed directly to rendering or uncontrolled model prompts.

## 3. Hybrid quests

Hybrid quests are allowed.

A generated hybrid quest may use **at most three semantic tags** as its explicit hybrid combination.

The limit applies to the hybrid combination itself; it does not mean the profile is limited to three enabled preferences.

The generation layer remains responsible for selecting and validating the final semantic set.

## 4. Selection persistence

The current theme/tag selection is persistent.

The UI must restore the last valid selection rather than resetting every time the selector is opened.

Selection state belongs to the appropriate profile/preferences authority. The Compose screen must not become the persistence owner.

## 5. Visual interaction rule

The authored button PNGs are skins for real controls.

They do not own:

- click handling;
- navigation;
- selection state;
- enabled state;
- accessibility semantics;
- domain state.

Selected controls use the shared **yellow glow** treatment.

The same semantic control may reuse one asset in both day and night modes, or use a mode-specific variant where the authored design provides one.

## 6. Garden model

The Garden contains both:

1. the visual garden/place; and
2. the flower collection/catalog experience.

The collection is not a second unrelated progression system.

### 6.1 Collection size

The initial collection size is **9 flowers**.

Each collection has its own progression.

### 6.2 Flower identity

Every flower has an immutable identity hash derived from its **scientific name**.

The scientific-name identity is the canonical deduplication key.

A flower must never appear twice across generated collections when the canonical scientific-name identity is already present.

### 6.3 Collection identity

A collection has its own immutable hash derived from the hashes of its nine flowers.

Conceptually:

`collectionHash = H(sorted(flowerHash_1 ... flowerHash_9))`

The exact hashing implementation belongs to the domain/data layer.

### 6.4 Collection generation

There is a global flower catalog.

The catalog may be populated from reputable botanical sources and Wikipedia's botanical category, subject to the project's research/provenance rules.

Collections are assembled **locally** from the global catalog.

A collection is therefore a deterministic product of:

- the available canonical flower catalog;
- the collection-generation algorithm;
- the uniqueness constraints;
- the resulting nine flower identities.

## 7. Flower discovery states

The flower's discovery state changes how it is presented; it does not create a second flower identity.

The current conceptual states are:

- **Desconhecida**
- **Pesquisada**
- **Coletada**

A researched flower may subsequently become collected.

The same immutable flower identity remains throughout.

### 7.1 Unknown

The card can show an obscured/placeholder representation.

The user does not yet receive the full botanical identity.

### 7.2 Researched

Research XP reveals the botanical identity/information.

The UI can show the researched flower while it is still not collected.

### 7.3 Collected

Completing any valid quest awards a flower and quest XP.

The awarded flower becomes collected and its generated pixel-art representation is revealed.

The collection card can retain the provenance of the quest that awarded it.

## 8. Two parallel XP progressions

The Garden uses two simultaneous progressions.

### Lifetime / level XP

- increases when quests are completed;
- determines player level;
- is never reduced by flower research.

### Research XP

- is derived from the accumulated progression/reward economy;
- is spendable on flower research;
- decreases when research is purchased;
- progresses independently from level XP.

Therefore:

**research spending must never reduce lifetime XP or level.**

The UI should make this distinction understandable rather than exposing internal ledger terminology.

## 9. Completing a collection

When all nine flowers in the active collection are collected, completion unlocks/generates the next collection.

The next collection must be generated **in background/cache**, rather than waiting for the user to open the Garden.

The generation must be governed and idempotent:

- do not generate duplicate collections for the same completed collection;
- persist the generated collection identity;
- never reuse a flower identity already consumed by an earlier collection;
- preserve the generated collection across process death;
- keep generation work outside the UI thread.

If background generation fails, the existing completed collection remains valid and the next-generation work is retryable. Failure must not corrupt the current collection.

## 10. Flower research and generated artwork

When a flower becomes collected/revealed, the pipeline may transform a real reference photograph into the game's pixel-art representation through the governed image-processing pipeline.

The UI consumes the resulting canonical asset by flower identity.

The UI must not:

- infer flower identity from image pixels;
- generate placeholder filenames from display names;
- substitute an arbitrary flower image when the canonical asset is unavailable.

## 11. Flower detail interaction

Pressing a discovered/researched/collected flower opens a compact detail surface.

The detail surface may show:

- common name;
- scientific name;
- rarity;
- botanical description;
- discovery/research state;
- XP associated with the quest that awarded it, when applicable;
- the quest that caused its acquisition, when applicable.

For an unknown flower, only the information allowed by its current discovery state is shown.

## 12. UI authority boundary

The UI renders state supplied by domain/data authorities.

It must not invent:

- flower identity;
- flower rarity;
- collection membership;
- game metadata;
- quest semantics;
- theme/tag relationships.

When a visual reference contains sample values such as a name, level, XP count or flower, those values are composition examples only.

## 13. P6 implementation order

The implementation should proceed in small verified checkpoints:

1. canonical button assets and shared controls;
2. Home/Quest composition;
3. Theme selection and persistent selection state;
4. custom tag/theme association UI;
5. Garden collection composition;
6. flower detail surface;
7. collection completion → background next-generation orchestration;
8. visual polish and accessibility regression;
9. E2E validation.

No step should introduce a parallel navigation authority or duplicate domain state.
