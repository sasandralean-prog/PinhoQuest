# PinhoQuest Garden Pixel-Art Genome

## Purpose

Define the visual contract for flower/garden artwork used by the Garden.

## Canonical pipeline

Flower research
→ licensed source image
→ provenance/license validation
→ normalization/crop
→ deterministic pixel renderer
→ compact FlowerArtwork
→ Garden

## Renderer contract

FlowerArtworkGenerator is the abstraction.

V1 implementation: DeterministicPixelRenderer

Future optional implementation: AiPixelRenderer

The Garden must not depend on renderer implementation.

## Deterministic renderer

Recommended stages:
1. normalize source orientation
2. crop subject/background according to deterministic rules
3. resize to target canvas
4. reduce color palette
5. quantize colors
6. optional restrained dithering
7. pixelize with stable nearest-neighbor rules
8. encode compact PNG/WebP
9. calculate artwork hash

Same source + same renderer version + same configuration must produce the same artwork.

## Target sizes

Prefer 64x64 for ordinary flower assets.
Allow 96x96 where composition needs more detail.

Avoid shipping original high-resolution photographs after transformation unless provenance/debugging explicitly requires them.

## Provenance

Every artwork should retain:
- flowerId
- artworkId
- source URL/reference
- author when available
- license
- attribution requirement
- renderer version
- renderer configuration hash
- artwork SHA-256

The app must not treat a Google Images result as proof of public-domain status.

## Asset packs

Artwork may be acquired as a collection pack when a garden collection is completed or otherwise unlocked.

Temporary source images should be cleaned after successful transformation unless retention is explicitly required.

The portable .pqbackup format may include compact final artworks and their manifests, never model binaries.

## AI renderer boundary

An AI image-to-image renderer is optional and must not be required for normal Garden operation.

If introduced:
- keep it outside the mandatory APK path where practical
- preserve provenance
- make output deterministic only when the chosen service/model/config supports it
- never let AI output replace botanical evidence


## Implementation update: photo-to-pixel-art v3 (historical, 2026-10-21)

The v3 Android implementation was `FlowerPixelArtProcessor`, pipeline ID `flower-pixel-v3`. The current implementation and v4 validation plan are recorded in `docs/design/flower-pixel-art-pipeline-v4.md`; this section preserves the earlier version's decisions.

The v3 deterministic pipeline used a multi-colour border model and anchored flood fill, source-space cleanup of small disconnected foreground islands, a 48 x 48 work grid, a median-cut palette capped at 16 base colours, one/two-pixel grid-speckle removal, light directional edge shading and nearest-neighbour enlargement to a 96 x 96 transparent PNG. The collection card consumes only a verified, versioned asset.

Pipeline version participates in three gates: scheduler cache validity, worker cache validity (including negative-result markers), and on-screen sprite validation. Therefore a checksum-correct older image is no longer sufficient to suppress regeneration under the active pipeline version or appear as a current sprite.

The BlueStacks screenshot evidence is stored under `docs/evidence/flower-pixel-art-v3-2026-10-21/`. The Camellia sprite was re-generated and visually reviewed after installation; detached speckles from the prior screenshot were removed while the flower silhouette and yellow centre stayed visible. The original source attribution and checksum metadata remain part of the detail view.

Validation boundary: local unit tests, lint, APK assembly and Android-test compilation passed. The BlueStacks visual flow passed after installing via its APK picker. Running the instrumented test command directly through this workstation's ADB transport was blocked by repeated `connection closed` / `file_sync_client protocol fault` errors, so device-instrumented tests are not considered executed here. Record the API 28 CI emulator run and exact code SHA in `CURRENT_STATE.md` before closing the automated device-test gate.
