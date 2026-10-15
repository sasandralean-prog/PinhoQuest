# PinhoQuest — P6 UI/UX Interaction Contract

**Status:** contrato canônico de semântica de interação P6, alinhado ao código inspecionado em `feature/p6-total-ui-refactor`.  
**Limite:** este texto define comportamento esperado; não afirma que os defeitos de composição já foram corrigidos. O inventário está em `ASSET_CATALOG.md` e os papéis visuais em `CANONICAL_GRAPHICS.md`.

## 1. Home e Quest

A tela principal oferece três ações:
1. **Sortear Quest** — abre a seleção de temas.
2. **Quest Aleatória** — solicita o modo RANDOM e deve respeitar filtros/contratos governados.
3. **Quest de Jogo** — exige candidato válido do catálogo de jogos; não pode se transformar silenciosamente numa quest NORMAL.

As ações, rótulos acessíveis, loading/disabled e callbacks são Compose. O background atual de Home/Quest usa `bg_start_day/night`, cuja arte contém marca/logotipo. Manter sua zona superior legível, sem redesenhar a marca.

`card_home_day.png` e `card_home_night.png` são superfícies de 612×292. Nome do jardim, número de flores, XP e copy são dinâmicos. A arte já contém motivos decorativos de flor/estrela; não sobrepor emojis duplicados. Valores devem permanecer reais e ser apresentados na área livre, sem invadir a árvore/decoração.

Quest gerada e quest ativa são estados distintos. Título, descrição, objetivos, duração, dificuldade, status e ações são dados e controles reais. Saídas geradas devem passar pelo `QuestValidator`; fallback procedural é válido quando o modelo não está disponível, mas o app não pode alegar que a inferência local está ativa quando não está.

## 2. Temas, tags e artes de categoria

Tema de aparência, tag/preferência semântica e imagem de categoria são conceitos distintos. A presença de PNG não cria ID nem muda as categorias que o core entende.

O catálogo de domínio atual (`SystemTagCatalog`) contém IDs como `games`, `creative`, `learning`, `music`, `photography`, `nature`, `technology`, `animals`, `adventures`, `relax`, `create` e `fantasy`. A seleção visual atual usa um subconjunto de nove IDs: `music`, `photography`, `nature`, `technology`, `animals`, `learning`, `relax`, `create` e `fantasy`.

O mapeamento visual aprovado para as nove preferências apresentadas é: `music→category_music`, `photography→category_photography`, `nature→category_nature`, `animals→category_animal`, `learning→category_learn`, `technology→category_science`, `relax→category_appreciation`, `create→category_creativity` e `fantasy→category_games` (arte do dragão). Esses aliases existem somente na camada visual; não criam um ID `games` para a opção Fantasia nem alteram as afinidades do `SystemTagCatalog`. O estado selecionado é indicado pelo glow dourado compartilhado, sem depender apenas da cor.

O mapeamento visual está aprovado; qualquer alteração de ID, afinidade ou significado de domínio continua exigindo mudança explícita e testes em `SystemTagCatalog`.

A seleção de várias preferências não é limitada a três. Qualquer limite de tags para uma quest híbrida é regra de domínio explícita, não efeito colateral da UI.

## 3. Onboarding

- O nome do jardim é campo editável real. `name_bar.png` é moldura visual; foco, teclado, caracteres permitidos, validação e persistência continuam Compose/domínio.
- A validação de nome continua com `GardenOwnerName`.
- Marca/logotipo embutidos no background não devem ser redesenhados por cima.
- A etapa de nome na implementação corrente usa rolagem vertical e `imePadding`; a ação IME Done só avança se `GardenOwnerName.create(name.trim())` for bem-sucedido. Ainda falta confirmar foco, reposicionamento e acesso ao CTA no teclado real do aparelho.
- A seleção visual de tags permite rolagem e evita o antigo espaçador vertical proporcional grande; as nove opções e Continuar precisam ser verificadas em viewport compacta, especialmente com fonte ampliada.
- Implementação parcial de P6-C; o gate permanece aberto até CI verde do novo SHA e validação visual/funcional em aparelho.

## 4. Configurações e preferências

O tema e a escala de fonte pertencem ao store de preferências existente. `btn_theme_day/night` têm canvas comum 300×180; `btn_font_size.png` é skin. Estado selecionado, valor persistido e escala aplicada são Compose/preferências.

Backup só é apresentado como sucesso depois que os bytes são preparados e gravados no destino escolhido. Apoio ao criador só pode informar resultado quando há integração real ao destino externo. Uma ilustração de botão não é confirmação de sucesso.

A instalação do modelo só deve aparecer como opção funcional se callback, catálogo de pacote e estado de instalação estiverem conectados. Se não houver pacote configurado, mostrar estado honesto de indisponibilidade e manter o fallback; não alegar que o modelo está instalado.

## 5. Perfil

