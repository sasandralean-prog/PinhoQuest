# P6-A — Canonical Asset Catalog

**Branch de referência:** feature/p6-total-ui-refactor  
**Status:** inventário documental atualizado após inspeção dos assets publicados na branch `feature/p6-total-ui-refactor`. Os PNGs estão nos diretórios canônicos abaixo; a sincronização do mapa Gradle e o consumo em runtime ainda precisam de validação. As dimensões antigas do ZIP não devem ser usadas como referência para botões/cards/nav depois da padronização mais recente.

## Autoridade

- Este documento cataloga nomes de origem, dimensões, classificação e aliases Android propostos.
- app/build.gradle.kts deve ser a implementação executável do mesmo mapeamento. A lista deste documento e o mapa Gradle precisam ser reconciliados no P6-A.
- CANONICAL_GRAPHICS.md define o papel visual e a composição por tela.
- P6_UI_UX_INTERACTION_CONTRACT.md define semântica de interação; assets não criam estado nem regras de domínio.
- Referências em docs/design/Screen descrevem composição e não devem ser usadas como telas estáticas interativas.

## Fonte de arquivos

A fonte publicada na branch está em `docs/design/Background/`, `docs/design/Button/`, `docs/design/Card/` e `docs/design/Navbar/` (grafia exata do diretório). Dois assets `card_flower_unknown_day.png` e `card_flower_unknown_night.png` foram movidos de Button para Card. A grafia `gardem` e a abreviação `unkw` são preservadas por decisão do proprietário. O arquivo Gradle precisa refletir a nova localização dos dois cards.

## Backgrounds

Todos são PNG RGB opacos, sem canal alfa. Os dez backgrounds padronizados usam canvas 1080×1920 (9:16); validar o enquadramento no Compose sem deformar a arte.

| Origem | Dimensões | Alias Android proposto | Papel |
|---|---:|---|---|
| Background/bg_config_day.png | 1536×2752 | canonical_bg_config_day | Configurações — dia |
| Background/bg_config_night.png | 784×1342 | canonical_bg_config_night | Configurações — noite; dimensões diferem do par diurno |
| Background/bg_gardem_art_day.png | 1536×2752 | canonical_bg_gardem_art_day | Jardim ilustrado — dia |
| Background/bg_gardem_art_night.png | 1536×2752 | canonical_bg_gardem_art_night | Jardim ilustrado — noite |
| Background/bg_gardem_empty_day.png | 1536×2752 | canonical_bg_gardem_empty_day | Jardim vazio — dia |
| Background/bg_gardem_empty_night.png | 1536×2752 | canonical_bg_gardem_empty_night | Jardim vazio — noite |
| Background/bg_profile_day.png | 1536×2752 | canonical_bg_profile_day | Perfil — dia |
| Background/bg_profile_night.png | 1536×2752 | canonical_bg_profile_night | Perfil — noite |
| Background/bg_start_day.png | 1536×2752 | canonical_bg_start_day | Abertura — dia |
| Background/bg_start_night.png | 1536×2752 | canonical_bg_start_night | Abertura — noite |

## Botões e artes de categoria

Todos são PNG RGBA com transparência. Para botões/categorias, consultar as dimensões dos arquivos publicados; medidas do ZIP anterior foram substituídas pela exportação padronizada. As dimensões do canvas não definem por si só o tamanho final de exibição.

| Origem | Dimensões | Alias Android proposto | Papel |
|---|---:|---|---|
| Button/btn_back.png | dimensão atual do asset publicado | btn_back | Botão/ícone de voltar; validar nitidez no tamanho real |
| Button/btn_confirm.png | dimensão atual do asset publicado | btn_confirm | Skin de confirmação |
| Button/btn_font_size.png | dimensão atual do asset publicado | btn_font_size | Skin reutilizável para controles de tamanho de fonte; rótulos/estado são Compose |
| Button/btn_garden_backup.png | dimensão atual do asset publicado | btn_garden_backup | Skin de backup |
| Button/btn_garden_collection.png | dimensão atual do asset publicado | btn_garden_collection | Ícone/skin para abrir coleção |
| Button/btn_garden_filter_all.png | dimensão atual do asset publicado | btn_garden_filter_all | Filtro Todas |
| Button/btn_garden_filter_collected.png | dimensão atual do asset publicado | btn_garden_filter_collected | Filtro Coletadas |
| Button/btn_garden_filter_searched.png | dimensão atual do asset publicado | btn_garden_filter_searched | Filtro Pesquisadas; alinhar com estado de domínio vigente |
| Button/btn_garden_filter_unkw.png | dimensão atual do asset publicado | btn_garden_filter_unkw | Filtro Desconhecidas; nome abreviado preservado |
| Button/btn_quest_draw.png | dimensão atual do asset publicado | btn_quest_draw | Ação Sortear quest |
| Button/btn_quest_game.png | dimensão atual do asset publicado | btn_quest_game | Ação Quest de Jogo |
| Button/btn_quest_random.png | dimensão atual do asset publicado | btn_quest_random | Ação Quest Aleatória |
| Button/btn_support_creator.png | dimensão atual do asset publicado | btn_support_creator | Skin de apoio ao criador |
| Button/btn_theme_day.png | dimensão atual do asset publicado | btn_theme_day | Seletor tema dia |
| Button/btn_theme_night.png | dimensão atual do asset publicado | btn_theme_night | Seletor tema noite |
| Button/btn_view_quests.png | dimensão atual do asset publicado | btn_view_quests | Skin Ver quests |
| Card/card_flower_unknown_day.png | dimensão do asset publicado | card_flower_unknown_day | Card placeholder de flor, dia; classificado como card apesar da pasta Button |
| Card/card_flower_unknown_night.png | dimensão do asset publicado | card_flower_unknown_night | Card placeholder de flor, noite; classificado como card apesar da pasta Button |
| Button/category_animal.png | dimensão atual do asset publicado | category_animal | Arte de categoria; associação semântica ainda exige validação |
| Button/category_appreciation.png | dimensão atual do asset publicado | category_appreciation | Arte de categoria; não inferir equivalência com outro conceito |
| Button/category_creativity.png | dimensão atual do asset publicado | category_creativity | Arte de categoria Criatividade |
| Button/category_games.png | dimensão atual do asset publicado | category_games | Arte de categoria Jogos |
| Button/category_learn.png | dimensão atual do asset publicado | category_learn | Arte de categoria Aprender |
| Button/category_music.png | dimensão atual do asset publicado | category_music | Arte de categoria Música |
| Button/category_nature.png | dimensão atual do asset publicado | category_nature | Arte de categoria Natureza |
| Button/category_photography.png | dimensão atual do asset publicado | category_photography | Arte de categoria Fotografia |
| Button/category_science.png | dimensão atual do asset publicado | category_science | Arte de categoria Ciência |

