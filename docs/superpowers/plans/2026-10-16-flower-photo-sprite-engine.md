# PinhoQuest Flower Photo → Sprite Engine (draft logical design)

Date: 2026-10-16
Status: design draft; photo acquisition/processing implementation is not yet enabled.
Owner: Rafa

## Goals

Turn a verified photograph of a real flower into a small, transparent pixel-art sprite without losing botanical identity or source attribution. The pipeline must be reproducible, license-aware, auditable and cheap enough to generate assets off-device or as a controlled background job. The game must never need to regenerate sprites during ordinary collection browsing.

## Authority boundaries

- Wikimedia Commons/Wikipedia is a source-discovery provider, not the authority for botanical truth or redistribution rights.
- Each source photo is an immutable provenance record.
- SHA-256 identifies exact source bytes; it does not prove ownership, copyright status or license.
- A sprite is a derived artifact. It must point to the exact source-photo SHA-256 and the transformation pipeline version.
- Room remains the live domain state for a player's flower catalog. Checked-in sprite files and the source ledger are immutable art/catalog assets, not a second live database.
- No asset is eligible for public APK distribution until its rights metadata is complete and its license permits the intended uses.

## Pipeline

1. **Discover** — query Wikimedia Commons API using botanical name and synonyms; request original file metadata, file page URI, author/credit, license short name and license URI, original dimensions and MIME type.
2. **Validate identity** — bind the photograph to a known flower taxon/accepted name through a separate botanical fact source. A filename alone must not establish species identity. If identity is uncertain, retain as a candidate only.
3. **License gate** — accept public-domain, CC0 or compatible Creative Commons terms only after interpreting attribution, share-alike, non-commercial and no-derivatives constraints against PinhoQuest distribution. Reject unknown/missing license, no-derivatives for a transformation pipeline, incompatible non-commercial use if monetization/support changes the use, and any terms that cannot be recorded. Never equate “available on Wikipedia” with “free to redistribute”.
4. **Download original** — fetch exact original bytes; do not recompress or strip metadata before hashing. Calculate SHA-256 over the byte-for-byte original. Store the original, where redistribution is permitted, under `docs/design/flora_sources/originals/<sha256>.<ext>`. Do not rename the hash-addressed object after admission.
5. **Create sidecar** — write `docs/design/flora_sources/records/<sha256>.json` and add the record to `registry.json`. The record stores original hash, original filename, source/file-page URLs, author, license, credit line, download timestamp, retrieval tool/API version, species/taxon linkage, rights review status and local file path. The sidecar should be committed alongside the original or withheld asset record.
6. **Normalize** — EXIF-orient, decode with size/pixel limits, crop to a padded square canvas, remove the background while retaining fine petal/leaf boundaries, and emit RGBA. Preserve source untouched.
7. **Pixelize deterministically** — scale to a canonical working canvas, apply alpha cleanup, resize down with a named algorithm, quantize into a bounded palette, use nearest-neighbour upscaling only if needed, and constrain final sprite dimensions. Keep a toolchain + version manifest so a generated result can be reproduced.
8. **Review** — verify silhouette and recognizable petals, no halo/fringing, alpha channel intact, color-count budget, no unexpected text/watermarks, matching botanical identity and license compatibility. Reject rather than auto-promote uncertain outputs.
9. **Hash derived output** — calculate independent SHA-256 for the transparent original-normalized intermediate and final sprite. The final sprite filename is content-addressed or mapped deterministically from `sourceSha256 + pipelineVersion + variant`.
10. **Catalog registration** — map the species/flower ID to the approved sprite path; retain the source record and credit attribution. The emoji placeholder is replaced only after the new sprite passes review; keep a deterministic placeholder fallback.
11. **Cache/invalidation** — key cache by source SHA-256, pipeline version, output target size and transformation options. A source or pipeline change creates a new artifact; do not silently overwrite provenance.
12. **Release audit** — generate a human-readable attribution report from the registry and verify every redistributable sprite resolves to an admitted source record.

## Proposed source record (JSON)

```json
{
  "recordVersion": 1,
  "sourceSha256": "<64 lowercase hex chars over exact downloaded bytes>",
  "sourceKind": "wikimedia-commons",
  "sourcePageUrl": "https://commons.wikimedia.org/wiki/File:...",
  "originalFileUrl": "https://upload.wikimedia.org/...",
  "originalFileName": "original-name.ext",
  "originalRelativePath": "docs/design/flora_sources/originals/<sha256>.<ext>",
  "retrievedAtUtc": "2026-10-16T00:00:00Z",
  "providerApi": "Wikimedia Commons API",
  "providerMetadataRevision": "<revision id or timestamp>",
  "creator": "<creator/photographer exactly as source credits>",
  "creditLine": "<required attribution line>",
  "license": {
    "identifier": "CC-BY-SA-4.0",
    "name": "Creative Commons Attribution-ShareAlike 4.0 International",
    "url": "https://creativecommons.org/licenses/by-sa/4.0/",
    "sourceLicenseTextUrl": "<license/permission page>",
    "reviewStatus": "PENDING",
    "reviewedBy": null,
    "reviewedAtUtc": null,
    "conditions": ["ATTRIBUTION", "SHARE_ALIKE"]
  },
  "botanical": {
    "acceptedScientificName": "<taxon name>",
    "commonNamePtBr": "<common name>",
    "taxonSourceUrl": "<authoritative botanical source>",
    "identityStatus": "CANDIDATE"
  },
  "transform": {
    "pipelineVersion": "flower-sprite-v0",
    "backgroundRemovalModel": "<name/version/hash>",
    "pixelizerVersion": "<tool/version>",
    "canonicalDimensions": [96, 96],
    "paletteLimit": 24,
    "transparentBackground": true
  },
  "derived": {
    "normalizedPngSha256": null,
    "spritePngSha256": null,
    "spriteRelativePath": null,
    "reviewStatus": "NOT_GENERATED"
  },
  "distribution": {
    "redistributableOriginal": false,
    "redistributableDerivedSprite": false,
    "attributionRequired": true,
    "releaseAllowed": false,
    "releaseBlockers": ["LICENSE_NOT_REVIEWED"]
  }
}
```

The example is a schema example, not a real license record. Replace every placeholder from retrieved metadata. Never copy example values into an admitted source record.

## Directory layout

```text
docs/design/flora_sources/
├── README.md
├── registry.json
├── originals/
│   └── <source-sha256>.<original-extension>
├── records/
│   └── <source-sha256>.json
├── licenses/
│   └── <license-id>.txt
├── normalized/
│   └── <source-sha256>-<pipeline-version>.png
├── sprites/
│   └── <source-sha256>-<pipeline-version>-96.png
└── attribution-report.md
```

Keep binary originals out of the public repository if the license does not permit redistribution. In that case preserve the exact SHA-256, URI, author/license metadata and a private/off-repo original copy; mark `originalRelativePath` null and `releaseAllowed=false`. Public distribution must not be blocked by a local-path assumption.

## Initial invariants

- Original hashes are computed before any EXIF orientation, crop or decode/re-encode.
- The same original bytes always produce the same source SHA-256, regardless of filename.
- SHA-256 integrity does not replace license or ownership review.
- Sprite hash is separate from original hash and is never described as a copyright claim.
- Share-alike licenses must be evaluated for the derived output and its distribution terms.
- Source and attribution metadata must be included in internal project records and a generated public attribution notice when required.
- Never ship an original or derived asset with unknown or incompatible rights.
- A runtime/photo download failure leaves the previous sprite or placeholder untouched.
- New source/photo transformations do not mutate flower identity, rarity, population facts, ownership or XP.
