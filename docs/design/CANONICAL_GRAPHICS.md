# PinhoQuest — Registro Canônico de Composição Gráfica

**Branch de referência:** `feature/p6-total-ui-refactor`  
**Status:** contrato normativo de papéis e composição. Não significa que todos os problemas de runtime estejam corrigidos.  
**Fonte de assets:** `docs/design/` na branch P6; o repositório de imagens externo está desatualizado e não é autoritativo.

## 1. Limite entre arte e comportamento

PNGs são backgrounds, superfícies decorativas ou skins. Não substituem estado, texto dinâmico, semântica, hitbox ou ação Compose. O mapa executável fica em `app/build.gradle.kts`; as dimensões e aliases estão em `ASSET_CATALOG.md`; semântica de ação/estado está em `P6_UI_UX_INTERACTION_CONTRACT.md`.

## 2. Componentes gráficos compartilhados

- `PinhoQuestBackground` usa `ContentScale.Crop` por padrão. Backgrounds de 1080×1920 são 9:16, mas Crop pode alterar enquadramento em aparelhos diferentes; validar logo, CTA e áreas focais em tamanho compacto e maior.
- `PinhoGraphicButton` e `PinhoBackButton` usam `ContentScale.Fit` no código observado. A documentação histórica que dizia `FillBounds` está desatualizada. Quando um asset parecer distorcido ou o label não couber, conferir a razão do contêiner e a área segura antes de mudar o PNG.
- Labels, enabled/disabled, estado selecionado, descrição acessível e ação do botão pertencem ao Compose.
- `PinhoParchment` é uma superfície Compose; deve dimensionar-se pelo conteúdo e não recortar textos longos.
- A navegação inferior tem uma única instância global. Os controles Compose fornecem hit targets e ações; a arte fornece fundo/ícones, sem ícones redundantes por cima.

## 3. Famílias de assets

### Backgrounds
- `bg_start_day.png` / `bg_start_night.png`: a arte contém a marca/logotipo. CTA Começar e conteúdo dinâmico são Compose. Não sobrepor outra marca.
- `bg_config_day.png` / `bg_config_night.png`: cenário de Configurações.
- `bg_gardem_art_day.png` / `bg_gardem_art_night.png`: cenário de Jardim quando o estado de domínio indicar progresso/arte.
- `bg_gardem_empty_day.png` / `bg_gardem_empty_night.png`: cenário de Jardim vazio.
- `bg_profile_day.png` / `bg_profile_night.png`: cenário de Perfil.
- O código atual reutiliza o background start também na Home/Quest. Os estados de quest gerada/ativa precisam preservar legibilidade e a zona superior da marca ou usar uma composição aprovada que não duplique logo.

### Botões
- `btn_back.png`: canvas 66×75 px; o Compose determina tamanho visível e área de toque. Não fazer upscale raster artificial.
- `btn_confirm.png`: canvas 187×86 px. A folha incorporada ocupa parte da área esquerda; o label Compose precisa começar após a arte e caber em fontes maiores.
- `btn_quest_draw.png`, `btn_quest_game.png`, `btn_quest_random.png` e `btn_view_quests.png`: skins das ações; label e callbacks são reais. A reserva de label precisa respeitar cada desenho.
- `btn_theme_day.png` / `btn_theme_night.png`: ambos 300×180; usar mesma proporção de canvas e rótulos/seleção em Compose.
- `btn_garden_filter_all.png`, `btn_garden_filter_collected.png`, `btn_garden_filter_searched.png`, `btn_garden_filter_unkw.png`: todos 120×48; a seleção e o texto do filtro são Compose.
- `btn_font_size.png`: canvas 224×68; Menor/Médio/Maior, escala aplicada e estado selecionado são Compose.
- `btn_garden_backup.png` / `btn_support_creator.png`: skins das ações de backup/apoio; callback e feedback vêm do fluxo real.

### Cards
- `card_home_day.png` / `card_home_night.png`: ambos 612×292. Os assets já contêm motivos decorativos, inclusive ícones flor/estrela. Não desenhar emojis de flor ou estrela na mesma área; dados dinâmicos devem ocupar a região de texto livre sem invadir a decoração.
- `card_garden_empty_day.png` / `card_garden_empty_night.png`: ambos 392×316. Diferem de backgrounds de tela inteira.
- `card_flower_unknown_day.png` 174×255 e `card_flower_unknown_night.png` 178×256: já trazem o símbolo “?”/inscrição de desconhecida. Não duplicar esses elementos em Compose sobre a mesma área. O estado de descoberta é autoridade para ocultar/revelar identidade.
- `card_profile_day.png` / `card_profile_night.png`: ambos 996×461.
- `card_profile_tags.png`: 1304×376. Elementos de texto/seleção mutáveis permanecem Compose.

### Campo de nome
- `name_bar.png`: canvas 388×124 px. É moldura de um campo editável real; foco, IME, validação e nome persistido continuam Compose/domínio.

