# Pinho Quest — UI / Visual Implementation Contract

**Status:** canonical implementation authority
**Scope:** P4–P7
**Purpose:** reconstruct the canonical Pinho Quest experience with real, responsive UI components while preserving the product’s visual identity and domain boundaries.

## 1. Canonical visual references

The canonical visual references are indexed by `docs/design/CANONICAL_GRAPHICS.md` and the authored assets under `docs/design/Screen/`, `docs/design/BackGround/` and `docs/design/Button/`.

The registry identifies which files are canonical, secondary, decorative, or non-canonical. In particular, **Home1 remains the primary/source-of-truth composition** and is refined rather than redesigned.

They define:
- composition;
- visual hierarchy;
- approximate spacing and proportions;
- visual language;
- palette and material language;
- navigation;
- interaction states;
- component relationships.

They are **reference compositions**, not screenshots to be placed as one static image.

The implementation must reconstruct the interface with real components and governed state. Reference screenshots and full-screen compositions are never the sole rendering mechanism for interactive UI.

## 2. Implementation boundary

### Background
Raster assets may be used when they preserve the intended scenery, atmosphere, texture, or environmental composition.

### UI
These must remain real UI components:
- text;
- buttons;
- cards;
- navigation;
- chips/tags;
- fields;
- selections;
- toggles and controls;
- loading, empty, success, failure and disabled states.

A screenshot must never become the sole rendering mechanism for interactive UI.

### Decoration
Transparent raster assets may be used for decorative elements when this preserves the authored artwork more faithfully.

Decorative assets must not silently own domain state or interaction semantics.

## 3. Responsiveness

The composition must survive different:
- screen sizes;
- aspect ratios;
- density buckets;
- font sizes;
- light/dark appearance where supported.

Absolute positioning is acceptable only for authored decorative composition where it does not compromise usability or responsive structure.

Text must not be baked into raster artwork when it is expected to adapt to user settings or state.

## 4. Interaction consistency

The visual appearance must remain coherent across all relevant states:
- idle;
- pressed;
- selected;
- disabled;
- loading;
- success;
- empty;
- failure/offline.

State changes must be represented by real component state, not by swapping a whole-screen screenshot.

## 5. Visual hierarchy

Each screen must have a clear purpose and a visually identifiable primary action.

Primary, secondary, tertiary and destructive actions must remain distinguishable.

Do not create multiple competing visual primaries merely because several actions are available.

## 6. Shared components

Repeated visual structures must use shared components/tokens rather than screen-specific copies.

The bottom navigation is one global reusable component wherever it is present:
- same destination order;
- same semantics;
- same selected/unselected behavior;
- same touch behavior;
- current destination remains identifiable without color alone.

A visual change to a shared component is a global change, not a local screen tweak.

## 7. Domain and data boundary

The visual layer renders governed state; it does not invent facts.

It must not:
- invent quest/game/flower metadata;
- infer domain identity from rendered text;
- duplicate navigation ownership;
- hardcode transient domain state;
- replace persistence with visual state.

Factual content that requires provenance must come from the appropriate catalog/data authority.

## 8. Accessibility and personalization

The implementation must preserve:
- readable contrast;
- adequate touch targets;
- semantic labels/content descriptions where needed;
- user-selected font size;
- light/dark behavior;
- reduced-motion expectations where applicable.

Large-font layouts are a first-class composition constraint, not a post-hoc patch.

## 9. Canonical authority order

For visual/UX decisions, use this order:

1. `docs/identity/PINHO_QUEST_VISUAL_IDENTITY_GENOME.md`
2. `docs/identity/PINHO_QUEST_GARDEN_PIXEL_ART_GENOME.md`
3. this `docs/design/UI_DESIGN_CONTRACT.md`
4. `docs/design/CANONICAL_GRAPHICS.md`
5. the individual references explicitly classified by that registry

The contract must not contradict the identity genomes. If a visual decision requires changing a canonical identity rule, update the relevant identity genome first, then reconcile this contract.

## 10. Phase boundary

