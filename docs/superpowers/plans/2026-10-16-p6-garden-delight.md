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

- Added `app/src/main/assets/videos/garden_loop.mp4`, compact H.264 silent loop, 160×280 at 8 fps, 8,054 bytes, SHA-256 `a09b9266a4373c68055f740ef24d3f92f762afa295100bd0b691227a4de63d73`.
- Added a Media3 ExoPlayer background composable bound to lifecycle, muted and looped. Night theme uses the video; day theme retains its approved static artwork. Missing/corrupt/mismatched asset falls back to the static background.
- The runtime verifies the exact packaged asset SHA-256 before copying it to cache for playback.
- Do not bundle the music into the video; soundtrack playback remains separate until the music file is supplied.
- Device performance and battery/thermal checks are still pending.

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


## Implementation checkpoint — video + persistent tags + provenance (2026-10-16)

### Night garden video
- Binary added at `app/src/main/assets/videos/garden_loop.mp4` (8,054 bytes; SHA-256 `a09b9266a4373c68055f740ef24d3f92f762afa295100bd0b691227a4de63d73`).
- Original user-supplied video SHA-256: `26d6a82fd79e897b241d05388bc4c6ec94083d5684652612ac591c4fe0f10a43` (2,007,561 bytes; 496×864; ~6.04 s). Compact encoded variant has separate SHA-256 and does not retain audio.
- Playback uses AndroidX Media3, mute=0, repeat-one, crop/zoom, lifecycle pause/resume, release on dispose, and SHA-256 verification. It is used on the night-theme Garden; day mode preserves the approved daytime art.
- Rights caveat: the video was supplied by the project owner, but ownership/license allowing public APK redistribution has not been independently established. See `docs/design/video_sources/garden-loop-provenance.json`; release remains blocked on confirmation.

### Custom tags
- Extended the existing Room-backed `tags` table, no new persistence store and no schema migration.
- USER-created tags support create/rename/delete, stable UUID-based IDs, label trim/validation (1–24 chars), case-insensitive profile-wide uniqueness, and a hard UI/repository cap of 10 USER tags. Built-in SYSTEM tags are unaffected.
- Profile UI shows the current count and handles adding/editing/removing. Persistence is through the existing canonical `RoomTagRepository`.
- Tests/CI still need to validate repository concurrency, backup round trip, process recreation and on-device UX. These custom labels are persisted and displayed, but their effect on quest ranking/generation should be considered a distinct follow-up unless covered by a future validated affinity mapping.

### Photo/sprite source register
- Documented the discovery → license gate → exact-byte hashing → metadata sidecar → background removal → deterministic pixelization → QA → derived-art hash → catalog registration pipeline in `docs/superpowers/plans/2026-10-16-flower-photo-sprite-engine.md`.
- Registered one Wikimedia Commons candidate in `docs/design/flora_sources/records/34f1bf5a9bf54d8000a01cec5b248a2e044129d5.json`: “Flower photograph.jpg”, credited to Anandha abirami Govindan, declared CC BY-SA 4.0, file page revision oldid 1255310682. The provider page exposes SHA-1 `34f1bf5a9bf54d8000a01cec5b248a2e044129d5`, not a SHA-256.
- **Explicit blocker:** the source photo bytes could not be fetched into the build workspace during this pass, so no project-computed SHA-256, stored original, processed derivative or sprite hash is claimed. The record is marked candidate/pending and release-disabled; do not treat it as an admitted sprite. Before processing, fetch the exact original bytes, compute SHA-256 locally, store the immutable original if redistribution permits, update the sidecar/registry, verify taxonomy, then transform.
