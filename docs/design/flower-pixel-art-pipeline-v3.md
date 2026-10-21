# Flower Pixel-Art Pipeline v3

**Date:** 2026-10-21
**Branch:** `feature/p6-garden-delight`
**Pipeline ID:** `flower-pixel-v3`
**APK:** `app/build/outputs/apk/debug/app-debug.apk`
**Local APK SHA-256:** `8444541d9934c2515112f9d9d7c268056cb1dc715ad5fe7d066c50f8e8f87182`

## What changed

The previous pipeline sampled a single median border colour and reduced a full-resolution cutout directly to 96 × 96. On photos with grass, leaves, stems and more than one background tone, that left small grey/green islands around a flower. The replacement keeps the pipeline deterministic and bounded, but improves both the cutout and sprite design.

1. **Bounded decode.** Read image dimensions first, reject corrupt or excessively large sources, and decode a downsampled bitmap (maximum source edge is bounded near 960 px before the processing stages).
2. **Multi-colour edge model.** Build a deterministic palette of up to 48 coarse RGB colours sampled along the entire border, retaining repeated colours and a median-colour fallback. This handles textured or gently varying backgrounds better than one global colour.
3. **Anchored background flood fill.** Only start from the image edge. Pixels close to a sampled background colour are removed; smooth colour drift can propagate only while remaining anchored near the border palette and near the previously removed neighbour. The engine rejects no-op and near-empty masks rather than emitting a bad cutout.
4. **Source-space speckle cleanup.** Remove small disconnected foreground islands (including islands touching the border) before finding the crop. Larger connected regions, including connected foliage, remain.
5. **Actual pixel-art grid.** Fit the isolated flower into a transparent 48 × 48 work grid, preserving aspect ratio and using nearest-neighbour sampling. This creates intentionally chunky pixels rather than a blurry photo at a smaller resolution.
6. **Compact deterministic palette.** Quantize to coarse channel values, use a median-cut palette capped at 16 base colours, and map pixels back to the nearest palette colour.
7. **Local grid cleanup.** Remove detached one- and two-pixel components from the 48 × 48 grid while protecting the central region where tiny stamens/pollen may be meaningful.
8. **Subtle edge lighting.** Use restrained directional highlight/shadow values at the alpha boundary, then quantize shades to the pixel grid. The edge treatment is intentionally mild so it adds volume without making the result look airbrushed.
9. **Output.** Nearest-neighbour upscale to a 96 × 96 RGBA PNG. The source photograph, attribution/license fields and source/output SHA-256 metadata remain separate from the derivative.

## Cache/version correctness

The processor version is part of cache validity, not just a metadata label:

- `FlowerSpriteWorker` validates cached output and negative-result markers against the current processor version.
- `FlowerSpriteScheduler` now applies the same version check before deciding a sprite is already valid or a negative result is still fresh. This fixes the discovered bypass where an older, checksum-valid sprite prevented any regeneration from being scheduled.
- `FlowerSpriteImageOrPlaceholder` will not display an old-version sprite as if it were current. It continues to use the placeholder while the new job runs, then observes the published replacement.
- On a pipeline bump, v2 sprite metadata and v2 failure markers no longer block a v3 retry.

The UI does not falsely mark an image as “real” merely because a PNG exists: the flower identity, pipeline version and file checksum must match.

## Regression coverage

`FlowerPixelArtProcessorInstrumentedTest` covers:

- `fullFrameCropAndIdentityScaleProduceTransparentPixelArtWithoutRecyclingAliases`: full-frame crop / identity-size bitmap ownership, alpha, output dimensions and visible silhouette.
- `texturedBorderBackgroundIsRemovedAndShadedSpriteKeepsACompactPalette`: deterministic textured-background input, transparent exterior, visible flower/leaves, compact palette, and at least two visibly distinct tones in an otherwise flat-colour petal region to prove edge shading is applied.
- `schedulerRegeneratesAValidSpriteWhenThePipelineVersionChanges`: a checksum-valid v2 sprite plus a still-fresh v2 negative marker must schedule work under v3.
- `persistedProcessedSpriteLoadsInsideTheRealGardenFlowerCard`: the saved PNG and versioned metadata display through the actual Garden collection card.

## Validation record

### Local build

The clean/recent local Gradle validation passed:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:compileDebugAndroidTestKotlin --no-daemon
```

Result: **BUILD SUCCESSFUL**. Unit tests, lint, application APK assembly, Android-test APK assembly and Android-test Kotlin compilation passed. Lint has no errors; a pre-existing Java `FileObserver` deprecation warning remains.

### BlueStacks visual validation

The new local APK was installed through the BlueStacks APK picker after ADB installation failed with `connection closed` / `file_sync_client protocol fault`. The existing profile/garden was retained, and opening the collection caused the Camellia to be regenerated; its displayed sprite SHA prefix changed from the previous build's value, showing the old cached output was replaced.

Observed in the collection screenshot and enlarged sprite capture:

- the white Camellia petals and yellow centre remain identifiable;
- the small detached flecks outside the flower visible in the earlier zoom are no longer visible;
- the pixel edges remain crisp and the shadowing is subtle;
- the card shows the generated sprite, not the generic pink-flower placeholder; the unknown second flower still correctly uses its placeholder.

Evidence files:

- `docs/evidence/flower-pixel-art-v3-2026-10-21/bluestacks-collection-v3.png`
- `docs/evidence/flower-pixel-art-v3-2026-10-21/camellia-sprite-v3-zoom.png`
- `docs/evidence/flower-pixel-art-v3-2026-10-21/bluestacks-camellia-provenance-v3.png`

### Remaining validation boundary

BlueStacks visual smoke is complete, and the new app rendered the refreshed sprite. The Android instrumentation APK was built, but executing `am instrument` on this workstation was blocked by the BlueStacks ADB transport repeatedly closing shell/file-sync connections. Do not describe the instrumented tests as executed on BlueStacks until the CI emulator run confirms them. The repository workflow is configured to run `connectedDebugAndroidTest` on API 28 after the change is pushed; record its result and exact source SHA in `CURRENT_STATE.md`.

The processor is still a deterministic colour-based cutout, not an ML segmentation model. Photos where the subject and background share nearly identical colours can still require a placeholder. This is intentional: a visible, documented fallback is preferable to confidently storing a damaged sprite.