## Cards e painéis

Todos são PNG RGBA com transparência. As medidas do ZIP anterior foram substituídas por exportações padronizadas; confirmar a dimensão atual diretamente no PNG antes de definir dimensões Compose.

| Origem | Dimensões | Alias Android proposto | Papel |
|---|---:|---|---|
| Card/card_garden_empty_day.png | dimensão atual do asset publicado | card_garden_empty_day | Ilustração/card do estado vazio — dia |
| Card/card_garden_empty_night.png | dimensão atual do asset publicado | card_garden_empty_night | Ilustração/card do estado vazio — noite |
| Card/card_home_day.png | dimensão atual do asset publicado | card_home_day | Superfície do resumo Home — dia; dados dinâmicos ficam em Compose |
| Card/card_home_night.png | dimensão atual do asset publicado | card_home_night | Superfície do resumo Home — noite; proporção difere do par diurno |
| Card/card_profile_day.png | dimensão atual do asset publicado | card_profile_day | Superfície do perfil — dia |
| Card/card_profile_night.png | dimensão atual do asset publicado | card_profile_night | Superfície do perfil — noite |
| Card/card_profile_tags.png | dimensão atual do asset publicado | card_profile_tags | Superfície decorativa para área de tags/perfil |

## Navegação e campo de nome

Todos são PNG RGBA com transparência.

| Origem | Dimensões | Alias Android proposto | Papel |
|---|---:|---|---|
| NavBar/name_bar.png | dimensão atual do asset publicado | name_bar | Moldura para campo de nome real |
| NavBar/nav_bar_day.png | dimensão atual do asset publicado | nav_bar_day | Skin/base da navegação inferior — dia |
| NavBar/nav_bar_night.png | dimensão atual do asset publicado | nav_bar_night | Skin/base da navegação inferior — noite; proporção difere do par diurno |

## Regras de consumo

1. Backgrounds: escolher Crop/Fit conforme o foco da composição; não deformar para preencher. Revalidar áreas de foco em telas diferentes.
2. Botões: imagem é skin. Callback, enabled/disabled, seleção, descrição acessível e hit target pertencem ao Compose.
3. Texto dinâmico ou sujeito a escala, tradução e acessibilidade — nome do jardim, XP, flores, rótulos de filtros, títulos, feedback — deve ser texto real em Compose.
4. Cards: usar a arte como superfície; dados mutáveis não devem ser rasterizados no card.
5. Navbar: os três destinos globais continuam Início | Jardim | Perfil. A arte pode ser o fundo visual, mas cada destino deve ter ação e área de toque real.
6. Categoria visual e tag semântica não são sinônimos. Não conectar category_* a IDs de Theme/Tag apenas por semelhança de nome.
7. Não usar ContentScale.FillBounds para skins, cards ou ícones quando isso alterar a proporção. Usar dimensionamento por razão do asset ou uma estratégia de crop aprovada.
8. Não executar upscaling automático de btn_back.png; validar a nitidez no tamanho de uso ou regenerar a arte se necessário.

## Pontos pendentes antes do P6-A PASS

- Confirmar por build que o mapeamento publicado funciona e que os 47 assets são gerados sem duplicação ou ausência. Corrigir o mapa `canonicalButtonAssets` no Gradle: os dois `card_flower_unknown_*.png` agora estão em `docs/design/Card/`, não em `docs/design/Button/`.
- Os backgrounds foram padronizados para 1080×1920; validar enquadramento em telas reais, sem esticar a arte.
- Verificar as dimensões da exportação atual de cards e navbar antes de definir escala de exibição; medidas antigas do ZIP não são normativas.
- Mapear semanticamente category_* após revisar o catálogo do domínio; itens ambíguos ficam pendentes, não associados por aproximação.
- Atualizar consumidores Kotlin, executar verifyCanonicalUiAssets, build/testes relevantes e registrar resultado. Este documento por si só não valida runtime.
