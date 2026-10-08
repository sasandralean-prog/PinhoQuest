# Pinho Quest — Canonical Graphics Registry

**Status:** canonical visual reference registry  
**Scope:** P6–P7 visual implementation  
**Authority:** this registry records which repository assets are authoritative references and what each asset is allowed to mean.

## 1. Canonical rule

The assets in this registry are **visual references and authored graphic elements**. They are not permission to turn screenshots into static UI.

Implementation must preserve:
- real Compose interaction and state;
- governed domain data;
- responsive layout;
- accessibility and font scaling;
- the single navigation authority.

Raster artwork may provide scenery, texture, decorative chrome, or the visual skin of a real component. It must not become a hidden hitbox, state store, navigation owner, or replacement for an interactive component.

## 2. Canonical authority chain

For visual decisions, use:

1. `docs/identity/PINHO_QUEST_VISUAL_IDENTITY_GENOME.md`
2. `docs/identity/PINHO_QUEST_GARDEN_PIXEL_ART_GENOME.md`
3. `docs/design/UI_DESIGN_CONTRACT.md`
4. **this registry**
5. the individual references listed below.

When a reference conflicts with a higher-level identity rule, the identity rule wins.

## 3. Home1 is the source of truth

**Home1 is canonical. It is refined, not redesigned.**

The Home/Quest composition must preserve:
- the PinhoQuest world and lakeside/garden atmosphere;
- the established logo and visual identity;
- the authored card/composition language;
- exactly three simultaneous quest actions:
  - **SORTEAR QUEST**
  - **Quest de Jogo**
  - **Quest Aleatória**
- the continuous bottom navigation:
  - **Início**
  - **Jardim**
  - **Perfil**

No fourth navigation destination is introduced by visual references. Settings remains an application/settings destination, not a fourth item in the canonical bottom navigation.

Current Home/Quest reference:
- `docs/design/Screen/1791406007965.jpg`

## 4. Screen references

### Settings

| Reference | Authority | Meaning |
|---|---|---|
| `docs/design/Screen/1791368109565.jpg` | canonical | Settings — dark |
| `docs/design/Screen/1791368270990.jpg` | canonical | Settings — light |

These establish the authored wood title bar, Day/Night controls, font-size controls, backup action, donation action, blurred/cozy room background, and persistent bottom navigation.

### Onboarding / name + tags

| Reference | Authority | Meaning |
|---|---|---|
| `docs/design/Screen/1791405606818.jpg` | canonical | Name/tags — light |
| `docs/design/Screen/1791405677300.jpg` | canonical | Name/tags — dark |
| `docs/design/Screen/1791405470928.jpg` | canonical | Tags-selection continuation |

The name field remains a real editable field. The artwork around it is visual chrome, not an input hitbox.

### Garden

| Reference | Authority | Meaning |
|---|---|---|
| `docs/design/Screen/1791406141636.jpg` | canonical | Empty Garden — dark |
| `docs/design/Screen/1791406257190.jpg` | canonical | Empty Garden — light |
| `docs/design/Screen/1791406339880.jpg` | duplicate/secondary | Same empty-garden composition; do not create a second semantic design from it |

The Garden remains a place, not a database table. The empty state communicates that the garden has room to grow and points toward quests.

### Reference board

`docs/design/Screen/1791405120200.jpg` is a **reference board/contact sheet**. It may be used to understand the family resemblance and progression of screens, but it is not a screen to embed or implement as a whole.

## 5. Background references

The current authored background set is under `docs/design/BackGround/`.

| Reference | Canonical role |
|---|---|
| `1791363403488.jpg` | PinhoQuest opening/hero garden-lake scene |
| `1791363635961.jpg` | Garden scene — dark visual treatment |
| `1791363831082.jpg` | PinhoQuest lake/mountain atmospheric scene |
| `1791363980201.jpg` | Cozy indoor garden/workroom atmosphere |
| `FunPic_20261007_053152346.jpg` | Garden scene — light visual treatment |

`Home (1).png` is currently an empty/placeholder repository object and is **not** a canonical visual reference.

## 6. Button and component artwork

The current authored button batch is stored at:

`docs/design/Button/`

The path was normalized from the historical `docs/design /Button/` path because the trailing space was not portable to Windows. The canonical assets were preserved byte-for-byte during that normalization.

> The directory name contains a trailing space. This is the repository path as currently published. Windows worktrees cannot materialize such a path reliably; this registry does not treat that filesystem limitation as permission to duplicate or redesign the artwork. A future path normalization must preserve the same assets and semantics.

### Primary green actions

- `BtnStart.png` — **Começar** / onboarding start
- `BtnSortQuest.png` — **SORTEAR QUEST**
- `BtnSeeQuests.png` — **Ver quests** / empty-garden action

### Shared navigation/system

- `BtnBack.png` — shared **Voltar** control

### Quest/action cards

- `BtnQuestGame.png` — **Quest de Jogo**
- `BtnQuestRandom.png` — **Quest Aleatória**

### Flower placeholders

- `CardFlowerUnknownA.png` — unknown flower placeholder/card variant A
- `CardFlowerUnknownB.png` — unknown flower placeholder/card variant B

These are state placeholders only. Flower identity remains domain-owned.

### Theme/category artwork

The authored batch contains one real interactive skin per canonical theme:

- `BtnTagMusica.png` — Música
- `BtnTagFotografia.png` — Fotografia
- `BtnTagNatureza.png` — Natureza
- `BtnTagTecnologia.png` — Tecnologia
- `BtnTagAnimais.png` — Animais
- `BtnTagAventuras.png` — Aventuras
- `BtnTagRelaxar.png` — Relaxar
- `BtnTagCriar.png` — Criar
- `BtnTagFantasia.png` — Fantasia

The extraction manifest is authoritative for the source/crop relationship:
`docs/design/Button/ASSET_EXTRACTION_MANIFEST.md`.

### Existing system artwork

The previously canonized system assets remain valid when present:

- `BtnConfirm.png` — Confirmar
- `BtnContinue.png` — Continuar
- `BtnBackup.png` — backup/cópia do jardim
- `BtnDay.png` — Dia
- `BtnNight.png` — Noite
- `BtnMaior.png` — Maior
- `BtnMedio.png` — Médio
- `BtnMenor.png` — Menor
- `BtnPinhoDonate.png` — Apoie o criador Pinho Abacaxi
- `TopBarConfig.png` — Configurações title bar

### Name-field artwork

- `LabelName.png` — canonical visual chrome for the name field.

The editable text value itself must remain real UI state.

### Decorative/instruction artwork

- `FunPic_20261007_055241466.png` — authored free-form preference/instruction panel reference.

The text inside this asset is presentation artwork only when used as a static decorative panel. Any user-editable or dynamic copy must remain real text.

## 7. Non-canonical captures

The following files currently present under `docs/design/Screen/` are **not** visual authority for Pinho Quest UI:

- `IMG-20261007-WA0029.jpg`
- `IMG-20261007-WA0030.jpg`

They are unrelated external shopping/product captures and must not be used as game UI references.

Likewise, files whose semantic role has not been identified are not automatically canonical merely because they live under `docs/design/`.

## 8. Implementation invariants

1. Never place a full-screen reference screenshot over a live UI.
2. Never place a transparent hotspot over a real input/button and allow it to intercept touch.
3. Button artwork may skin a real button; it does not own the click.
4. Name-field artwork may frame a real text field; it does not replace the field.
5. Screen references define composition, not domain state.
6. Home1 remains the canonical source of truth; later references refine it rather than redesigning its mechanics/navigation.
7. Shared bottom navigation remains one global component with `Início | Jardim | Perfil`.
8. Light/dark references define visual treatments, while Settings remains the authority for the selected theme.
9. Typography that must react to user settings remains real text.
10. Any new asset must be explicitly classified as canonical, secondary, decorative, or non-canonical before becoming an implementation reference.

## 9. Canonization gate

A visual reference is considered **canonized** only when:
- its repository path is recorded here;
- its semantic role is identified;
- its authority level is explicit;
- its interactive/static boundary is explicit;
- implementation rules do not contradict the identity genomes.

This registry is the index used by P6 visual implementation and future visual audits.
