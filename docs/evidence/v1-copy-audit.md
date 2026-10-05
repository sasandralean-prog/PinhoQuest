# V1 user-facing language audit

## Scope

P5.5 audits primary user-facing messages for backup/restore, model installation and quest-generation failures.

The UI presents semantic outcomes rather than implementation details. Diagnostics may retain technical reasons elsewhere, but primary copy must remain understandable and truthful.

## Rules

- Do not expose Room, provider names, inference/runtime class names, exception names or raw schema/transport errors in primary copy.
- Explain what happened in terms of the user's action.
- State when the existing local garden/progress remains safe.
- Do not claim a backup succeeded unless the backup target actually completed.
- Invalid restore is fail-closed: current local state remains intact.
- Copy remains warm, concise and non-coercive.

## Covered mappings

- no profile available for backup
- ambiguous multiple-profile backup state
- invalid snapshot state
- invalid/incompatible restore
- model installation rejection
- stale/unavailable Game Quest catalog
- generic quest-generation failure
- invalid session transitions

## Evidence

UserFacingCopyTest asserts that backup/restore messages remain non-empty and do not expose implementation terms such as Room, exception, provider, inference or raw schema wording.

The complete app test cannot currently execute because :app:compileDebugKotlin is blocked by the existing LiteRT-LM 0.17.1 Kotlin metadata mismatch (dependency metadata 2.4.0 vs project compiler metadata 2.1.0). No runtime bypass or parser fallback was introduced to work around this.

## Accessibility boundary

P5.5 also removes emoji-only navigation semantics and gives the quest-generation loading indicator an explicit spoken description. Main screens remain vertically scrollable and contain no fixed-height text containers in the current UI tree.
