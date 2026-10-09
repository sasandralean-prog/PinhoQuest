# PinhoQuest — Canonical Graphics Registry

**Status:** registro normativo dos papéis visuais para P6/P7.  
**Branch de referência:** feature/p6-total-ui-refactor.  
**Autoridade:** este documento classifica papéis visuais e composições. O inventário e os aliases vivem em ASSET_CATALOG.md e no mapa executável de app/build.gradle.kts.

## 1. Limite canônico

Os PNGs são fundos, superfícies decorativas ou skins de componentes reais. Não substituem estado, texto dinâmico, semântica, hitbox ou ação Compose. Screenshots descrevem composição; nunca devem ser colocados sobre a UI interativa como uma tela estática.

## 2. Autoridade visual

A cadeia de autoridade segue:
1. docs/identity/PINHO_QUEST_VISUAL_IDENTITY_GENOME.md
2. docs/identity/PINHO_QUEST_GARDEN_PIXEL_ART_GENOME.md
3. docs/design/UI_DESIGN_CONTRACT.md
4. este registro
5. artes individuais classificadas aqui e em ASSET_CATALOG.md

Home1 continua a composição canônica de Home/Quest: refinar, sem alterar a semântica das ações ou a navegação. A barra global permanece Início | Jardim | Perfil.

## 3. Família de backgrounds

- bg_start_day.png / bg_start_night.png: cenário da abertura. O botão Começar é controle separado. A marca e o slogan devem ser Compose ou asset de marca separado, não duplicados no fundo.
- bg_config_day.png / bg_config_night.png: ambiente interno para Configurações.
- bg_gardem_art_day.png / bg_gardem_art_night.png: cenário da composição Jardim em arte, quando os dados de domínio indicarem progresso.
- bg_gardem_empty_day.png / bg_gardem_empty_night.png: cenário da composição de jardim vazio.
- bg_profile_day.png / bg_profile_night.png: cenário de Perfil. Reutilização em cards/empty states só é permitida quando a composição testada não conflitar com bordas, texto ou controles.

A grafia gardem é intencionalmente preservada nesta geração por decisão do proprietário. Qualquer renome futuro exige atualizar catálogo, Gradle e consumidores no mesmo checkpoint.

## 4. Componentes gráficos

- btn_back.png: skin de voltar; o hit target e a semântica são Compose. O canvas de 66×75 px exige teste em escala real.
- btn_confirm.png, btn_quest_draw.png, btn_quest_game.png, btn_quest_random.png, btn_view_quests.png: skins de ações reais.
- btn_font_size.png: skin dos controles de fonte; rótulos Menor/Médio/Maior, escala aplicada e estado selecionado são Compose.
- btn_theme_day.png / btn_theme_night.png: skin dos seletores; valor e persistência vêm das preferências.
- btn_garden_backup.png / btn_support_creator.png: decoração das ações de backup e apoio; callbacks e feedback são Compose.
- btn_garden_collection.png: skin/ícone para abrir a coleção.
- btn_garden_filter_all.png, btn_garden_filter_collected.png, btn_garden_filter_searched.png, btn_garden_filter_unkw.png: skins de filtro. A seleção e os resultados derivados do domínio não são rasterizados.
- category_*.png: imagens para opções de categoria. A presença do PNG não define automaticamente Theme/Tag nem autoriza o mapeamento semântico.
- card_flower_unknown_day.png / card_flower_unknown_night.png: placeholder de flor desconhecida; não revela identidade botânica.
- card_home_day.png / card_home_night.png: superfície para o resumo Home; nome, contagem de flores, XP e mensagens ficam em Compose.
- card_profile_day.png / card_profile_night.png / card_profile_tags.png: superfícies de perfil; conteúdo e seleção continuam reais.
- card_garden_empty_day.png / card_garden_empty_night.png: componente ilustrado do estado vazio, distinto do background de tela inteira.
- name_bar.png: moldura para um campo real, com foco, teclado e validação no Compose.
- nav_bar_day.png / nav_bar_night.png: base visual para a única navbar global. Destinos, rótulos acessíveis, seleção e hit targets continuam um componente Compose compartilhado.

## 5. Composições de tela

### Abertura
Usar bg_start_day/night e montar logo/marca, slogan e CTA em camadas independentes. O botão Começar não faz parte do background. Validar a zona segura e o recorte em telas com proporções distintas.

### Home/Quest
Preservar as três ações: SORTEAR QUEST, Quest de Jogo e Quest Aleatória. O resumo pode usar card_home_day/night; nome do jardim, flores e XP são dados dinâmicos. Não inferir que bg_start seja também background da Home sem confirmar a intenção de composição e o fluxo de navegação.

### Onboarding — nome
Usar cenário coerente com tema. name_bar é uma moldura; o campo digitável é real. O valor do nome, foco, teclado e validação permanecem Compose/domínio.

### Onboarding — categorias/tags
Usar category_* somente depois de mapear os itens aos IDs de Theme/Tag do catálogo de domínio. Não converter “categoria” automaticamente em “tag”. Assets sem correspondência aprovada ficam não mapeados.

### Configurações
Usar bg_config_day/night, btn_theme_day/night, btn_font_size, btn_garden_backup e btn_support_creator. Títulos e labels são texto real. A navbar global segue o contrato de destinos.

### Jardim vazio
Usar bg_gardem_empty_day/night e, quando a composição aprovada pedir, card_garden_empty_day/night. A mensagem e CTA são Compose e não podem fingir flores coletadas.

### Jardim em arte
Usar bg_gardem_art_day/night quando o domínio indicar progresso. A arte de fundo não fabrica flores, XP, raridade ou identidade de coleção.

### Coleção
Mostrar nove posições da coleção ativa com dados de domínio. card_flower_unknown_day/night pode preencher placeholders permitidos pelo estado de descoberta. Filtros mudam a projeção visual, não criam uma segunda fonte de estado.

### Perfil
Usar bg_profile_day/night e surfaces card_profile_day/night e card_profile_tags.png conforme composição validada. Preferências e estado das tags vêm do catálogo e dos dados reais.

## 6. Escala, tema e acessibilidade

- Os fundos são imagens 9:16 em 1536×2752, exceto bg_config_night (784×1342); revisar o par diurno/noturno antes do gate.
- card_home_day/night e nav_bar_day/night possuem proporções diferentes dentro do par; cada elemento precisa manter razão própria ou a arte deve ser normalizada numa decisão explícita.
- PinhoGraphicButton usa ContentScale.FillBounds no código atualmente observado; isso pode distorcer os novos assets se a razão do container divergir. P6-B deve retirar esse comportamento genérico para componentes que precisem preservar proporção.
- Texto configurável e dinâmico nunca deve ser rasterizado em background ou card.
- Controles gráficos oferecem área de toque adequada independentemente do tamanho visível, descrição acessível e estado selecionado perceptível sem depender exclusivamente de cor.
- Overlay de tema deve ser validado visualmente e não substituir um par específico de assets quando este existe.

## 7. Gate de canonização

Os papéis desta registry são decisões de design; não significam que a integração foi concluída. Para P6-A PASS, o catálogo, o diretório real, o mapa Gradle, os aliases e os consumidores Kotlin precisam concordar. O gate também exige executar as verificações disponíveis e registrar evidência. Para P6-B PASS, cada tela precisa de captura reproduzível, build/testes pertinentes e verificação de toque, proporção, tema e escala de fonte.
