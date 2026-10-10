# P6-A — Catálogo Canônico de Assets

**Branch de referência:** `feature/p6-total-ui-refactor`  
**Snapshot de código de base desta revisão:** `cc385fac097112d76e5e5381b8ac9040354c280f`  
**Estado:** os 47 mapeamentos publicados foram reconciliados com `app/build.gradle.kts`. Este catálogo descreve o inventário e os aliases de build; não declara que os consumidores Compose já usam todas as proporções corretamente.

## Autoridade e mecanismo de build

- A fonte canônica dos assets do APK nesta branch é `docs/design/`. O repositório de imagens externo está desatualizado e não é fonte de verdade para esta revisão.
- `app/build.gradle.kts` define quatro mapas: `canonicalBackgroundAssets`, `canonicalButtonAssets`, `canonicalCardAssets` e `canonicalNavBarAssets`.
- O Gradle sincroniza os arquivos para `app/build/generated/p6-canonical-ui-res/drawable-nodpi`. Backgrounds recebem aliases `canonical_bg_*`; os outros grupos usam seus nomes Android-safe já existentes.
- O verificador `:app:verifyCanonicalUiAssets` é dependência de `preBuild` e verifica fontes requeridas, saídas geradas, nomes de saída duplicados e o total esperado de 47 mapeamentos. O sucesso da verificação não certifica proporção, legibilidade ou composição em runtime.
- `docs/design/CANONICAL_GRAPHICS.md` define o papel visual e as composições; `docs/design/P6_UI_UX_INTERACTION_CONTRACT.md` governa interação e estado. Um PNG nunca é autoridade de estado do produto.
- A grafia `gardem` e a abreviação `unkw` são preservadas. Não renomear sem atualizar arquivos, mapa Gradle, catálogo, consumidores e testes no mesmo checkpoint.
- Os arquivos auxiliares de 1 byte chamados `Null` não fazem parte dos 47 mapas e não devem ser tratados como imagens.

## Backgrounds

Todos os dez backgrounds são PNG RGB opacos padronizados para canvas 1080 × 1920 px (9:16). O alias gerado é o mostrado abaixo.

| Origem publicada | Dimensão | Arquivo/alias gerado | Papel |
|---|---:|---|---|
| `Background/bg_config_day.png` | 1080 × 1920 | `canonical_bg_config_day.png` | Configurações — dia |
| `Background/bg_config_night.png` | 1080 × 1920 | `canonical_bg_config_night.png` | Configurações — noite |
| `Background/bg_gardem_art_day.png` | 1080 × 1920 | `canonical_bg_gardem_art_day.png` | Jardim em arte — dia |
| `Background/bg_gardem_art_night.png` | 1080 × 1920 | `canonical_bg_gardem_art_night.png` | Jardim em arte — noite |
| `Background/bg_gardem_empty_day.png` | 1080 × 1920 | `canonical_bg_gardem_empty_day.png` | Jardim vazio — dia |
| `Background/bg_gardem_empty_night.png` | 1080 × 1920 | `canonical_bg_gardem_empty_night.png` | Jardim vazio — noite |
| `Background/bg_profile_day.png` | 1080 × 1920 | `canonical_bg_profile_day.png` | Perfil — dia |
| `Background/bg_profile_night.png` | 1080 × 1920 | `canonical_bg_profile_night.png` | Perfil — noite |
| `Background/bg_start_day.png` | 1080 × 1920 | `canonical_bg_start_day.png` | Abertura/Home/Quest — dia conforme consumidores atuais |
| `Background/bg_start_night.png` | 1080 × 1920 | `canonical_bg_start_night.png` | Abertura/Home/Quest — noite conforme consumidores atuais |

Os backgrounds de abertura incluem a marca/logotipo. O CTA é Compose. Não desenhar uma segunda marca sobre a arte. Como o código usa Crop por padrão para backgrounds, a posição do conteúdo deve ser validada em proporções e tamanhos diferentes.

## Botões e artes de categoria

