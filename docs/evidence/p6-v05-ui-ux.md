# P6 V0.5 — UI/UX Visual Integration

Date: 2026-10-06
Branch: feature/cr-0-runtime-consolidation
Scope: first functional visual skin pass using the official PinhoQuest screen templates.

## What changed

- Integrated the supplied/official screen artwork as low-opacity visual backgrounds for Quest, Tags, Garden and Settings.
- Integrated the onboarding artwork as a visual background while preserving the existing functional onboarding controls.
- Added a shared PinhoQuest visual language:
  - forest-green primary actions;
  - warm cream surfaces;
  - rounded controls;
  - serif display typography for headings;
  - softer navigation colors.
- Styled tag and settings FilterChips to match the visual language.
- Preserved existing navigation and domain callbacks; no new UI authority was introduced.
- Added the official artwork to Android `drawable-nodpi` resources.
- Added a pull-request CI artifact step for the debug APK so the functional V0.5 build can be retrieved without committing binaries to the repository.

## Functional boundary

The artwork is presentation-only. Existing Compose controls remain the interaction authority.

Therefore:

- quest generation remains the existing governed flow;
- onboarding validation remains unchanged;
- tags remain backed by the existing tag state;
- garden remains backed by the existing GardenUiState;
- settings remain backed by the existing preference callbacks;
- no image contains authoritative application state.

## Validation

- `:app:compileDebugKotlin` — PASS
- `:app:assembleDebug` — PASS
- `:app:connectedDebugAndroidTest` with `OnboardingQuestFlowTest` — PASS, 4/4
- `:app:connectedDebugAndroidTest` with `FoundationActivityE2ETest` — PASS, 1/1
- APK installed successfully on Pixel_4_API_33 / Android 13 during validation.

## Known V0.5 limitation

The supplied screen templates are composite reference screens, so this pass intentionally uses them as atmospheric backgrounds rather than extracting every baked visual into independent assets. The next visual pass can replace individual embedded button artwork with true reusable assets/states (normal, pressed, disabled, selected) without changing the functional contracts.

## Gate

P6 V0.5 visual integration: IMPLEMENTED / FUNCTIONAL_SMOKE_PASS.

This is a UI/UX integration checkpoint, not a V1 release gate.
