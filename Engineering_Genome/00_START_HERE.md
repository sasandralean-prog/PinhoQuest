# Engineering Genome — 00 START HERE

Pinho Quest is a personal anti-boredom micro-adventure generator.

The repository should be approached as a governed system, not as a pile of screens and callbacks.

## Read order

1. `docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`
2. `architecture/PINHO_QUEST_V1_ARCHITECTURE.md`
3. `Engineering_Genome/01_ENGINEERING_PHILOSOPHY.md`
4. `Engineering_Genome/02_AUTHORITY_MAP.md`
5. `Engineering_Genome/10_RAFA_LUCIO_HANDOFF.md`
6. `docs/superpowers/plans/2026-10-01-pinho-quest-v1-roadmap.md`

## Product intent

The app should answer one question well:

> “Estou entediado. Me dá alguma coisa legal para fazer agora.”

The expected loop is:

```text
Tédio → Sortear → Fazer → Concluir → Ganhar progresso/flor → Sair do app
```

The app is not intended to maximize time spent inside itself.

## V1 identity

- Android first.
- Pure Kotlin/JVM domain and core.
- Android adapters around the core.
- Future Windows frontend supported by the same domain/core contracts.
- Local LLM available from V1.
- Procedural fallback always available.
- Internet research available from V1 for games and real flower catalog expansion.
- Room is the canonical live state.
- Public/private backups are snapshots.
- Four main tabs: Quests, Tags, Jardim, Configurações.
- Garden name is user-owned, 1–20 trimmed characters.
- Friendly, optimistic, truthful language; technical details stay behind diagnostics.

## Non-negotiable behavior

- No hardcoded device-specific bypass.
- No duplicated canonical flow.
- No LLM as factual authority.
- No silent data loss.
- No fake success messages.
- No streaks, FOMO, lootboxes, daily-login pressure or punitive retention.
- No weakening a validation gate merely to make a flow pass.
- No implementation work before the approved design and implementation plan gates are satisfied.

## Canonical design authority

The canonical V1 design is:

`docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`

Supporting documentation summarizes it; supporting docs must be changed together when architecture changes materially.

## Runtime consolidation frontier

Operational project state: CURRENT_STATE.md
P3 runtime consolidation plan: docs/consolidationruntime.md

The CR sprint family (CR-0 onward) governs the convergence of the local FunctionGemma runtime. Read the consolidation plan before modifying P3 inference contracts.

## Current application frontier — 2026-10-05

P5 is **CLOSED at the functional application gate** after CR-9/CR-9.1 productive Android evidence and the P5 UI suite passed 10/10 on Pixel_4_API_33 / Android 13. A known LiteRT-LM/Kotlin metadata limitation remains bounded at lint analysis; it is not being hidden by runtime changes.

## P6 visual implementation and transition protocol

Before any P6 UI, asset, garden-state, quest-generation-diagnostic or navigation change, read:

- `Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md` — official P6 scope, document authority, confirmed findings vs hypotheses, gates P6-A through P6-E, evidence requirements, checkpoint and rollback protocol.
- `docs/design/ASSET_CATALOG.md` — asset filenames and generated Android resource aliases.
- `docs/design/CANONICAL_GRAPHICS.md` — classified visual compositions and reference roles.
- `docs/design/UI_DESIGN_CONTRACT.md` — implementation/accessibility boundary.
- `docs/design/P6_UI_UX_INTERACTION_CONTRACT.md` — product interaction decisions.

The three global bottom-navigation destinations in the current P6 visual contract are **Início, Jardim and Perfil**. Settings, onboarding, tag selection and flower details are internal destinations/flows unless the product authority is explicitly changed. The historical four-tab statement elsewhere in this file is legacy wording and must not be used to override the current P6 contracts; reconcile it during the documentation gate.

A protocol or a green build does not close a P6 gate. Record the actual branch, HEAD SHA, test command, result and linked evidence in `CURRENT_STATE.md`.