Os PNGs desta seção têm transparência RGBA. Dimensão do canvas não é, por si só, tamanho de exibição em dp. O componente consumidor precisa respeitar a proporção e manter texto, callback, estado e área acessível como Compose.

| Origem publicada | Dimensão | Arquivo/alias gerado | Papel |
|---|---:|---|---|
| `Button/btn_back.png` | 66 × 75 | `btn_back.png` | Voltar; não ampliar artificialmente o raster |
| `Button/btn_confirm.png` | 187 × 86 | `btn_confirm.png` | Confirmação |
| `Button/btn_font_size.png` | 224 × 68 | `btn_font_size.png` | Skin para Menor/Médio/Maior |
| `Button/btn_garden_backup.png` | 682 × 160 | `btn_garden_backup.png` | Backup/exportação |
| `Button/btn_garden_collection.png` | 62 × 54 | `btn_garden_collection.png` | Acesso visual à coleção |
| `Button/btn_garden_filter_all.png` | 120 × 48 | `btn_garden_filter_all.png` | Filtro Todas |
| `Button/btn_garden_filter_collected.png` | 120 × 48 | `btn_garden_filter_collected.png` | Filtro Coletadas |
| `Button/btn_garden_filter_searched.png` | 120 × 48 | `btn_garden_filter_searched.png` | Filtro Pesquisadas; confirmar semântica com o estado de domínio |
| `Button/btn_garden_filter_unkw.png` | 120 × 48 | `btn_garden_filter_unkw.png` | Filtro Desconhecidas |
| `Button/btn_quest_draw.png` | 349 × 95 | `btn_quest_draw.png` | Sortear quest |
| `Button/btn_quest_game.png` | 174 × 86 | `btn_quest_game.png` | Quest de Jogo |
| `Button/btn_quest_random.png` | 165 × 90 | `btn_quest_random.png` | Quest Aleatória |
| `Button/btn_support_creator.png` | 428 × 128 | `btn_support_creator.png` | Apoio ao criador |
| `Button/btn_theme_day.png` | 300 × 180 | `btn_theme_day.png` | Selecionar tema dia |
| `Button/btn_theme_night.png` | 300 × 180 | `btn_theme_night.png` | Selecionar tema noite |
| `Button/btn_view_quests.png` | 258 × 87 | `btn_view_quests.png` | Ver quests |
| `Button/category_animal.png` | 160 × 120 | `category_animal.png` | Arte de categoria; associação semântica controlada |
| `Button/category_appreciation.png` | 160 × 120 | `category_appreciation.png` | Arte de categoria; associação ainda não aprovada |
| `Button/category_creativity.png` | 160 × 120 | `category_creativity.png` | Arte de categoria Criatividade; associação ainda precisa ser aprovada |
| `Button/category_games.png` | 160 × 120 | `category_games.png` | Arte de Jogos; não cria tag nova |
| `Button/category_learn.png` | 160 × 120 | `category_learn.png` | Arte de Aprender; conferir o ID semântico existente |
| `Button/category_music.png` | 160 × 120 | `category_music.png` | Arte de Música |
| `Button/category_nature.png` | 160 × 120 | `category_nature.png` | Arte de Natureza |
| `Button/category_photography.png` | 160 × 120 | `category_photography.png` | Arte de Fotografia |
| `Button/category_science.png` | 160 × 120 | `category_science.png` | Arte de Ciência; não equiparar automaticamente a Tecnologia |

Os arquivos de categorias permanecem fisicamente em `docs/design/Button/` e fazem parte de `canonicalButtonAssets` para fins de diretório/fonte de build. Essa classificação física não determina sua semântica de produto. O código atual mapeia cinco IDs visuais a imagens; os restantes usam superfície neutra. Resolver em P6-E após conferir `SystemTagCatalog` e aprovar os IDs, não por semelhança lexical.

## Cards

Todos são PNG RGBA. Os dois cards de flor desconhecida estão fisicamente em `docs/design/Card/` e constam em `canonicalCardAssets` no mapa Gradle atual.

