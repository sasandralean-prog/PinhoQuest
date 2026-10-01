# Pinho Quest V1 — Implementation Roadmap

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to execute the child plans task-by-task.

**Goal:** Deliver the approved Pinho Quest V1 as a sequence of independently reviewable vertical slices.

**Architecture:** The V1 is split into five ordered plans because the approved spec spans independent subsystems with different failure modes. Each plan must leave the repository buildable and testable before the next plan starts.

**Tech Stack:** Kotlin/JVM core, Android/Kotlin, Jetpack Compose, Room, DataStore, WorkManager, SAF, on-device LLM runtime, HTTP/web research adapters.

**Spec:** `docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md`

## Global Constraints

- Android ships first; the domain/core stays free of Android SDK types.
- Ordinary quests remain usable offline after the local model is installed; procedural generation remains available regardless.
- Internet is V1 functionality for game discovery/refresh and real-flower catalog research/expansion.
- Room is the only live domain-state authority.
- Backups are snapshots, never a second live database.
- LLM output is creative input only; it never becomes factual authority for games, flowers, rarity, XP, or reward ownership.
- No device-name whitelist, validation bypass, duplicated canonical flow, streak, FOMO, lootbox, daily-login pressure, or punitive retention mechanic.
- User-facing copy stays warm, simple, optimistic, lightly playful, and truthful.

## Review Focus

- Process death between a user action and persistence must not duplicate or lose canonical state.
- Offline mode must preserve ordinary quests and previously cached game/catalog behavior without pretending stale data is fresh.
- Large font/display scaling must not clip cards, navigation, quest objectives, or garden labels.
- Retry/recreation must not duplicate XP, completions, rewards, model downloads, or restores.
- External-data corruption or source changes must fail closed into explicit semantic outcomes, not silently promote bad facts.

---

## Ordered plans

1. `docs/superpowers/plans/2026-10-01-pinho-quest-foundation.md`
   - Android shell, module boundaries, domain/core, Room v1, DataStore, profile/tags, procedural Quest flow, four-tab UI, baseline humanized copy.

2. `docs/superpowers/plans/2026-10-01-pinho-quest-progression-garden.md`
   - XP ledger, levels, completion transaction, Garden, flower discovery, rarity scale, unique drops, first embedded catalog.

3. `docs/superpowers/plans/2026-10-01-pinho-quest-local-llm.md`
   - ModelStore, package verification, inference admission, on-device composer, fallback, local execution profile.

4. `docs/superpowers/plans/2026-10-01-pinho-quest-web-research.md`
   - Integrated game/flower web research, provenance, bounded rotating game catalog, offline Game Quest, dynamic catalog expansion.

5. `docs/superpowers/plans/2026-10-01-pinho-quest-backup-polish-e2e.md`
   - Private/public backups, SAF, restore/migrations, appearance/accessibility hardening, final E2E/release gate.

## Gate between plans

A plan is complete only when:
- all tests named in that plan are green;
- `assembleDebug` and lint are green;
- any runtime evidence named in that plan is captured;
- documentation still matches owners/contracts;
- no correctness regression from the plan remains open.

If implementation pressure reveals an incorrect authority boundary, stop and amend the design/spec rather than creating a bypass.