### Navbar
- `nav_bar_day.png`: 768×181; `nav_bar_night.png`: 798×167.
- Os dois PNGs já contêm casa, flor e livro. O item visual não deve desenhar outro símbolo no mesmo ponto. Compose mantém os três destinos, labels, semântica, foco e clique.
- Os ícones fazem parte dos assets, mas rótulos e ações não: confirmar alinhamento do label com cada ícone no teste de tela.

### Categorias
Há nove imagens `category_*` 160×120, fisicamente em `docs/design/Button/` e copiadas pelo mapa `canonicalButtonAssets`. O mapeamento visual aprovado é: Música→`category_music`, Fotografia→`category_photography`, Natureza→`category_nature`, Animais→`category_animal`, Aprender→`category_learn`, Tecnologia→`category_science`, Relaxar→`category_appreciation`, Criar→`category_creativity` e Fantasia→`category_games` (a arte do dragão). Esse alias visual não muda o ID ou a afinidade de domínio. Todas as nove opções visuais agora têm arte; testes protegem esse contrato.

## 4. Regras de composição por tela

### Abertura e onboarding
A marca embutida nos backgrounds start não deve ser duplicada. Botões e campos são controles reais. No código atual da branch P6, a etapa de nome usa rolagem vertical e `imePadding`, com ação IME Done que só avança se `GardenOwnerName` validar o nome; a tela de tags é rolável e reserva menos espaço vazio acima das opções. O item segue em P6-C até ser validado com teclado real aberto e viewport compacta.

### Home/Quest
A Home mantém Sortear Quest, Quest Aleatória e Quest de Jogo. O card de resumo mostra nome do jardim, contagem e XP a partir de estado real. Textos ficam na área segura livre da ilustração; não duplicar ícones já desenhados. P6-B ajustou o resumo ao canvas comum 612×292 e os controles gráficos às proporções canônicas. P6-C começou a aplicar reserva inferior calculada a partir da proporção da arte da navbar e dos insets do sistema às telas de quest; a legibilidade final de conteúdo longo aguarda CI e captura no aparelho.

### Configurações
Usar o cenário config, os botões de tema, fonte, backup e apoio. As imagens de tema têm canvas comum 300×180; o Compose controla seleção e preferência persistida. A navbar continua global e não deve ocultar ações do conteúdo rolável.

### Jardim e coleção
- A fonte de dados é `GardenUiState` e as autoridades de domínio/dados.
- Vazio, jardim em arte e coleção são composições distintas da mesma autoridade; não fabricar XP nem flores.
- A coleção comporta nove posições; filtros alteram a projeção, não o domínio.
- A grade deve começar abaixo dos filtros e usar apenas o espaço restante; o código atual dá o peso vertical da área remanescente à `LazyVerticalGrid` dentro do pergaminho.
- Os quatro filtros preservam rótulos completos; em largura inferior a 520 dp são distribuídos em duas colunas/duas linhas, em vez de esmagar todos numa única linha.
- Cards de flor desconhecida usam proporção retrato e a arte existente sem redesenhar “?” ou “???”. Nome botânico, raridade e estado continuam vindo do domínio.
- P6-C introduziu uma reserva inferior comum calculada a partir da proporção da navbar e dos insets do sistema; CI e teste em aparelho ainda precisam confirmar o resultado.
- Filtro “Pesquisadas” deve corresponder à semântica dos estados de descoberta/revelação no domínio atual.

### Perfil
Nome e preferências vêm do estado real. As nove tags visuais atuais não têm nove artes semanticamente aprovadas; manter superfície neutra onde a associação não existe. Não apresentar superfícies neutras como se fossem um asset quebrado.

## 5. Escala, tema e acessibilidade

- Testar tema claro/escuro, fonte 1.0 e 1.3 e ao menos uma viewport compacta.
- Labels essenciais nunca podem ser cortados ou ficar sob decoração da imagem.
- Hit targets devem ter pelo menos 48 dp, independentemente do tamanho visual da ilustração.
- Estado selecionado não pode depender apenas da cor.
- Nenhum fluxo pode depender de texto incorporado no PNG para descrição acessível ou estado de domínio.
- Texto grande/variável deve poder quebrar linha e rolar; contêiner fixo não deve recortar texto.
- Animação/recompensa visual não pode criar falso sucesso.

## 6. Evidência e gate

Estado de implementação na revisão de 10/10/2026:
- **P6-A:** inventário e aliases canônicos reconciliados; consultar o protocolo para o SHA/CI de evidência.
- **P6-B:** concluído nesta rodada conforme aceitação provisória do proprietário; [Android CI #38081485398](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38081485398) passou no SHA `ee42ea2748e3b8e79d46d9d37983153619b83d8f`. Polimentos visuais residuais não são declarados resolvidos por esse gate.
- **P6-C:** implementação em andamento na branch `feature/p6-total-ui-refactor`. Jardim, coleção, safe areas e IME tiveram correções iniciais; a execução de CI do SHA `31e603392d89734102e7b89a09144efc9886c494` estava em andamento na revisão. Ainda faltam CI verde ligado ao novo SHA e capturas no aparelho. Não declarar gate visual PASS só por compilar.
- **P6-E:** o mapeamento visual das nove categorias foi aprovado pelo proprietário. Licença/procedência da MS Boli e distribuição verificável do modelo local continuam pendentes.
