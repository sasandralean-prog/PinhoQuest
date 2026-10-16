# Flower source asset ledger

This directory tracks the exact source bytes and usage rights for photos used to build PinhoQuest flower sprites.

## Required admission fields

Every photo must have a record under `records/<source-sha256>.json` with the exact SHA-256 of the downloaded original, provider page URL, original file URL/name, creator, credit line, license name/identifier/URI, retrieved timestamp, botanical taxon evidence, transformation pipeline version, hashes of derived intermediates/output, and explicit release eligibility.

The SHA-256 is a byte-integrity identifier, not proof of ownership or license. Always review the provider's metadata and license text.

## Files

- `registry.json`: compact index of admitted candidates and their review states.
- `originals/`: immutable original bytes only where redistribution is permitted. Use the SHA-256 as the basename.
- `records/`: per-image provenance/rights sidecars.
- `licenses/`: snapshot copies or stable references to applicable license text.
- `normalized/` and `sprites/`: derived outputs keyed to source hash and pipeline version.
- `attribution-report.md`: generated human-readable credits for distribution.

If the image may not legally be redistributed, do not commit its original bytes. Keep its hash + provenance and an appropriately private source copy, set the original path to null, and block any derived artifact whose license does not allow the intended distribution.

One Wikimedia flower-photo candidate is recorded under `records/`, with the creator and declared CC BY-SA 4.0 license preserved. It is **not admitted for sprite generation or public release yet**: its original bytes have not been fetched into this workspace, so the project-computed SHA-256 is missing and taxonomic identity is not confirmed. Do not infer permission from the fact that an image appears on Wikipedia. Complete the exact-byte hash, local source archive, taxon verification and transformation/license review before admission.
