# Collection I — factual source evidence

**Date reviewed:** 2026-10-01  
**Catalog:** `collection-i-primeiras-descobertas`, version 1  
**Rarity scale:** version 1, absolute mature-individual estimate, log10 reference interval 1–10,000.

## Evidence rule

A flower is classified only when the source provides a defensible point estimate/observed global count that can be represented as mature individuals. Ranges, upper/lower bounds, locality observations that do not establish a global point estimate, and ambiguous `individuals/clumps` remain `UNKNOWN`.

`Comum / Incomum / Rara / Raríssima` are the four equal bands of the versioned logarithmic scale. They are **not IUCN conservation categories** and do not influence drop probability.

## Classified entries

| Species | V1 estimate | Source evidence |
| --- | ---: | --- |
| `Camellia granthamiana` | 3,000 mature individuals | IUCN global assessment: https://doi.org/10.2305/IUCN.UK.2015-4.RLTS.T62053240A62053244.en |
| `Sambucus palmensis` | 340 mature individuals | IUCN global assessment (2026): https://staging-www.iucnredlist.org/es/species/61596/245042349/pdf |
| `Caulokaempferia arunachalensis` | ~160 mature individuals | Kew POWO conservation account: https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A77297123-1/general-information |
| `Vesalea coriacea` | ~120 mature individuals | Kew POWO population survey account: https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A77105363-1/general-information |
| `Dypsis dracaenoides` | ~100 mature individuals | Kew POWO conservation account: https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A77105549-1/general-information |
| `Ravenea beentjei` | ~40 mature individuals | Kew POWO conservation account: https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A77105557-1/general-information |
| `Dypsis gronophyllum` | ~40 mature individuals | Kew POWO conservation account: https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A77105551-1/general-information |
| `Ceropegia graminea` | ~30 mature individuals | Kew POWO conservation account: https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A77205195-1/general-information |
| `Phanera laotica` | ~30 mature plants | Kew POWO conservation account: https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A77218370-1/general-information |
| `Rorippa valdes-bermejoi` | 13 mature individuals/stands | IUCN global assessment (2025): https://staging-www.iucnredlist.org/fr/species/175300/117849461/pdf |
| `Diospyros atrata` | 6 mature individuals located by survey | BGCI project survey: https://www.bgci.org/our-work/projects-and-case-studies/genetic-diversity-and-seed-germination-studies-on-endangered-diospyros-species-sri-lanka/ |

## Deliberately UNKNOWN examples

- `Didymocarpus phuquocensis`: the publication reports about 2,000 **mature individuals/mature clumps**. Because the counting unit is explicitly ambiguous, V1 retains the factual source but does not classify rarity: https://pmc.ncbi.nlm.nih.gov/articles/PMC7486308/
- `Keetia susu`: sources report observations of about 20 mature individuals, but this is not treated as a closed global population point estimate. It remains `UNKNOWN`: https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A60477041-2/general-information
- `Dinizia jueirana-facao`: IUCN gives a global **range of 22–24 mature individuals**, not a single point estimate. V1 stores the range and leaves rarity `UNKNOWN`: https://doi.org/10.2305/IUCN.UK.2019-2.RLTS.T118938381A118938383.en
- Other entries with only lower/upper bounds likewise remain `UNKNOWN`.

## Loader/authority note

The approved plan named `app/src/main/assets/...`, but the implemented authority is `android-data`: embedded catalogs/goals live under `android-data/src/main/resources/` and are loaded/validated there. This keeps factual seed data next to its persistence adapter and allows JVM loader tests without giving the Compose app ownership of catalog truth.

Every future dynamic `CatalogPack` must pass the same `CatalogPackValidator` before persistence.