P4 establishes stable structure and content/data behavior without turning into a visual redesign.

P5 closes reliability, settings, backup/restore, accessibility and V1 behavior.

P6 is the main visual refinement phase: spacing, typography, microinteractions, composition polish and distribution.

P7 deepens the Living Garden, flowers, rarity, provenance and pixel-art discovery.

Detailed pixel-level polish should remain in P6/P7 unless required earlier for correctness or accessibility.

## 11. Canonical graphics boundary

Authored button and field artwork may skin real interactive components, but must never become hidden interaction owners. A raster reference cannot intercept touches from a real input, button, navigation item, or other control.

Home1 preserves exactly three quest actions — **SORTEAR QUEST**, **Quest de Jogo**, **Quest Aleatória** — and the single continuous bottom navigation **Início | Jardim | Perfil**. Visual references may refine spacing, texture, typography and composition, but may not invent replacement mechanics or a fourth navigation destination.

## 12. Canonical design rule

> **Pinho Quest should feel like a place to visit, not a tool to operate.**

The architecture may remain rigorous underneath; the interface should feel warm, calm, intuitive and alive.

## 13. P6 product interaction decisions

The detailed interaction contract is canonized in `docs/design/P6_UI_UX_INTERACTION_CONTRACT.md`.

Key invariants:
- **SORTEAR QUEST** opens theme/category selection rather than bypassing selection.
- Theme selections may be combined freely, including all available themes.
- The last valid theme/tag selection is remembered.
- Custom tags may belong to multiple themes and participate while enabled.
- Hybrid quests may combine at most three explicit semantic tags.
- Quest de Jogo receives a game from the dynamic catalog and the selected game remains explicit in the generated quest.
- Garden collections contain nine unique flower identities and the next collection is prepared in background/cache when the current collection is completed.
- Flower identity is keyed by scientific-name-derived hash; collection identity is derived from its nine flower identities.
- Lifetime/level XP and spendable research XP are independent progressions.

## 14. P6 implementation protocol

The official gate sequence, evidence quality rules, checkpoint discipline and transition policy are defined in
[`Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md`](../../Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md).

This contract remains the implementation authority for UI boundaries. The P6 protocol governs how those boundaries are validated and when a gate may be reported as complete; it does not override the visual identity genomes or product interaction contract.

Before implementation:
- verify the exact source asset and Android alias in [`ASSET_CATALOG.md`](ASSET_CATALOG.md);
- verify its visual role in [`CANONICAL_GRAPHICS.md`](CANONICAL_GRAPHICS.md);
- do not use a composition reference as a static interactive screen;
- validate real touch semantics, accessibility, responsive sizing, theme and font scale;
- report a gate as validated only with the evidence required by the P6 protocol.


## 15. P6-E — texto e acessibilidade após revisão de screenshots (2026-10-14)

Os screenshots de runtime revelaram sobreposição de textos que estavam posicionados por offsets percentuais independentes, rótulos sem limite de linhas, quebra ruim do cabeçalho do Jardim e cores sem contraste suficiente no tema noturno das Configurações. As correções iniciais estão registradas em `docs/evidence/p6-e-accessibility-audit-2026-10-14.md`.

Regras de aceite específicas:
- Copiar e métricas sobre cards devem compor um único fluxo ou regiões explicitamente delimitadas; textos irmãos não podem competir por coordenadas verticais independentes quando podem quebrar linhas.
- Rótulos sobre botões ilustrados têm limite explícito de linhas e elipse, e devem reservar a zona do ícone já embutido na arte.
- Cabeçalhos devem usar a largura restante; títulos e métricas precisam de quebra/limite deliberado e o estado semântico deve permanecer no texto acessível.
- O conteúdo rolável reserva o espaço da barra global e continua acessível até o fim.
- Tema noturno nunca depende de verde-escuro sobre fundo escuro; verificar contraste de textos sobre arte, pergaminho e botões.
- Testar escala 1.0 e 1.3, ambos os temas e viewport compacta. Uma compilação de AndroidTest não substitui execução nem evidência em aparelho.
- A fonte de display atual é Ihlimia, extraída do ZIP fornecido para um recurso `res/font` gerado durante o build e usada somente em headings curtos. O ZIP não inclui licença; apesar da indicação do proprietário de que ela é livre, registrar a fonte oficial dos termos de incorporação/redistribuição antes do release. Texto funcional, métricas e controles não usam a fonte manuscrita.


