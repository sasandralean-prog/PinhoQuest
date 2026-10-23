# Flower Pixel-Art Pipeline v6

**Date:** 2026-10-23
**Branch:** `feature/p6-garden-delight`
**Pipeline ID:** `flower-pixel-v6`
**Supersedes:** v5, rejected after direct inspection of its recovered 96 × 96 sprite

## Findings from the real-photo experiment

The v5 photo test had originally used weak thresholds (more than 180 opaque pixels and 8 green pixels). After the logcat evidence export was repaired, the actual sprite was retrieved and inspected. It contained only 468 visible pixels, 112 near-neutral/pale pixels, a largest connected component of 128 pixels and 15 separate opaque components. That is not a credible flower cutout, even though the old numeric assertions passed.

The photo is a useful hard case: a large white camellia blossom is surrounded by dense, glossy green leaves. The previous flood-fill allowed a colour gradient to propagate around the edges of the petals and remove most of the flower. Separately, v5 seeded the nearby-green recovery distance map from every remaining foreground pixel, meaning that small residual background speckles could act as false foliage seeds. Both behaviors are corrected in v6.

## v6 processing changes

1. **Protected central bloom colours.** The flood-fill now protects likely pale, red/pink/purple, blue and warm pollen colours near the central bloom, including when their colour happens to match a sampled background tone. This spatial/colour heuristic is deliberately bounded around the photo centre; it is not semantic segmentation and does not claim to solve off-centre blooms.
2. **Tighter background drift.** The smooth-gradient distance limits have been reduced. This prevents gradual background colour changes from eating into light petals as readily as v5 did.
3. **Bloom-seeded foliage halo.** The green recovery pass now starts only at retained central bloom detail, not from every arbitrary pixel that survived flood-fill. It restores green pixels only within a smaller, bounded distance of that bloom area. This reduces the risk of converting distant garden leaves or speckles into foreground.
4. **Component protection.** Small disconnected components are normally removed as texture noise, except components whose colour/location match likely bloom detail. This prevents the source-resolution cleanup from deleting small but real petal fragments before downscaling.
5. **Cache invalidation.** The runtime pipeline version is bumped to `flower-pixel-v6`, so v5 sprites and rejection markers are regenerated and cannot masquerade as validated output.

The existing 48 × 48 working grid, compact colour palette, transparency, directional shading and 96 × 96 nearest-neighbour upscale remain unchanged.

## Stronger real-photo acceptance test

The API 28 Android instrumentation test processes the unmodified, attributed *Camellia granthamiana* photo documented in `app/src/androidTest/assets/flower_fixtures/README.md` with the actual production engine. Before its quality assertions, it emits:
- the exact original JPEG,
- the unmodified engine PNG output,
- a labeled original-vs-sprite JPEG on a checkerboard background,
- a JPEG screenshot of the Compose Garden collection, with the Camellia sprite and the still-unresolved *Didymocarpus phuquocensis* placeholder visible together.

The assertions now require:
- a 96 × 96 output,
- more than 900 visible pixels,
- more than 220 near-neutral/pale petal pixels,
- a connected opaque component larger than 400 pixels,
- at least 40 green plant pixels,
- green pixels occupying less than 70% of visible pixels,
- at least 80 warm yellow/orange centre pixels,
- a transparent canvas.

These thresholds directly reject the v5 sprite measured above and distinguish a tiny residue or all-green patch from a flower with retained petals, centre and nearby foliage. The thresholds are intentionally not reduced to get a passing CI run.

## Evidence export reliability

The Android emulator runner resets the logcat buffer to a small default size and does not reliably permit test artifacts to be written to shared Downloads. v6 keeps the emitted files in test-private temporary storage and streams each file as indexed Base64 chunks under the `PqFlowerEvidence` logcat tag. Each file header includes expected length and SHA-256.

`tools/flower_evidence/extract_logcat.py` reconstructs the four expected images after instrumentation, checking chunk completeness, Base64 validity, byte length, JPEG/PNG signature and SHA-256. It fails closed if any image is missing/corrupted. The comparison and UI screenshot use JPEG to reduce log volume; the sprite remains PNG to preserve alpha. Two Python unit tests cover normal chunk reconstruction and rejection of missing/bad data.

## Known limitations / separate placeholder

The engine is deterministic colour segmentation, not ML object segmentation. It still needs visual inspection for difficult photos, especially off-centre flowers, green blossoms and backgrounds with petal-like colours. The test must remain a quality gate rather than merely a build check.

The unresolved *Didymocarpus phuquocensis* card is intentionally shown beside the Camellia in the UI screenshot. The current exact-phrase Wikimedia Commons provider search did not return a candidate; that is a separate source/provider coverage problem. Do not display an unrelated species to conceal the placeholder.

## Acceptance gate

**v6 is not accepted based on compilation or a green unit-test suite alone.** Close the visual gate only after the API 28 run uploads checksum-verified original, sprite, comparison and collection screenshot files and the comparison visibly shows:
- the recognizable white camellia blossom and warm centre,
- nearby green foliage,
- a transparent exterior without a broad rectangular chunk of garden background,
- the unresolved Didymocarpus placeholder still distinguishable in the collection screenshot.