Nome do jardim, tags habilitadas e preferências são dados reais. `card_profile_day/night` e `card_profile_tags.png` são superfícies. Configurações é destino interno, não quarta aba global. A lista das nove tags exibidas é controlada pelo contrato/UI atual; assets ausentes ou não aprovados não devem inventar novas tags.

## 6. Jardim, coleção e descoberta

O Jardim tem três composições da mesma fonte governada: vazio, jardim em arte e coleção aberta. Dados vêm de `GardenUiState`/domínio; abrir coleção não cria flores, XP, raridade ou identidades.

- O estado vazio deve convidar a iniciar uma quest sem fingir coleta.
- O jardim em arte aparece quando o estado de domínio tem progresso pertinente.
- A coleção apresenta até nove posições da coleção atual. `card_flower_unknown_day/night` só representa estados nos quais a identidade deve permanecer escondida.
- As artes de flor desconhecida já incluem o símbolo de interrogação e inscrição de desconhecida; não os duplicar em Compose.
- Filtros: Todas, Coletadas, Pesquisadas e Desconhecidas. A projeção do filtro deriva do estado de descoberta; não introduzir novo estado apenas para a aparência. O filtro Pesquisadas precisa manter a semântica atual de REVEALED/COLLECTED conforme o domínio, sem confundir pesquisa com coleta.
- Filtros não podem cortar nomes; grade começa abaixo dos filtros e respeita o espaço restante.
- A navbar global não pode esconder itens da grade, detalhe de flor ou CTAs.

Correções de geometria, rolagem, filtros e safe areas estão em implementação em P6-C: a grade usa a altura restante do painel, os filtros mudam para duas colunas em largura compacta, o card desconhecido usa proporção retrato e as telas passam a reservar espaço inferior com base na navbar/insets. Isso não declara o gate concluído; CI do novo SHA e revisão no aparelho são obrigatórios.

## 7. Navbar e navegação

Uma única barra global tem três destinos: **Início | Jardim | Perfil**. `nav_bar_day/night` já contêm os ícones ilustrados de casa, flor e livro. O Compose não deve desenhar outro ícone/emoji sobre a mesma área. Deve manter áreas de toque, labels, semântica de seleção e ações reais.

Configurações e seleção de temas são fluxos internos; não criam abas globais duplicadas. A navegação não deve ter ownership duplicado por tela.

## 8. Texto, acessibilidade e imagem

- Texto mutável, localizável, funcional ou sensível à escala pertence ao Compose.
- O PNG pode conter lettering apenas quando faz parte decorativa intencional. Esse lettering não substitui label acessível nem dado variável.
- Contraste, escala de fonte, quebra de linha, rolagem e alvo de toque precisam funcionar em tema claro/escuro e escala ampliada.
- Texto sobre arte deve usar contraste adequado para o tema atual ou uma superfície/sombra controlada; não usar verde escuro sobre fundos noturnos escuros.
- Rótulos sobre botões ilustrados precisam de limites de linha explícitos e área de texto separada dos símbolos incorporados na arte.
- Seleção e estado não dependem somente da cor.
- A arte não pode interceptar interação nem substituir um campo editável.
- O valor persistido é autoridade do store/domínio; a UI só representa esse valor.

## 9. Invariantes de geração e verdade

- `QuestPlanner` seleciona categoria/ambiente respeitando modo e filtros.
- `QuestValidator` é a autoridade que promove um draft a Quest válida.
- Ausência/falha do modelo local pode recorrer ao compositor procedural, mas essa rota deve ser diagnosticável e honesta.
- O modo RANDOM não pode ficar deterministicamente preso à primeira categoria por falta de histórico.
- Uma quest inválida, sessão que falhou ou backup não gravado não pode produzir mensagem de sucesso falso.
- O estado canônico de jardim/XP/coleção não é duplicado em estado local de apresentação.

## 10. Gates e evidência

- `P6-A`: inventário/aliases e documentos reconciliados com o mapa Gradle.
- `P6-B`: composição, proporções e controles gráficos.
- `P6-C`: layouts responsivos, teclado, rolagem e safe areas.
- `P6-D`: contexto, variedade e fallback de quests.
- `P6-E`: distribuição segura do modelo, mapeamento semântico de categoria, fonte e acessibilidade.
- `P6-Final`: regressão de release ligada ao SHA testado.

