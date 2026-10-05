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