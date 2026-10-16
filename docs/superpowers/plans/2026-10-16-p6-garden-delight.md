# P6 — Garden delight follow-up implementation plan

Status: in progress on `feature/p6-garden-delight`
Base: `feature/p6-total-ui-refactor` (P6-C/P6-E owner-approved checkpoint)

## Scope from Rafa

1. Open the creator-support destination supplied by the product owner:
   `https://buy.stripe.com/5kQ00i75X64v9So31QcV201`
2. Add subtle, lightweight fireflies on the Quests/Home, Garden and Profile screens.
3. Replace the static Garden backdrop with the supplied short vertical video, looping without embedded audio.
4. Build a deterministic flower-art pipeline: find real flower photographs via Wikimedia/Wikipedia APIs, preserve source/author/license metadata, remove backgrounds, quantize to pixel art, generate transparent PNG sprites, cache and map them to flower definitions.
5. Add up to ten user-created custom tags without limiting built-in system tags or silently rewriting existing preferences.
6. Keep soundtrack playback separate from the background video; the music asset will be supplied later.

## Implemented in this branch

- The existing creator-support button now opens the canonical Stripe URL using Android `ACTION_VIEW`; if no activity can open it, the app shows a human-readable error.
- Added a single Compose Canvas overlay for subtle upward-moving, softly glowing fireflies. It is installed on Home/Quests, Garden and Profile. It is decorative and does not intercept taps.
- The effect is deliberately bounded (12–14 particles) and uses one animation clock instead of one composable/animation per firefly.

## Remaining implementation slices

### A. Garden video backdrop

- The current repository contains static drawable backgrounds but no matching video asset at the branch tip. Add the owner's video to a tracked app asset path before implementing playback.
- Use a lifecycle-aware player, muted video, repeat mode, crop/fit behavior documented for the supplied 496×864 vertical clip, and release the player when the screen is no longer active.
- Keep a static image fallback when video decoding fails or reduced-motion is enabled.
- Do not bundle the music into the video; soundtrack playback is a separate decision after the music file is supplied.
- Verify battery/thermal behavior and APK size on a low-memory Android phone.

### B. Flower photo-to-sprite pipeline

- Research via Wikimedia Commons API rather than scraping rendered Wikipedia pages.
- Persist source page URI, file URI, author, license identifier/URI, attribution text, and retrieval timestamp with every candidate.
- Reject candidates with missing/unsupported licensing metadata; never assume that a photo on Wikipedia is public domain.
- Run background removal and pixel-art conversion as an offline asset-generation step (not on every app launch). Validate transparent edges, silhouette retention, palette bounds, output dimensions, and alpha channel.
- Cache generated sprites by source-file hash + pipeline version. Keep the existing placeholder as a fallback if generation fails.
- Do not ship credentials/secrets; use public APIs and rate limits.
- Add fixture-based tests for metadata parsing, license filtering, and deterministic quantization. Background removal quality needs a chosen implementation and benchmark before selecting an on-device model/library.

### C. Custom tags

- Reuse the existing tag source model and Room repository as the authority; do not create a parallel preference store.
- Add create/rename/delete operations for USER tags, enforce at most ten USER-created tags per profile, and preserve SYSTEM tags.
- Validate trimmed labels (non-empty, bounded length, case-insensitive duplicate check).
- Confirm how tag deletion affects history and derived suggestions before implementing destructive behavior; prefer deactivation/archive if historical references require stable IDs.
- Add UI entry and list management in Profile/Tags, with visible count (for example, 3/10), accessible focus/keyboard behavior, and process-death persistence tests.
- Include custom tags in backup/restore and ensure tag IDs remain stable.

### D. Support-payment configuration

- The app only opens the supplied Stripe Payment Link. The minimum contribution (R$ 3,00) and whether the payer may choose any higher amount are controlled by that link's Stripe configuration, not Android code.
- Before release, verify in Stripe Dashboard/test mode that the payment page actually permits a minimum of R$ 3,00 and no maximum. Do not claim those constraints are enforced by the app itself.

## Validation gates

- Run Android CI and inspect the exact failing step if red.
- Add UI tests proving firefly overlay does not intercept taps and appears only on the intended screens.
- Add video lifecycle/fallback tests once the source clip is committed.
- Add sprite metadata/license/transform tests before enabling generated sprites in the live garden.
- Add Room/repository/UI tests for the ten-custom-tag cap, duplicate labels, deletion/deactivation, and backup round-trip.
- Validate on the target Android device with the largest in-app font scale, day/night themes, and reduced motion where supported.

## Safety and architecture constraints

- Room remains the only authority for live profile/tag/garden data.
- External botanical facts and photo attribution remain traceable.
- Generated art never replaces a valid flower identity with an unverified claim.
- Fireflies and video are presentation-only; they must not alter quest/garden state.
- The soundtrack remains optional and separate from the video.
