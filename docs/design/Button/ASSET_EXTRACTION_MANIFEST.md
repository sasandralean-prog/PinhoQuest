# PinhoQuest — Canonical Button Asset Extraction

**Source artwork:** `buttons.png` supplied for the P6 UI refactor.  
**Source dimensions:** 1417×1890 RGBA.  
**Extraction rule:** each authored control/card is isolated from the transparent source without repainting, text substitution, or interaction semantics. Transparent outer margins are trimmed only.

## Canonical extracted elements

| File | Role |
|---|---|
| `BtnStart.png` | Onboarding/start action — **Começar** |
| `BtnSeeQuests.png` | Empty-garden action — **Ver quests** |
| `BtnBack.png` | Shared back control |
| `CardFlowerUnknownA.png` | Unknown flower/card placeholder variant A |
| `CardFlowerUnknownB.png` | Unknown flower/card placeholder variant B |
| `BtnSortQuest.png` | Primary Home action — **SORTEAR QUEST** |
| `BtnQuestRandom.png` | Quest action — **Quest Aleatória** |
| `BtnQuestGame.png` | Quest action — **Quest de Jogo** |
| `BtnTagMusica.png` | Theme — Música |
| `BtnTagFotografia.png` | Theme — Fotografia |
| `BtnTagNatureza.png` | Theme — Natureza |
| `BtnTagTecnologia.png` | Theme — Tecnologia |
| `BtnTagAnimais.png` | Theme — Animais |
| `BtnTagAventuras.png` | Theme — Aventuras |
| `BtnTagRelaxar.png` | Theme — Relaxar |
| `BtnTagCriar.png` | Theme — Criar |
| `BtnTagFantasia.png` | Theme — Fantasia |

## Implementation boundary

These files are **skins/assets only**. Compose owns click handling, enabled/disabled state, selected state, accessibility semantics and navigation.

Selected theme/tag controls use the shared yellow glow. The same extracted assets are valid in day and night unless a future authored variant is explicitly registered.

The two unknown-flower variants are visual placeholders only; flower identity remains domain-owned and must never be inferred from the bitmap.
