# Flower Pixel-Art Pipeline v4

**Date:** 2026-10-22
**Branch:** `feature/p6-garden-delight`
**Pipeline ID:** `flower-pixel-v4`

## Motivation

The v3 remover learned the background palette from the entire perimeter and seeded its flood fill all around the image. In real botanical photos, leaves and stems often touch the sides. That means real plant colours can be mistaken for background and disappear with the cutout. This update changes the mask's starting assumptions and adds a reproducible, real-photo regression test.

## Processing changes

1. **Corner-seeded background model.** Learn repeated colour samples from four corner patches (each about one ninth of the decoded width and height), not every pixel along the full perimeter. Seed flood fill from those corner patches rather than all four edges. This reduces the chance that side-touching foliage is automatically treated as background.
2. **Locally anchored colour flood fill.** Preserve the previous bounded, deterministic colour-distance and smooth-gradient rules. Only pixels connected to a corner seed and sufficiently close to the sampled background model are removed.
3. **Plant cutout and cleanup.** Keep non-background components, including leaves, stems and other foliage connected to the flower; remove small disconnected islands and crop to the surviving foreground.
4. **Pixel-art rendering.** Preserve the existing transparent 48 × 48 working grid, compact deterministic colour palette, nearest-neighbour 96 × 96 output and restrained directional shading.
5. **Cache correctness.** Bump the pipeline identifier to `flower-pixel-v4`. Existing v3 sprites and rejection markers are now stale and cannot be shown as v4. Successful v4 work must create matching metadata and checksums before the garden card displays the sprite.

## Visual and regression testing

The Android instrumentation fixture uses an unmodified photo of *Camellia granthamiana* in a leafy setting from Wikimedia Commons. See `app/src/androidTest/assets/flower_fixtures/README.md` for its file page, photographer and CC BY-SA 2.0 license. The test exercises the actual Android processor instead of a separate desktop approximation.

The test asserts a decodable 96 × 96 transparent output, a minimum visible silhouette and visible green pixels from leaves/stem. It writes the original, output sprite and a side-by-side original-versus-pixel-art comparison; it also captures a screenshot of the real garden collection card displaying the sprite. The CI workflow uploads these image files in the `pinhoquest-flower-visual-evidence` artifact for visual inspection. They are created outside the application APK.

The collection-card test uses a valid v4 metadata record and checksum, so it tests the same verification path that prevents stale or corrupt sprites from masquerading as successful generations.

## Limits and placeholders

This remains deterministic colour segmentation, not semantic AI segmentation. A leaf whose colour and texture are indistinguishable from its background cannot always be separated cleanly; busy foliage can also be retained as unwanted foreground. The comparison screenshot is therefore a required visual review, not just a passing compile. When Wikimedia Commons has no suitable derivative-permitting, correctly attributed photo, or the processor rejects a poor cutout, the app can still display its intended placeholder. Such a placeholder must not be replaced with an unrelated plant photo.

The same regression screenshot includes the collected `Didymocarpus phuquocensis` entry, which currently displays the tulip placeholder because the production provider's exact-phrase Wikimedia Commons search returned no file candidate in a check on 2026-10-22. This result diagnoses the current provider/search-coverage path; it does **not** prove no usable image exists elsewhere or under a synonym. The placeholder is deliberately visible alongside the successful flower rather than silently replaced with another species.

## Validation record

Validation status for this version is tracked by the branch's Android CI run and the uploaded visual-evidence artifact. Do not mark the v4 visual gate complete until the real-photo instrumentation test, API 28 emulator run and original-versus-output screenshot have all been inspected.
