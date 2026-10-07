# Pinho Quest — UI / Visual Implementation Contract

**Status:** canonical implementation authority
**Scope:** P4–P7
**Purpose:** reconstruct the canonical Pinho Quest experience with real, responsive UI components while preserving the product’s visual identity and domain boundaries.

## 1. Canonical visual references

The canonical visual references are indexed by `docs/design/CANONICAL_GRAPHICS.md` and the authored assets under `docs/design/Screen/`, `docs/design/BackGround/` and `docs/design /Button/`.

The registry identifies which files are canonical, secondary, decorative, or non-canonical. In particular, **Home1 remains the primary/source-of-truth composition** and is refined rather than redesigned.

They define:
- composition;
- visual hierarchy;
- approximate spacing and proportions;
- visual language;
- palette and material language;
- navigation;
- interaction states;
- component relationships.

They are **reference compositions**, not screenshots to be placed as one static image.

The implementation must reconstruct the interface with real components and governed state. Reference screenshots and full-screen compositions are never the sole rendering mechanism for interactive UI.

## 2. Implementation boundary

### Background
Raster assets may be used when they preserve the intended scenery, atmosphere, texture, or environmental composition.

### UI
These must remain real UI components:
- text;
- buttons;
- cards;
- navigation;
- chips/tags;
- fields;
- selections;
- toggles and controls;
- loading, empty, success, failure and disabled states.

A screenshot must never become the sole rendering mechanism for interactive UI.

### Decoration
Transparent raster assets may be used for decorative elements when this preserves the authored artwork more faithfully.

Decorative assets must not silently own domain state or interaction semantics.

## 3. Responsiveness

The composition must survive different:
- screen sizes;
- aspect ratios;
- density buckets;
- font sizes;
- light/dark appearance where supported.

Absolute positioning is acceptable only for authored decorative composition where it does not compromise usability or responsive structure.

Text must not be baked into raster artwork when it is expected to adapt to user settings or state.

## 4. Interaction consistency

The visual appearance must remain coherent across all relevant states:
- idle;
- pressed;
- selected;
- disabled;
- loading;
- success;
- empty;
- failure/offline.

State changes must be represented by real component state, not by swapping a whole-screen screenshot.

## 5. Visual hierarchy

Each screen must have a clear purpose and a visually identifiable primary action.

Primary, secondary, tertiary and destructive actions must remain distinguishable.

Do not create multiple competing visual primaries merely because several actions are available.

## 6. Shared components

Repeated visual structures must use shared components/tokens rather than screen-specific copies.

The bottom navigation is one global reusable component wherever it is present:
- same destination order;
- same semantics;
- same selected/unselected behavior;
- same touch behavior;
- current destination remains identifiable without color alone.

A visual change to a shared component is a global change, not a local screen tweak.

## 7. Domain and data boundary

The visual layer renders governed state; it does not invent facts.

It must not:
- invent quest/game/flower metadata;
- infer domain identity from rendered text;
- duplicate navigation ownership;
- hardcode transient domain state;
- replace persistence with visual state.

Factual content that requires provenance must come from the appropriate catalog/data authority.

## 8. Accessibility and personalization

The implementation must preserve:
- readable contrast;
- adequate touch targets;
- semantic labels/content descriptions where needed;
- user-selected font size;
- light/dark behavior;
- reduced-motion expectations where applicable.

Large-font layouts are a first-class composition constraint, not a post-hoc patch.

## 9. Canonical authority order

For visual/UX decisions, use this order:

1. `docs/identity/PINHO_QUEST_VISUAL_IDENTITY_GENOME.md`
2. `docs/identity/PINHO_QUEST_GARDEN_PIXEL_ART_GENOME.md`
3. this `docs/design/UI_DESIGN_CONTRACT.md`
4. `docs/design/CANONICAL_GRAPHICS.md`
5. the individual references explicitly classified by that registry

The contract must not contradict the identity genomes. If a visual decision requires changing a canonical identity rule, update the relevant identity genome first, then reconcile this contract.

## 10. Phase boundary

P4 establishes stable structure and content/data behavior without turning into a visual redesign.

P5 closes reliability, settings, backup/restore, accessibility and V1 behavior.

P6 is the main visual refinement phase: spacing, typography, microinteractions, composition polish and distribution.

P7 deepens the Living Garden, flowers, rarity, provenance and pixel-art discovery.

Detailed pixel-level polish should remain in P6/P7 unless required earlier for correctness or accessibility.

## 11. Canonical graphics boundary

Authored button and field artwork may skin real interactive components, but must never become hidden interaction owners. A raster reference cannot intercept touches from a real input, button, navigation item, or other control.

Home1 preserves exactly three quest actions — **SORTEAR QUEST**, **Quest de Jogo**, **Quest Aleatória** — and the single continuous bottom navigation **Início | Jardim | Perfil**. Visual references may refine spacing, texture, typography and composition, but may not invent replacement mechanics or a fourth navigation destination.

## 12. Canonical design rule

> **Pinho Quest should feel like a place to visit, not a tool to operate.**

The architecture may remain rigorous underneath; the interface should feel warm, calm, intuitive and alive.
