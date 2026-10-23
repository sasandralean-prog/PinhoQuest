# Runtime flower-photo discovery

PinhoQuest discovers flower photographs at runtime and stores accepted originals only in the app's private files directory. The app does not bundle dynamically fetched photos into the APK or treat a provider search result as proof of a license.

## Discovery sources

1. **Wikimedia Commons** — the Action API searches both the exact scientific-name phrase and the unquoted binomial. Candidate metadata must still include the genus, a supported image format, attribution details where required, and an explicit derivative-compatible license.
2. **iNaturalist** — the public observations API is queried by scientific taxon and research-grade observations. The runtime filter only accepts images hosted by the documented open-photo bucket and rejects missing, non-commercial, no-derivatives, and otherwise unsupported photo licenses.
3. **GBIF occurrence media** — the occurrence API is queried by scientific name and still-image media. The provider requires the media object's own explicit license and creator metadata; it deliberately does not assume that an occurrence or dataset license automatically covers its images. Some GBIF records aggregate community-science datasets, including Pl@ntNet observations when those records are present in GBIF. This is not a direct Pl@ntNet API integration.

## Rights gate

The runtime gate permits CC0/public-domain candidates and recognized CC BY / CC BY-SA licenses with a known version. It rejects CC BY-NC, CC BY-ND, unknown/missing licenses, and attribution-required candidates that provide no usable creator or credit line. Accepted records preserve source provider, taxon name, source page URL, original image URL, creator/credit, license name/URL, and source SHA-256.

A CC BY-SA source requires the derivative sprite to remain under the compatible ShareAlike terms when redistributed. A locally generated sprite is not proof of rights; source metadata remains part of its provenance.

## Failure behavior

The search reports provider query errors, empty searches, license/attribution rejection, download errors, and segmentation rejection separately. A temporary provider failure is retried; short negative caches are versioned so a previous no-result result does not suppress the expanded provider search. Several licensed candidates can be processed in order, so an unusable first photo does not block later candidates.

All source API calls are rate-limited by timeouts and use HTTPS. Photo downloads are capped at 6 MiB and SHA-256 hashed before the original is stored privately.