## 16. P6-E follow-up (2026-10-15)

A auditoria e o plano de correção foram atualizados após a CI #38603677089. O defeito de compilação dos testes instrumentados (uso de `assertExists` e ausência de import de `onNodeWithText`) foi corrigido em `LargeFontLayoutTest.kt`. O build agora extrai `ihlimia.ttf` de `docs/design/ihlimia-maisfontes.8b4f.zip`; o uso é restrito a títulos decorativos curtos. Home metrics e rótulo de backup também receberam ajustes de legibilidade. P6-C/P6-E continuam abertos até CI verde no SHA atual, teste visual em aparelho e registro da licença de redistribuição.


## 17. Segunda rodada P6-C/P6-E — screenshots de 15/10/2026

O proprietário validou o primeiro APK visualmente e enviou sete novos prints. O refino subsequente deve preservar a arte canônica e resolver sobreposições com fluxo de layout e superfícies com contraste, não com coordenadas independentes.

- **Navbar:** legenda em `PinhoDisplayFontFamily`, centrada em cada destino igualitário e posicionada mais abaixo que a região de ícone/arte. No tema claro, a aba selecionada usa PinhoForest para contraste; no escuro, PinhoGold continua como destaque.
- **Home:** GardenSummaryCard utiliza toda a largura disponível e um inset PinhoParchment para texto e métricas. O cartão de jogo usa aspecto mais alto (174:100) e rótulo 15sp com reserva à esquerda para o gamepad da arte.
- **Garden:** mensagens e contagem sobre a arte são agrupadas em pergaminho; Abrir coleção usa fundo creme e contorno próprios.
- **Profile:** a placa do nome usa serif legível, largura de texto de 82% e menor padding inferior para que o nome não suba até as folhas. A fonte de display permanece no título breve “Perfil”.
- **Onboarding:** superfície PinhoParchment une título, nome, preview e confirmação, sem alterar o BasicTextField nem o caminho IME Done.
- **Seleção de temas:** o título começa abaixo do wordmark e o título/helper/contador usam superfícies próprias. O fundo ganha tonalização sutil dependente do tema.
- **Settings:** a explicação do cérebro criativo usa painel próprio para se separar da arte desfocada.

**Gate:** a CI [#38690256884](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38690256884) passou para o checkpoint de código `c2e8d05adaf0cff34e58ea81d88718ba74412440` (build OK, 0 erros/38 avisos). A validação visual ainda está pendente: P6-C/P6-E só fecham depois de instalar esse APK e observar escalas 1.0/1.3, temas claro/escuro, viewport compacta, scroll e IME em runtime.


## 18. Aceite do responsável — P6-C/P6-E (2026-10-15)

Rafa revisou o APK no aparelho e aprovou explicitamente P6-C e P6-E para a iteração atual. As pequenas imperfeições que ainda aparecem no posicionamento de textos são aceitas temporariamente e ficam agrupadas numa única etapa futura de polimento: baseline do campo de nome, distância entre título/input/prévia no onboarding, quebra/centralização da frase “Vai aparecer como Jardim de …” e equilíbrio final da navbar/cards.

Não devem ser abertas microcorreções isoladas para esses itens antes da etapa combinada, salvo se surgir nova regressão funcional ou de acessibilidade. A etapa de polimento deve preservar arte canônica, estado, validação, semântica, navegação, callbacks e fluxo IME.

A fonte Ihlimia está visualmente aprovada para os papéis integrados. A falta de licença/README dentro do ZIP continua sendo um item documental de release: registrar a licença original ou autorização explícita para incorporar e redistribuir o TTF em APK público. Esse item não reabre a aprovação visual de P6-E.
