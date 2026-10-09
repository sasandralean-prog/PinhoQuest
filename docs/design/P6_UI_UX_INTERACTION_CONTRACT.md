# PinhoQuest — P6 UI/UX Interaction Contract

**Status:** contrato canônico de interação P6.  
**Escopo:** Home/Quest, onboarding, temas/tags, Perfil, Configurações e Jardim/coleção/descoberta.

Este contrato governa semântica de ações e estado de produto. O inventário e aliases ficam em ASSET_CATALOG.md; os papéis visuais e composições ficam em CANONICAL_GRAPHICS.md. A arte é skin/composição, nunca a autoridade do comportamento.

## 1. Home/Quest

A Home mantém exatamente três ações:
1. **SORTEAR QUEST** — abre seleção de temas/categorias.
2. **Quest de Jogo** — utiliza contexto de jogo do catálogo dinâmico; o jogo selecionado deve continuar explícito na quest gerada.
3. **Quest Aleatória** — usa temas/tags habilitados dentro do contrato governado de geração.

As skins podem usar btn_quest_draw.png, btn_quest_game.png e btn_quest_random.png. O CTA, callback, estado carregando/desabilitado e rótulo acessível continuam Compose.

A superfície de resumo pode usar card_home_day/night. Nome do jardim, contagem de flores, XP e copy variável permanecem dados e texto reais.

## 2. Tema, tag e arte de categoria

Tema selecionável, tag semântica e imagem de categoria são conceitos diferentes. O pacote atual inclui category_animal, category_appreciation, category_creativity, category_games, category_learn, category_music, category_nature, category_photography e category_science.

A presença de um PNG não cria um novo Theme/Tag nem atualiza o conjunto canônico do domínio. Cada asset visual precisa de associação explícita a um ID de domínio antes de ser selecionável. Em particular, não equiparar appreciation a affection, learn a learning, science a technology ou animal a animals sem verificar o catálogo e aprovar o mapeamento.

A seleção aceita combinações dos temas disponíveis e restaura a última seleção válida. Tags podem pertencer a mais de um tema conforme o modelo vigente. O limite de até três tags aplica-se à combinação semântica explícita de uma quest híbrida, não ao número de preferências habilitadas.

## 3. Onboarding

O nome do jardim deve ser inserido num campo editável real. name_bar.png, quando usado, é moldura decorativa; foco, teclado, limite de caracteres, validação e persistência permanecem Compose/domínio.

Os rótulos e o slogan não devem depender de texto gravado no fundo. O botão de confirmação é uma skin para um controle com ação real.

## 4. Configurações

O tema e o tamanho de fonte pertencem à autoridade de preferências já existente. btn_theme_day/night e btn_font_size são skins. Valor selecionado, persistência e escala de texto permanecem estado/configuração real.

Backup e apoio ao criador só são mostrados como ações funcionais quando há callback/fluxo disponível. Arte não representa confirmação de sucesso; feedback vem da operação real.

## 5. Perfil

card_profile_day/night e card_profile_tags são superfícies visuais. Nome, preferências, tags ativadas, texto de perfil e seleção são dinâmicos e permanecem Compose/dados governados. Configurações é um fluxo interno, não uma quarta aba global.

## 6. Jardim e coleção

O Jardim possui três composições da mesma fonte de dados: vazio, jardim em arte e coleção aberta.

- **Vazio:** bg_gardem_empty_day/night; opcionalmente card_garden_empty_day/night quando a composição aprovada pedir. Não fabricar flores coletadas; CTA convida para o fluxo de quests.
- **Jardim em arte:** bg_gardem_art_day/night quando o estado de domínio indicar progresso.
- **Coleção:** nove posições por coleção. card_flower_unknown_day/night só representa um placeholder quando o estado de descoberta permitir ocultar a identidade.

Os filtros visuais representam Todas, Coletadas, Pesquisadas e Desconhecidas, mas não criam estados de domínio novos. Mapear “Pesquisadas” à semântica exata do modelo atual; não confundir pesquisa/revelação com coleta. Id, raridade, pertença à coleção e progressão são governados por domínio/dados, nunca inferidos do PNG.

Abrir ou fechar a coleção altera apresentação/navegação, não gera coleções, gasta XP ou altera a identidade de flores.

## 7. Texto, estado e imagem

Texto mutável, localizável, acessível ou sensível à escala de fonte fica no Compose. PNGs podem fornecer textura, molduras, ícones e skins. Texto visual estático só pode permanecer incorporado quando for parte decorativa intencional e não substituir label acessível ou dado dinâmico.

## 8. Invariantes

- Uma única barra global: **Início | Jardim | Perfil**.
- A arte não intercepta clique nem substitui um campo editável.
- Tema, fonte, tags, flor, raridade, XP e coleção vêm de autoridades reais.
- Trocar o nome de um asset não muda semântica do domínio.
- P6 gates e evidência de validação são governados por Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md.