| Origem publicada | Dimensão | Arquivo/alias gerado | Papel |
|---|---:|---|---|
| `Card/card_flower_unknown_day.png` | 174 × 255 | `card_flower_unknown_day.png` | Placeholder de flor desconhecida — dia |
| `Card/card_flower_unknown_night.png` | 178 × 256 | `card_flower_unknown_night.png` | Placeholder de flor desconhecida — noite |
| `Card/card_garden_empty_day.png` | 392 × 316 | `card_garden_empty_day.png` | Card ilustrado do jardim vazio — dia |
| `Card/card_garden_empty_night.png` | 392 × 316 | `card_garden_empty_night.png` | Card ilustrado do jardim vazio — noite |
| `Card/card_home_day.png` | 612 × 292 | `card_home_day.png` | Superfície do resumo da Home — dia |
| `Card/card_home_night.png` | 612 × 292 | `card_home_night.png` | Superfície do resumo da Home — noite |
| `Card/card_profile_day.png` | 996 × 461 | `card_profile_day.png` | Superfície do perfil — dia |
| `Card/card_profile_night.png` | 996 × 461 | `card_profile_night.png` | Superfície do perfil — noite |
| `Card/card_profile_tags.png` | 1304 × 376 | `card_profile_tags.png` | Superfície decorativa para preferências |

As artes de card podem conter símbolos decorativos. Antes de sobrepor qualquer texto ou emoji, verificar se ele já existe no PNG. Nome, XP, contagem, descrição, raridade e estados mutáveis permanecem dados/Compose. As proporções acima são canvases canônicos; não implicam que os consumidores Kotlin estejam todos atualizados.

## Navbar e campo de nome

PNG RGBA com transparência:

| Origem publicada | Dimensão | Arquivo/alias gerado | Papel |
|---|---:|---|---|
| `Navbar/name_bar.png` | 388 × 124 | `name_bar.png` | Moldura; o campo digitável é Compose |
| `Navbar/nav_bar_day.png` | 768 × 181 | `nav_bar_day.png` | Base ilustrada da navegação inferior — dia |
| `Navbar/nav_bar_night.png` | 798 × 167 | `nav_bar_night.png` | Base ilustrada da navegação inferior — noite |

Os dois assets de navbar já contêm os desenhos de casa, flor e livro. Não desenhar ícones duplicados por cima. O Compose continua responsável por alvos de toque, rótulos, semântica, seleção e ações.

## Regras de consumo e gates

1. Background: `PinhoQuestBackground` usa `ContentScale.Crop` por padrão. Preservar proporção e validar áreas de foco; não esticar.
2. Botão e botão voltar: as imagens usam `ContentScale.Fit` no componente compartilhado atual. Corrigir a proporção do contêiner consumidor quando necessário, em vez de deformar o asset.
3. Nome, XP, flores, título, descrição, rótulo, seleção, feedback e estado devem continuar em Compose/dados reais.
4. Navbar tem exatamente três destinos globais: Início | Jardim | Perfil.
5. Categoria visual não é sinônimo de Tag/Theme. Todo mapeamento deve ser explícito e validado com o domínio.
6. Não fazer upscale artificial de `btn_back.png`. Validar o tamanho visual e o alvo de toque real.
7. Os 47 mapeamentos atuais dividem-se em 10 Background, 25 Button (incluindo os nove `category_*`), 9 Card e 3 Navbar.

## Estado P6-A do catálogo

- **Inventário e aliases:** reconciliados com `app/build.gradle.kts` da branch observada.
- **Cards de flor desconhecida:** paths e grupo Gradle já corretos; não há alteração necessária no mapa só por causa dessa movimentação.
- **Dimensões:** registradas a partir da padronização dos pacotes e confrontadas com os assets publicados em amostras rastreáveis. Antes de uma mudança de arte, repetir a leitura do PNG efetivamente versionado.
- **Build/runtime:** o catálogo, por si só, não prova comportamento em aparelho. A CI do SHA base passou; o commit documental desta revisão precisa ser acompanhado pela execução do CI no SHA resultante.
- **P6-B/C:** continuam responsáveis por corrigir os parâmetros de proporção, composição, safe areas, textos e navegação observados nas screenshots.