**Estado atual (2026-10-15):** P6-B permanece aceito; P6-C e P6-E seguem abertos. A correção de `LargeFontLayoutTest.kt` e a extração de Ihlimia foram validadas pela [Android CI #38684219225](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38684219225) no checkpoint de código `f67ea6501dfe657b9c3f9f421413a98d5a84eb07`; a execução concluiu com `BUILD SUCCESSFUL`, 0 erros e 38 avisos de lint. A CI compila testes instrumentados, mas não os executa no aparelho. Falta validação visual/funcional real e registro da licença de incorporação/distribuição antes de fechar gates. Os gates são governados por `Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md`. Documento atualizado não significa runtime corrigido.


## 11. Evidência visual P6-E — revisão de 14/10/2026

**Status:** quatro screenshots de runtime fornecidos por Rafa apontaram problemas de texto/contraste. As imagens não vieram associadas ao SHA do APK; tratá-las como observação de sintomas, não como prova de que todos os aparelhos/escala reproduzem exatamente o mesmo resultado.

Achados:
- Home: métricas, nome do jardim e tagline eram posicionados por offsets verticais independentes; rótulos de Quest Aleatória/Quest de Jogo podiam aproximar-se de ícones embutidos.
- Jardim: o cabeçalho não reservava a largura restante e uma frase de progresso longa podia deixar “flores” isolado.
- Perfil: o nome e a descrição da placa ocupavam altura excessiva e subiam para a zona decorativa das folhas; a descrição longa precisa continuar alcançável acima da barra global.
- Configurações: títulos fixos verde-escuro tinham pouco contraste sobre o fundo noturno; texto marrom sobre botão verde tinha contraste fraco.

Correções iniciais publicadas nesta branch:
- O cartão Home usa um fluxo vertical compartilhado, em vez de offsets de texto concorrentes.
- Botões gráficos limitam rótulos a duas linhas por padrão; labels de tags ficam numa linha; GAME foi afastado do ícone incorporado.
- O cabeçalho do Jardim usa largura flexível e mantém XP, nível e contagem como nós semânticos separados.
- A placa do Perfil recebeu ajuste vertical, espaçamento e reserva inferior.
- Configurações usa cores de texto sensíveis ao tema e texto creme nas ações de modelo verdes.

**A validação permanece aberta.** É necessário obter CI ligado ao SHA exato e capturar o APK em escala 1.0 e 1.3, temas claro/escuro e viewport compacta. O workflow atual compila os testes instrumentados mas não os executa; compilação de AndroidTest não fecha o gate P6-C ou P6-E. Consulte `docs/evidence/p6-e-accessibility-audit-2026-10-14.md` para o plano completo.

### Histórico da fonte anterior — MV Boli

A decisão anterior de não embutir `mv-boli.ttf` continua válida para aquele arquivo: o ZIP não tinha licença, e a documentação Microsoft restringe redistribuição de fontes Windows em apps:
- https://learn.microsoft.com/en-us/typography/font-list/mv-boli
- https://learn.microsoft.com/en-us/typography/fonts/font-faq

Essa decisão histórica não bloqueia por si só a alternativa selecionada abaixo.

### Fonte de display atual — Ihlimia

- Fonte selecionada por Rafa: `docs/design/ihlimia-maisfontes.8b4f.zip`.
- O ZIP contém `ihlimia.ttf` e não inclui licença/README. Rafa identifica a fonte como de licença livre; o arquivo enviado, isoladamente, não comprova quais direitos de redistribuição e incorporação em APK são concedidos. Guardar a licença/autorização do autor ou da fonte oficial antes de release público.
- `app/build.gradle.kts` extrai somente `ihlimia.ttf` para `build/generated/p6-display-font-res/font/`; o ZIP não é empacotado como recurso.
- A família `PinhoDisplayFontFamily` é aplicada apenas a headings curtos do Início e Perfil (nome do jardim e título Perfil). O título do Jardim permanece na família anterior para priorizar legibilidade com nomes longos. Texto do corpo, quests geradas, métricas, rótulos de navegação e controles continuam nas famílias anteriores, para não trocar legibilidade por ornamentação.
- A fonte deve ser validada com nomes longos, escalas de fonte, temas claro/escuro e layouts compactos antes de fechar P6-E.


## 12. Follow-up P6-E — correção da CI e legibilidade (2026-10-15)

A execução [#38603677089, job #115988990661](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38603677089/job/115988990661) falhou em `:app:compileDebugAndroidTestKotlin`: o teste importava `assertExists` não disponível na versão Compose UI Test configurada e usava `onNodeWithText` sem import. `LargeFontLayoutTest.kt` foi alterado para usar `onAllNodesWithText`/`onAllNodesWithContentDescription` com `assertCountEquals`.

A legibilidade recebeu ajustes: as métricas do cartão Home passaram de 10sp para 11sp com line-height correspondente; o botão de backup usa rótulo 16sp, no máximo duas linhas. A fonte manuscrita não foi aplicada a botões/controles porque os textos funcionais precisam continuar fáceis de ler.

**Evidência CI:** a [Android CI #38684219225](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38684219225) passou no checkpoint de código `f67ea6501dfe657b9c3f9f421413a98d5a84eb07`, incluindo `syncDisplayFont` e compilação de AndroidTest. O heading do Jardim permanece na família legível anterior. A validação no aparelho e o registro da licença de Ihlimia para incorporação/distribuição em app continuam pendentes. P6-C e P6-E permanecem abertos.
