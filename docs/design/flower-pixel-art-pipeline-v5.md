# Flower Pixel-Art Pipeline v5

**Date:** 2026-10-22
**Branch:** `feature/p6-garden-delight`
**Pipeline ID:** `flower-pixel-v5`
**Supersedes:** v4, whose API 28 real-photo test demonstrated loss of green foliage

## Why v4 was rejected

The real *Camellia granthamiana* fixture passed the non-empty-sprite checks but failed the explicit green-pixel assertion. Its foliage and garden background share similar colour ranges, so the corner-based flood fill still removed essentially all green leaves around the bloom. The failing test was kept strict; its threshold was not lowered.

## v5 cutout change

After the existing colour-based background flood-fill has isolated the non-background bloom, the processor runs a bounded, multi-source breadth-first distance map from the surviving flower silhouette. It restores only plant-green pixels originally masked as background when they fall within a small halo of the remaining flower, with radius `max(6, min(width, height) / 16)` pixels in the downsampled source.

This is deliberately a local foliage-preservation heuristic, not a semantic segmentation model. Pixels outside the halo remain governed by the background mask, so v5 does not simply make all green parts of the photograph opaque. Existing connected-component cleanup, foreground crop, transparent output, 48 × 48 working grid, palette reduction, shading and 96 × 96 nearest-neighbour upscale remain unchanged. The cache/version gate now uses `flower-pixel-v5`; old v3/v4 sprites and rejection markers must be regenerated.

## Reproducible visual gate

The Android instrumentation test uses the unmodified, attributed CC BY-SA 2.0 photo documented in `app/src/androidTest/assets/flower_fixtures/README.md`. It exercises the actual production processor. It checks the 96 × 96 size, visible silhouette, transparency and at least eight visible pixels matching the green-leaf criterion.

**Evidence is saved to the target app's private cache before the quality assertions.** The CI step extracts the known filenames with `adb exec-out run-as com.pinhoquest cat ...`, avoiding fragile external-storage aliases. The instrumented test creates:
- `camellia-original.jpg` — exact source photograph,
- `camellia-sprite.png` — raw engine output,
- `camellia-original-vs-sprite.png` — labeled, side-by-side comparison,
- `camellia-garden-collection-screen.png` — actual Compose collection UI with both the processed Camellia and an intentionally unresolved *Didymocarpus phuquocensis* entry.

This ordering means failed green-retention assertions should still leave screenshots for diagnosis. The API 28 CI step performs test, evidence pull and exit-code return in one shell line because multiline state was not preserved by the emulator action's command handling. The `pinhoquest-flower-visual-evidence` artifact is retained for seven days.

## Placeholder diagnosis

An exact-phrase Wikimedia Commons API query for `Didymocarpus phuquocensis` returned no candidate on 2026-10-22. That identifies missing coverage in the current provider/search path as a plausible reason for the tulip placeholder; it does not prove that no usable photograph exists under a synonym or another source. The test intentionally keeps the placeholder visible and never substitutes the Camellia or another unrelated species.

## Acceptance criteria

The v5 implementation is **not accepted solely because it builds or its assertions pass**. Inspect the original-versus-sprite comparison and collection screenshot from the same API 28 run. The foliage must be recognisable beside the flower and the larger garden background must stay transparent. If v5 retains an obviously rectangular/green backdrop or destroys the flower outline, refine the segmentation again instead of weakening the checks.

## Licensing

Source: Wikimedia Commons file page linked in `app/src/androidTest/assets/flower_fixtures/README.md`; photographer and CC BY-SA 2.0 attribution are recorded there. The test fixture is packaged in the instrumentation-test assets, not production app assets.
