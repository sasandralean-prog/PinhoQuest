# P6-A — Canonical design asset catalog

**Authority:** the editable source assets in `docs/design/BackGround/` and `docs/design/Button/`.
**Composition references:** `docs/design/Screen/`. These JPGs describe the intended layout; they are not full-screen runtime assets and must not be layered over live Compose controls.
**Scope:** asset naming, Android resource availability, and source-of-truth documentation only. Screen composition and behavior changes belong to P6-B onward.

## Rules

- Do not rename or rewrite the original PNG/JPG files to satisfy Android resource naming rules.
- `app/build.gradle.kts` maps the original filenames to stable lowercase Android resource aliases and copies them to a generated resource directory.
- `verifyCanonicalUiAssets` is a `preBuild` dependency. It fails if any mapped source asset or generated output is missing, or if two assets claim the same resource name.
- Files called `null`/ `Null 1` are not design assets and are intentionally excluded.
- The timestamp-named JPGs under `BackGround/` are preserved as unclassified source/reference images; they are not yet wired as runtime backgrounds because their intended day/night/screen role is ambiguous.
- Legacy resources under `app/src/main/res/drawable-nodpi/` are intentionally retained until P6-B migrates their consumers and regression tests prove they are unused.

## Background source-to-resource map

| Source in `docs/design/BackGround/` | Generated resource |
|---|---|
| `ConfigBGDIA.png` | `canonical_bg_settings_day` |
| `JArdimVazioBG.png` | `canonical_bg_garden_empty_alt` |
| `JardimArteDia.png` | `canonical_bg_garden_art_day` |
| `JardimArteNoite.png` | `canonical_bg_garden_art_night` |
| `JardimVazioDiaBG.png` | `canonical_bg_garden_empty_day` |
| `PerfilBGDIA.png` | `canonical_bg_profile_day` |
| `PerfilBGNoite.png` | `canonical_bg_profile_night` |
| `StartBG.png` | `canonical_bg_start` |
| `StartBGDIa.png` | `canonical_bg_start_day` |

## Button/card source-to-resource map

| Source in `docs/design/Button/` | Generated resource |
|---|---|
| `BackButton.png` | `btn_back` |
| `BackupButton.png` | `canonical_backup_button` |
| `ButtonTopBarConfig.png` | `canonical_topbar_config` |
| `DOnateButton.png` | `canonical_donate_button` |
| `DayButton.png` | `canonical_day_button` |
| `FlowerCardDay.png` | `canonical_flower_card_day` |
| `FlowerCardNight.png` | `canonical_flower_card_night` |
| `FlowerColectionButton.png` | `canonical_flower_collection_button` |
| `FlowerFilter.png` | `canonical_flower_filter` |
| `FlowerFilterSearched.png` | `canonical_flower_filter_searched` |
| `FlowerFilterUnkw.png` | `canonical_flower_filter_unknown` |
| `FlowersFilterAll.png` | `canonical_flower_filter_all` |
| `GameQuestButton.png` | `btn_quest_game` |
| `JardimDeDay.png` | `canonical_garden_day_element` |
| `JardimDeNight.png` | `canonical_garden_night_element` |
| `JardimVAzioNight.png` | `canonical_garden_empty_night_element` |
| `JardimVazioDIa.png` | `canonical_garden_empty_day_element` |
| `MaiorButton.png` | `canonical_font_larger_button` |
| `MedioButton.png` | `canonical_font_medium_button` |
| `MenorButton.png` | `canonical_font_smaller_button` |
| `NameBar.png` | `canonical_name_bar` |
| `NavBarDay.png` | `canonical_nav_bar_day` |
| `NavBarNight.png` | `canonical_nav_bar_night` |
| `NightButton.png` | `canonical_night_button` |
| `RandomQuestButton.png` | `btn_quest_random` |
| `SortQuest.png` | `btn_sort_quest` |
| `StartButton.png` | `btn_start` |
| `TagAnimals.png` | `btn_tag_animais` |
| `TagCreate.png` | `btn_tag_criar` |
| `TagFantasy.png` | `btn_tag_fantasia` |
| `TagLearn.png` | `btn_tag_learn` |
| `TagMusic.png` | `btn_tag_musica` |
| `TagNature.png` | `btn_tag_natureza` |
| `TagPhoto.png` | `btn_tag_fotografia` |
| `TagRelax.png` | `btn_tag_relaxar` |
| `TagTech.png` | `btn_tag_tecnologia` |
| `VerQuestButton.png` | `canonical_see_quests_button` |

## Semantic tag mapping

The nine canonical tag graphics map to the existing system tags: Animals → `animals`; Create → `create`; Fantasy → `fantasy`; Learn → `learning`; Music → `music`; Nature → `nature`; Photo → `photography`; Relax → `relax`; Tech → `technology`.

The canonical `TagLearn.png` asset is **not** the Adventures tag. The visual catalog therefore maps it to the system tag `learning`; the old visual mapping of Adventures to a non-existent `TagAdventures.png` was removed. This changes only the displayed visual-tag selection catalog, not the domain tag definitions or quest-generation semantics.

## Canonical screen references

- `1791405120200.jpg`: overall composition reference.
- `1791368109565.jpg` and `1791368270990.jpg`: settings day/night composition references.
- The reference images are not production backgrounds. P6-B must compose live controls and text over the current canonical backgrounds while using these images as layout guidance.

## Deliberately not changed in P6-A

- Which specific screen uses each background.
- Layout, sizing, cropping, overlays, typography, bottom navigation, and back-stack behavior.
- Garden state transitions and quest generation.
- Removal of legacy sprite sheets or old drawable files.
