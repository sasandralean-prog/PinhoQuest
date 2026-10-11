# P6 — Contrato Oficial de Sprints, Implementação e Transição

**Projeto:** PinhoQuest  
**Branch de trabalho:** feature/p6-total-ui-refactor  
**Revisão do contrato:** 2026-10-10  
**HEAD de código revisado antes desta atualização documental:** 42426c2f6667c86c3f7c70f99b50ca20543d4f64  
**Estado deste documento:** contrato de execução e critérios de gate; não é declaração de que todos os gates estejam fechados.

> Esta revisão substitui as distribuições de sprint e os estados técnicos contraditórios registrados nas notas históricas deste arquivo. Histórico continua útil como contexto, mas o estado vigente deve ser lido nas seções 1–5 e no quadro de sprints abaixo. Código, CI e evidências reproduzíveis prevalecem sobre README antigo, relato histórico, intenção e screenshot isolado.

## 1. Objetivo e limites de P6

P6 deve entregar uma interface coesa, acessível e responsiva para PinhoQuest, preservando as autoridades de estado e de domínio já existentes. A interface deve usar corretamente os assets canônicos, mostrar texto dinâmico sem colisões, permitir completar os fluxos principais e apresentar quests variadas com fallback honesto quando o modelo local não está instalado ou não pode ser usado.

P6 não é somente uma troca de PNGs. O trabalho inclui o encaixe real entre assets, Compose, estados, navegação e geração de quests.

### Incluído

- Alinhar a documentação operacional P6 ao código atual e às evidências disponíveis.
- Corrigir a composição de cards, botões e navbar que duplica arte ou sobrepõe texto.
- Corrigir filtros, cartões da coleção, safe areas e layout de onboarding com teclado.
- Corrigir a seleção de categoria e a diversidade do compositor procedural.
- Definir e implementar um caminho seguro e verificável para disponibilizar LiteRT-LM, caso continue sendo requisito de produto V1.
- Completar a associação visual das categorias somente após validar IDs semânticos no catálogo de domínio.
- Integrar MS Boli somente após verificar procedência e licença; manter o texto funcional legível e responsivo.
- Testes automatizados, revisão em aparelho, screenshots comparativas, build e atualização do estado documental.

### Fora de escopo, salvo decisão explícita

- Redesenhar as artes canônicas sem aprovação do proprietário.
- Colocar um modelo binário grande no Git apenas por conveniência.
- Introduzir uma segunda autoridade de navegação, estado, persistência, recompensas ou geração de quests.
- Corrigir a aparência com dados, nomes, dimensões ou branches específicos do aparelho.
- Associar uma ilustração a uma tag apenas porque os nomes parecem semelhantes.
- Declarar P6 concluído somente porque compila, ou com base apenas nas screenshots.
- Tratar o repositório de imagens externo/desatualizado como fonte de verdade.
- Tratar o README como fonte atual do estado de implementação sem cruzar com a branch P6.

## 2. Hierarquia de autoridade

1. Código, testes, assets e configuração de build publicados na branch de referência.
2. Evidência de CI ligada ao SHA exato, logs de execução e resultados dos testes.
3. Contratos de produto e arquitetura aprovados, desde que não contradigam o comportamento atual confirmado.
4. Screenshots reais do APK, usadas como evidência de sintomas visuais/funcionais e não como prova isolada de causa.
5. Catálogo e registro gráfico, que devem ser reconciliados com código e build.
6. README, apontamentos antigos e notas de sprint: fontes auxiliares sujeitas a atualização.

Documentos que devem permanecer sincronizados:
- Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md — plano, gates, estado e evidências.
- docs/design/P6_UI_UX_INTERACTION_CONTRACT.md — semântica das ações e dos estados.
- docs/design/ASSET_CATALOG.md — origem, classificação, dimensões e aliases.
- docs/design/CANONICAL_GRAPHICS.md — papéis visuais e composição por tela.
- app/build.gradle.kts — mapeamento executável de assets.
- Testes e artefatos CI — evidência das verificações executadas.

## 3. Snapshot confirmado e trabalho já realizado

### 3.1 Snapshot desta revisão

- **Branch:** `feature/p6-total-ui-refactor`.
- **HEAD de código revisado antes desta atualização:** `31e603392d89734102e7b89a09144efc9886c494`.
- **P6-B:** concluído nesta rodada por aceitação provisória do proprietário. A execução [Android CI #38081485398](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38081485398) concluiu com `success` no SHA `ee42ea2748e3b8e79d46d9d37983153619b83d8f`. Pequenos polimentos estéticos restantes não são descritos como resolvidos nem bloqueiam esta aceitação.
- **P6-C:** primeira implementação publicada nos commits `1df9957d02f6002985233a577187089685b7e2ee`, `73f380203b4279f164dc48fce0bfa7c6cd5954f3`, `8274be1342ab423eb071366390e64a783fe55043`, `19c083a83bc9727663ff9df780a64e4fff4af5b0`, teste da ação IME Done em `dd0d3115bf03c0be75fb4bcbbf0b52856e2d9c41` e import `dp` do teste compacto em `42426c2f6667c86c3f7c70f99b50ca20543d4f64`.
- A execução [Android CI #38093315567](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38093315567) terminou `failure` no SHA `31e603392d89734102e7b89a09144efc9886c494`: `:app:assembleDebug` e `:app:compileDebugKotlin` passaram, mas `:app:compileDebugAndroidTestKotlin` falhou porque faltava importar `dp` em `GardenFlowTest.kt`. O import foi adicionado no SHA `42426c2f6667c86c3f7c70f99b50ca20543d4f64`. A execução [Android CI #38093650095](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38093650095) estava `in_progress` para esse SHA quando o snapshot foi feito; P6-C continua sem declaração de CI verde.
- O workflow compila testes instrumentados mas não os executa em aparelho/emulador. Evidência real do IME, escalas ampliadas e safe areas em dispositivo continua necessária para fechar P6-C.
- A CI verde do P6-B não substitui a CI do SHA atual nem comprova a validação visual de runtime.

### 3.2 Assets publicados e mapeamento de build

Os assets canônicos do APK vêm de docs/design/ na branch P6. O repositório PinhoQuestImagens não é fonte da verdade nesta etapa.

A inspeção do tree e dos arquivos confirmou estes diretórios:
- docs/design/Background/
- docs/design/Button/
- docs/design/Card/
- docs/design/Navbar/

O app/build.gradle.kts define mapeamentos canônicos, inclui os dois card_flower_unknown em canonicalCardAssets e associa os recursos aos diretórios atuais. As imagens category_* continuam fisicamente em docs/design/Button/ por decisão/estrutura histórica do pacote; isso não as transforma semanticamente em botões nem autoriza renomeação de IDs. O verificador deve continuar cobrindo os aliases e arquivos esperados. Há entradas auxiliares chamadas Null nos diretórios de arte; não devem ser tratadas como assets.

Exemplos de hashes e dimensões conferidos diretamente na branch:
| Asset | Dimensão publicada | Resultado da comparação |
|---|---:|---|
| Button/btn_garden_filter_all.png | 120 × 48 px | hash do blob Git confirmado |
| Button/btn_theme_day.png | 300 × 180 px | hash do blob Git confirmado |
| Card/card_home_day.png | 612 × 292 px | hash do blob Git confirmado |
| Card/card_home_night.png | 612 × 292 px | hash do blob Git confirmado |
| Card/card_garden_empty_day.png | 392 × 316 px | hash do blob Git confirmado |
| Card/card_garden_empty_night.png | 392 × 316 px | hash do blob Git confirmado |
| Card/card_profile_night.png | 996 × 461 px | hash do blob Git confirmado |
| Button/category_animal.png | 160 × 120 px | hash do blob Git confirmado |
| Button/category_creativity.png | 160 × 120 px | hash do blob Git confirmado |
| Card/card_flower_unknown_day.png | 174 × 255 px | asset preservado; hash do blob Git confirmado |

A padronização dos backgrounds para 1080 × 1920 px (9:16) também foi registrada e os arquivos estão publicados. Os exemplos acima não devem ser lidos como inventário de todos os 47 aliases nem como declaração de que cada tela já consome a proporção correta.

### 3.3 Trabalho concluído ou já disponível

- O inventário visual e a estrutura de diretórios foram publicados na branch.
- O mapeamento Gradle atual já inclui os cards de flor desconhecida em canonicalCardAssets; notas históricas dizendo que o mapa ainda os coloca em canonicalButtonAssets estão desatualizadas.
- Existem componentes compartilhados de Compose para fundo, botão gráfico, painel em pergaminho, tag visual e navbar.
- O estado de tema e escala de fonte tem persistência já conectada ao store de preferências.
- A grade e a coleção já usam dados de GardenUiState; não se deve criar uma segunda fonte de estado para “consertar” visualmente os cards.
- O fluxo de geração e validação de quests, o compositor procedural e a infraestrutura LiteRT-LM estão presentes no código.
- As verificações e o APK não foram gerados por esta atualização documental. A conclusão do CI consultado é uma evidência remota anterior, não uma nova execução feita por este commit de documentação.

### 3.4 Defeitos observados nas screenshots e confirmados por inspeção do código

1. GardenSummaryCard desenha emojis de flor/estrela sobre card_home_day/night, cujas artes já contêm motivos decorativos; usa também a proporção antiga 1062/450 no card noturno em vez de 612/292.
2. Os botões gráficos têm vários parâmetros de proporção antigos, e labelStartFraction de aproximadamente 0.14–0.15 pode deixar a folha incorporada na arte por cima do início do texto.
3. PinhoBottomNavigation desenha caracteres/emoji de casa, flor e livro sobre nav_bar_day/night, que já contêm os três ícones.
4. GardenFlowerCard desenha “?” e textos sobre card_flower_unknown, que já contém símbolo e texto decorativos; a altura de 160 dp não corresponde à proporção do asset.
5. A LazyVerticalGrid usa fillMaxSize dentro do painel com filtros; os filtros têm larguras iguais e labels longos; o conteúdo inferior disputa espaço com a navbar sobreposta.
6. OnboardingScreen usa deslocamentos verticais relativos à altura e não tem um contrato suficiente para manter o CTA visível com o IME aberto nem para garantir acesso a todas as categorias.
7. SettingsScreen usa proporções antigas 295/168 e 290/172 para assets finais com canvas 300/180. EmptyGardenState usa 392/282 para card_garden_empty_day, cujo canvas final é 392/316.
8. Somente cinco tags visuais têm mapeamento explícito em tagGraphicAsset; quatro usam a superfície neutra. Os outros cinco assets de categoria não devem ser associados por aproximação semântica.
9. QuestPlanner.selectNovelCategory seleciona a primeira categoria candidata ausente em recentCategories. O contexto de produção observado preenche afinidades mas não fornece histórico recente; por isso, o caminho aleatório pode escolher CODING repetidamente.
10. ProceduralComposer mapeia cada categoria para uma composição determinística fixa; CODING produz “Frankenstein Digital”. Sem modelo ativo, PinhoQuestAppGraph usa ProceduralComposer.
11. A distribuição CR-7.4 agora usa uma GitHub Release versionada (`cr74-semantic-isolation-v1`), com `model.litertlm` de 284692656 bytes e SHA-256 `e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb`; `Cr74SemanticIsolationModelCatalog` está ligado ao coordenador e a Configurações exige consentimento explícito. O modelo fica intencionalmente fora do APK padrão. A execução de inferência no HEAD atual ainda precisa de evidência nova.

Essas observações foram reproduzidas pela inspeção da branch e comparadas com as screenshots. Qualquer hipótese adicional sobre densidade, teclado, tema ou comportamento em outras telas precisa de teste, não deve ser registrada como fato até ser reproduzida.

### 3.5 Implementação corrente após P6-B

Os itens abaixo registram a implementação publicada, não o fechamento do gate:
- **C-01:** o card desconhecido usa proporção retrato `174/255` e preserva o lettering que já existe no PNG.
- **C-02/C-03:** a grade recebe `weight(1f)` dentro do painel rolável, e os filtros completos usam uma linha em larguras ≥520 dp ou duas colunas/duas linhas em telas mais estreitas. Foi adicionado teste Compose instrumentado para 360×720 dp que verifica os quatro filtros e a operação de Desconhecidas.
- **C-04:** `PinhoQuestNav` fornece `LocalPinhoBottomNavigationInset` calculado com a proporção da navbar atual e `WindowInsets.navigationBars`; Jardim, perfil, configurações e telas de quest consomem essa reserva em vez de alturas mágicas locais.
- **C-05/C-06:** a etapa de nome é rolável e usa `imePadding`; o campo tem semântica acessível e ação Done condicional à validação. A página de tags reduz o espaçador superior e permanece rolável. Falta confirmar o comportamento com IME real no aparelho.
- **C-07:** conteúdo Home/quest gerada/ativa reserva espaço inferior calculado para não ficar sob a navbar; textos longos continuam aguardando regressão visual/instrumentada.
- A execução #38093315567 mostra que o código de produção compilou e o APK debug foi montado, mas a compilação de AndroidTest falhou por um import `dp` ausente em `GardenFlowTest.kt`. Isso foi corrigido em `42426c2f6667c86c3f7c70f99b50ca20543d4f64`; o novo CI #38093650095 ainda precisa concluir com sucesso.

## 4. Priorização e política de gate

- **P1 — bloqueia uso confiável ou um fluxo principal:** ação encoberta, informação essencial ilegível, seleção que não funciona, conteúdo inacessível, repetição que contradiz o modo de geração prometido, ou requisito de produto V1 sem caminho de execução.
- **P2 — bloqueia acabamento consistente/acessível:** inconsistência visual, suporte insuficiente a escala de fonte, mapeamento de arte ainda pendente, tipografia de identidade ou pequenas divergências entre telas.
- Um P1 não pode ser rebaixado para deixar um gate verde.
- Um P2 pode ser explicitamente adiado somente com escopo, impacto, evidência e aprovação registrados; não pode ser descrito como concluído.
- “PASS” exige evidência associada ao commit verificado. “PASS CI” não significa “PASS visual”. “BLOCKED” exige identificar uma dependência real e o próximo passo que a remove.

## 5. Plano oficial por sprints

A sequência abaixo substitui cronogramas históricos com apenas três marcos. P6-A consolida a base já publicada e limpa as divergências documentais; P6-B a P6-E implementam as correções; P6-Final fecha integração e release. As datas antigas são apenas contexto e não devem ser tratadas como compromisso atual.

### P6-A — Baseline canônico e reconciliação de evidências

**Objetivo:** fixar a fonte de verdade, confirmar os mapeamentos de assets e fazer com que documentos operacionais parem de descrever um estado anterior da branch.

**Estado-base:** estrutura de assets e mapas Gradle presentes; CI remoto no SHA de referência concluído com sucesso. O estado visual observado ainda contém defeitos, portanto isso não fecha P6-B/C.

**Itens**
- **A-01 — Snapshot reproduzível:** manter branch, SHA, execução CI, APK e data associados em toda revisão de gate.
- **A-02 — Catálogo coerente:** corrigir ASSET_CATALOG e CANONICAL_GRAPHICS onde ainda afirmam que cards de flor estão no grupo Gradle errado ou que os aliases não foram publicados. Preservar os diretórios físicos e a classificação semântica explícita.
- **A-03 — Contratos sincronizados:** atualizar este protocolo e reconciliar P6_UI_UX_INTERACTION_CONTRACT com as regras de arte não autoritativa, dimensões finais e papéis reais de navbar/cards.
- **A-04 — Baseline visual:** registrar os nomes e sintomas das dez capturas fornecidas pelo proprietário em `docs/evidence/p6-a-2026-10-10-baseline.md`; para regressão reproduzível, capturar depois um APK/commit identificado e registrar resolução, tema, escala e estado de dados. As imagens originais ainda não estão armazenadas no repositório.
- **A-05 — Auditar mapeamento:** executar o verificador de assets no checkout atual e confirmar que nenhum arquivo auxiliar Null entra como recurso e que os aliases não colidem.

**Arquivos principais:** app/build.gradle.kts (somente se a execução demonstrar divergência), docs/design/ASSET_CATALOG.md, docs/design/CANONICAL_GRAPHICS.md, docs/design/P6_UI_UX_INTERACTION_CONTRACT.md, este protocolo e pasta de evidências.

**Critérios de aceite**
- O catálogo e o mapa Gradle descrevem os mesmos diretórios, arquivos e aliases.
- Os dois cards de flor desconhecida estão classificados e mapeados como cards.
- A documentação não afirma que o repositório de imagens externo é a fonte da verdade.
- A execução de :app:verifyCanonicalUiAssets registra PASS no SHA revisado.
- O registro identifica as dez capturas baseline recebidas (Home, onboarding com/sem teclado, tags, jardim, coleção, perfil, configurações e quest); para comparar mudanças futuras, as capturas devem ser geradas/armazenadas com SHA de APK e condições de teste conhecidos.
- O protocolo distingue PASS de build e PASS de runtime visual.

**Testes/evidências:** verificador Gradle, assembleDebug, lista dos aliases gerados, resultado de CI e baseline de screenshots.

**Risco/rollback:** baixo. Não mover assets nem renomear arquivos sem mudança coordenada de Gradle, catálogo, consumidores e testes.

**Gate P6-A:** a configuração de assets e os 47 aliases foram reconciliados documentalmente; o CI #159 passou no SHA de código base `cc385fac097112d76e5e5381b8ac9040354c280f`. O resultado da CI para o commit documental que contém esta revisão ainda precisa ser consultado. O baseline sintomático recebido foi indexado, mas seus binários e metadados exatos de APK não estão armazenados no repositório. Não declarar P6-B/C visual PASS por isso.

### P6-B — Composição visual e componentes compartilhados (P1)

**Objetivo:** eliminar sobreposições causadas pelo código compartilhado e usar as proporções finais dos assets, sem redesenhar as artes.

**Estado em 10/10/2026: CONCLUÍDO NESTA RODADA.** O proprietário aceitou o resultado visual atual, reconhecendo que ainda cabe polimento futuro. [Android CI #38081485398](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38081485398) passou no SHA `ee42ea2748e3b8e79d46d9d37983153619b83d8f`. Este status não fecha P6-C nem P6-Final, e não afirma que todas as capturas de release já existam.

**Itens**
- **B-01 — HomeSummary:** corrigir GardenSummaryCard para proporção 612/292 nos dois temas; eliminar emojis duplicados; reservar área segura para os textos; preservar o nome, contagem e XP como dados reais; garantir que o texto não invada a árvore decorativa.
- **B-02 — Botão gráfico:** revisar PinhoGraphicButton para que imagem mantenha proporção natural e que os labels usem área útil compatível com a arte. Remover dependência de um labelStartFraction genérico que não serve para todos os PNGs; registrar ajustes por família de asset quando justificados pelo espaço visual.
- **B-03 — Navbar:** manter nav_bar_day/night como superfície ilustrada; remover os caracteres de ícone duplicados. Os três destinos continuam controles Compose verdadeiros, com click target, semântica, seleção e rótulo acessível.
- **B-04 — Temas:** atualizar SettingsScreen para proporção 300/180 nos dois botões de tema, sem distorcer imagem nem mover labels para cima da ilustração do sol/lua.
- **B-05 — Jardim vazio:** usar a proporção final 392/316 para card_garden_empty_day/night quando o par for apresentado em canvas comum; ajustar o conteúdo restante sem sobreposição.
- **B-06 — Voltar:** validar PinhoBackButton quanto a tamanho visual, proporção e alvo de toque mínimo de 48 dp. Preservar o PNG 66×75; não ampliar o arquivo raster artificialmente. Ajustar a geometria Compose se a captura e o teste de toque comprovarem insuficiência.

**Arquivos principais:** PinhoQuestVisuals.kt, QuestScreen.kt, SettingsScreen.kt, GardenScreen.kt.

**Critérios de aceite**
- Nenhum símbolo duplicado na Home ou navbar.
- Home day/night usa o mesmo canvas de card e a mesma área semântica de texto.
- Texto de todo botão principal fica separado da folha/ícone desenhado no PNG e permanece legível em scale 1.0 e 1.3.
- Todas as imagens preservam aspecto natural; nenhum FillBounds pode deformar um asset sem decisão visual expressa.
- Controles permanecem clicáveis e acessíveis por Compose, e não são substituídos por imagem estática.
- Capturas antes/depois nas duas variantes de tema não mostram sobreposições.

**Testes:** testes Compose de componentes partilhados, verificação de proporção no catálogo, screenshots, semântica/click test da navbar e botões.

**Dependências:** P6-A baseline e aliases estáveis.

**Risco/rollback:** médio; um ajuste de área de texto pode afetar várias telas. Alterar uma família de componente por vez, manter screenshots de referência e reverter por componente se a semântica de clique for afetada.

### P6-C — Jardim, coleção, onboarding e safe areas (P1)

**Objetivo:** tornar todos os controles e conteúdos essenciais alcançáveis em telas compactas, com teclado aberto e escala de fonte aumentada.

**Estado em 10/10/2026: EM ANDAMENTO; gate aberto.** A grade agora usa o espaço vertical restante do pergaminho, os filtros preservam os quatro rótulos completos e se distribuem em duas linhas em largura compacta, cards desconhecidos usam proporção retrato e a navbar compartilha uma reserva inferior calculada por proporção e inset do sistema. O onboarding de nome usa scroll + `imePadding` e valida a ação IME Done via `GardenOwnerName`; um teste instrumentado foi adicionado para proteger essa transição. O SHA `31e603392d89734102e7b89a09144efc9886c494` compilou o app, mas falhou na compilação dos testes pela importação `dp`; a correção está em `42426c2f6667c86c3f7c70f99b50ca20543d4f64`. A CI [#38093650095](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38093650095) estava em andamento; a validação visual/funcional em aparelho ainda não foi feita.

**Itens**
- **C-01 — Card de flor desconhecida:** usar proporção derivada do asset 174×255; não redesenhar o ponto de interrogação nem o “???” já incorporados; manter no Compose apenas os dados que o asset não representa (estado real, raridade/nome quando revelados). Definir explicitamente os estados hidden, hinted, revealed e collected.
- **C-02 — Grade da coleção:** depois dos filtros, a LazyVerticalGrid deve usar somente o espaço remanescente (por exemplo, weight dentro de um contêiner de altura limitada), sem ocupar o cabeçalho inteiro. A rolagem deve pertencer a um só contêiner por eixo para evitar conflitos.
- **C-03 — Filtros:** quatro filtros devem caber sem cortar labels. Testar labels completas (“Todas”, “Coletadas”, “Pesquisadas”, “Desconhecidas”) em largura compacta. Se não houver largura, usar uma solução responsiva aprovada (quebra de linha ou região horizontal rolável); não esconder sufixos para forçar o layout.
- **C-04 — Navbar e safe area:** reservar no layout raiz o espaço que a barra global ocupa, com insets reais. Cards e ações devem continuar acessíveis atrás/ao redor da barra; não usar alturas mágicas específicas do aparelho.
- **C-05 — Onboarding nome:** aplicar tratamento IME, rolagem/foco e reposicionamento por constraints de forma que o campo e o CTA Confirmar sejam alcançáveis com o teclado aberto. A validação de nome continua pertencendo a GardenOwnerName.
- **C-06 — Onboarding tags:** remover espaçadores proporcionais que consomem a área de conteúdo sem necessidade. Todas as categorias e o CTA Continuar devem permanecer alcançáveis; a área que rola e a ação inferior precisam ter responsabilidades claras.
- **C-07 — Layout de quest:** descrições e objetivos longos devem rolar dentro da área de conteúdo sem serem encobertos por navbar, CTA ou barras de sistema. Evitar fixar altura de parchment de acordo com um exemplo único.

**Arquivos principais:** GardenScreen.kt, FlowerDetailScreen.kt, OnboardingScreen.kt, QuestScreen.kt, PinhoQuestNav.kt e componentes de layout compartilhados.

**Critérios de aceite**
- Nenhum card é coberto pela navbar nem invade a região dos filtros.
- Cards desconhecidos não têm “?” ou “???” duplicados.
- Os quatro filtros continuam legíveis e operáveis em larguras compactas.
- Com teclado aberto, o usuário consegue selecionar o campo, inserir nome válido e alcançar Confirmar sem fechar o teclado à força.
- As nove opções de onboarding e o CTA Continuar podem ser acessados por rolagem em tela compacta.
- Quest com título/descrição/objetivos longos permite alcançar o último objetivo e ambos os CTAs.
- Testes com fonte 1.0 e 1.3, tema dia e noite, sem overflow, corte nem alvo encoberto.

**Testes:** Compose UI tests para IME/CTA, semântica dos filtros, rolagem da coleção, card states e interação por clique; capturas em tamanho compacto e tela de referência.

**Dependências:** P6-B entrega componentes e proporções previsíveis.

**Risco/rollback:** médio/alto porque mexe em áreas roláveis e teclado. Usar testes de fluxo antes de alterar a hierarquia raiz; evitar aninhar duas listas verticais roláveis sem limites claros.

### P6-D — Geração de quests, contexto e fallback diverso (P1)

**Objetivo:** fazer o modo aleatório variar de verdade, preservar as restrições de geração e garantir que a saída seja válida, mesmo sem modelo local.

**Itens**
- **D-01 — Histórico real:** rastrear de onde pode vir recentCategories no contrato de core. Alimentar o planner somente com histórico autorizado e disponível; não fabricar uma lista em estado local da UI.
- **D-02 — Seleção não enviesada:** corrigir selectNovelCategory para não escolher sempre o primeiro candidato. Manter os filtros hard como autoridade; quando todos os candidatos foram usados, fazer rotação determinística documentada ou seleção pseudoaleatória testável, sem ignorar restrições válidas.
- **D-03 — Diversidade procedural:** substituir o mapeamento único por categoria por um conjunto finito de composições úteis com variação de ângulo, ação, duração e objetivo, sem combinações sem sentido nem fixtures específicas para contornar testes.
- **D-04 — Pipeline observável:** expor diagnóstico não sensível sobre estado do modelo, caminho selecionado (LLM/procedural), categoria, resultado de validação e razão de fallback. Não registrar conteúdo pessoal do usuário ou prompt privado em logs de produção.
- **D-05 — Contrato de saída:** continuar passando toda saída por QuestValidator. Saída LLM inválida, inferência falha ou modelo indisponível nunca deve produzir falso sucesso; fallback válido é permitido e deve ser diagnosticável.
- **D-06 — Testes de variedade:** testar categorias candidatas, categorias recentes vazias/parciais/completas, filtros, modo RANDOM e modo GAME. Testar uma série reproduzível de 20 gerações sob seed/controlador determinístico de teste. O objetivo não é “nunca repetir qualquer texto”, e sim provar que escolhas não ficam presas ao primeiro item e que a variedade programada é alcançável.
- **D-07 — Estado persistido:** verificar que histórico usado para variar geração vem do repositório/serviço de domínio adequado, e só é atualizado conforme evento válido definido pelo contrato. Não mudar a fonte de verdade em memória da tela.

**Arquivos principais:** QuestPlanner.kt, QuestContext/QuestContextProvider, QuestSessionService e eventuais ports de histórico, ComposerPort.kt, QuestValidator.kt, PinhoQuestAppGraph.kt e testes de quest-core.

**Critérios de aceite**
- Com contexto vazio, RANDOM não fica deterministamente preso em CODING.
- Com categorias recentes parciais, há rotação/diversidade sem violar filtros explícitos.
- GAME nunca se converte silenciosamente em NORMAL ou em uma quest de outra categoria quando não existe candidato válido.
- Todas as saídas passam por QuestValidator; falha de geração aparece como estado de erro ou fallback válido claramente identificável.
- Suite de 20 gerações controladas cobre mais de uma categoria e demonstra ausência de viés fixo para o primeiro item; teste usa seed ou fonte de escolha injetável, não expectativa probabilística instável.
- Nenhum log de produção contém texto pessoal, prompts completos ou dados sensíveis.

**Testes:** quest-core unit tests, testes do contexto e serviço, testes de integração para falha/retorno de inferência e confirmação de que histórico é atualizado somente por caminho governado.

**Dependências:** P6-A permite reproduzir a versão; nenhum ajuste de UI substitui esta correção.

**Risco/rollback:** alto para regra de domínio. Mudanças pequenas e cobertas por testes; não alterar validações para fazer a geração “passar”. Se a nova variedade produzir drafts inválidos, corrigir o compositor/planejador, não enfraquecer QuestValidator.

### P6-E — Distribuição do modelo, categorias semânticas e tipografia (P1/P2)

**Objetivo:** resolver a ausência do modelo local sem incluir um binário grande no Git por padrão; completar a consistência visual e de acessibilidade.

**Itens**
- **E-01 — Decisão formal de distribuição LiteRT-LM (P1, bloqueia o requisito LLM V1):** registrar se o modelo continuará requisito de produto. Como está ausente do APK e não há catálogo de pacote configurado, escolher uma distribuição externa imutável ou outro mecanismo aprovado. Não fingir que o modelo está ativo.
- **E-02 — Manifesto confiável:** especificar modelId, versão, runtimeFormat, tamanho máximo esperado, bytes, SHA-256, licença, URL oficial e backends suportados. A origem do manifesto precisa ser confiável; SHA-256 sem uma origem íntegra/autenticada não basta contra adulteração. Preferir manifesto assinado ou canal autenticado e versionado, com estratégia explícita de rotação.
- **E-03 — Instalação segura:** ligar ModelPackageCatalog e ModelInstallCoordinator ao fluxo real de Settings/Profile, usando WorkManager existente onde apropriado. Validar conectividade, tamanho e hash; baixar para .part; rejeitar truncado; promover atomicamente após validação; preservar modelo anterior até novo modelo válido; mostrar progresso, cancelamento/retry e erros compreensíveis. Não passar dados privados do jardim para um servidor de inferência: geração local é local.
- **E-04 — UX honesta sem modelo:** oferecer fallback procedural sem quebrar o loop principal. Mostrar que o cérebro criativo não está instalado e permitir instalação explícita com tamanho/licença/fonte antes de iniciar o download. Nunca comunicar “LLM ativo” sem modelo instalado e runtime carregado. Quando a fonte oficial do artefato estiver indefinida, manter o catálogo desconfigurado e registrar esse gate como BLOCKED; não inventar URL ou modelo.
- **E-05 — Mapeamento visual de categorias (P2):** comparar os nove assets com os IDs de domínio de SystemTagCatalog e documentar associação aprovada. Não mapear appreciation→affection, learn→learning, science→technology nem animal→animals apenas por semelhança lexical. Se não houver arte semanticamente adequada, manter a superfície neutra intencional, com estilo consistente e sem parecer um asset quebrado.
- **E-06 — MS Boli (P2):** validar procedência, arquivo e licença antes de adicionar a fonte em res/font. Usar apenas nos papéis de display de quests aprovados. Texto funcional, labels, métricas e acessibilidade permanecem legíveis; uma fonte manuscrita não pode substituir layout responsivo.
- **E-07 — Escala de fonte e contraste (P2):** testar densidade e escala do Compose em múltiplos estados; corrigir componentes fixos. Validar contraste sobre arte e pergaminho; os estados não podem depender apenas da cor.

**Arquivos principais:** ModelInstallCoordinator.kt, ModelDownloadWorker.kt, ModelDownloadTransport.kt, UnconfiguredModelPackageCatalog, PinhoQuestAppGraph.kt, SettingsScreen.kt, PinhoQuestNav.kt, PinhoQuestVisuals.kt, catálogo de domínio, MainActivity.kt, recursos de fonte e testes respectivos.

**Critérios de aceite**
- A decisão de distribuição do modelo está documentada; o P6-Final não fecha com “LLM disponível” como alegação se o modelo não puder ser instalado/ativado.
- Instalação verifica manifesto confiável, tamanho e hash antes de ativar; instalação parcial nunca substitui modelo ativo.
- Falhas de rede, digest, arquivo e runtime deixam o app utilizável em fallback e mostram estado verdadeiro.
- Não existe falso positivo de “modelo instalado” quando o catálogo retorna null ou o runtime não carrega.
- Os nove assets têm decisão semântica documentada; nenhum nome de imagem cria automaticamente uma tag.
- MS Boli só entra após licença/procedência verificadas, e o app não depende dela para layout correto.
- Fonte 1.0 e 1.3, temas claro/escuro e strings compridas não causam clipping nem perdem o acesso ao CTA.

**Testes:** testes unitários do catálogo e coordenador de instalação, downloads interrompidos, hash incorreto, versão inválida, troca atômica, falha do runtime, fallback procedural, testes de fonte/contraste e capturas.

**Dependências:** P6-D estabiliza o contrato de composição/fallback. E-01 precisa de decisão de produto e pacote real confiável para uma implementação completa.

**Risco/rollback:** alto para distribuição de modelo e privacidade. Nunca ativar artefato só por URL; não sobrescrever versão válida antes de validar a nova; se distribuição confiável não estiver disponível, manter fallback funcional e gate de modelo explicitamente BLOCKED, sem fingir conclusão.

### P6-Final — Regressão integrada, release e fechamento de gates

**Objetivo:** provar que a experiência completa funciona no APK produzido a partir de um SHA conhecido.

**Itens**
- **F-01 — QA de fluxos completos:** onboarding novo com teclado; conclusão do nome e das tags; Home; sorteio normal/aleatório/game; início, abandono e conclusão de quest; mudança de tema; escala de fonte; jardim vazio, arte, coleção, filtros, descoberta de flor, perfil e backup.
- **F-02 — Matriz visual:** capturas de Home, onboarding, Quest, Jardim/coleção, Perfil, Configurações e navbar em temas claro/escuro. Incluir escala 1.0 e 1.3 e pelo menos uma condição de tela compacta.
- **F-03 — Acessibilidade:** verificar áreas clicáveis de pelo menos 48 dp, descrições, estados selecionados, navegação, foco de teclado e contraste; ações importantes não podem depender de imagem semântica.
- **F-04 — Modelo/fallback:** comprovar caminho sem modelo e, caso E-01 esteja concluído, instalação/carregamento real do modelo. Capturar diagnóstico redigido sem dados pessoais.
- **F-05 — CI e build:** executar tarefas de assets, build debug/release, testes unitários e instrumentados disponíveis. Não declarar uma tarefa executada sem log/resultado.
- **F-06 — Documentação de release:** atualizar protocolo, contratos visuais, catálogo e estado corrente com SHA, link da execução CI, resumo de testes, capturas e defeitos remanescentes.
- **F-07 — Integridade da branch:** garantir que mudanças são incrementais, revisáveis e não alteram main diretamente. Toda mudança de documentação deve corresponder ao código real; nenhuma pendência pode permanecer descrita como resolvida sem evidência.

**Critérios de aceite**
- Todas as tarefas P1 concluídas e com evidência reproduzível.
- Todos os P2 concluídos ou explicitamente aceitos como deferred pelo proprietário, com justificativa e impacto documentados.
- Build de release termina com sucesso a partir do SHA final e os testes relevantes passam.
- O verificador canônico de assets passa no SHA final.
- Nenhuma screenshot da matriz contém texto/ícone duplicado, CTA encoberto, clipping de texto essencial ou card cortado.
- O fluxo de quest mantém autoridade única de domínio e a saída inválida não pode se tornar sucesso.
- Estado de modelo/LLM é verdadeiro e verificável no APK.
- Contratos e estado do projeto descrevem o artefato que foi realmente testado.

**Dependências:** P6-A a P6-E. Um item P1 bloqueado, uma fonte de modelo sem confiança ou uma validação visual essencial ausente impede PASS final.

**Risco/rollback:** alto se houver integração em massa. Integrar por commit pequeno, guardar o último APK validado, associar cada evidência ao SHA e reverter a mudança isolada quando uma regressão é introduzida.

## 6. Matriz de priorização por classe

| ID | Prioridade | Problema/resultado requerido | Sprint | Gate |
|---|---|---|---|---|
| UI-01 | P1 | Remover ícones/emoji duplicados no card Home; texto respeita área ilustrada | P6-B | B |
| UI-02 | P1 | Navbar deixa de redesenhar ícones já existentes no PNG; destinos Compose continuam acessíveis | P6-B | B |
| UI-03 | P1 | Botões principais não têm texto sob a folha/ícone; proporção correta | P6-B | B |
| GDN-01 | P1 | Cards desconhecidos não duplicam símbolo/rótulo e respeitam proporção | P6-C | C |
| GDN-02 | P1 | Grade usa apenas espaço restante; navbar não cobre cards | P6-C | C |
| GDN-03 | P1 | Filtros mostram rótulos completos sem truncamento | P6-C | C |
| ONB-01 | P1 | CTA acessível com teclado; onboarding de tags sem sobreposição | P6-C | C |
| QUEST-01 | P1 | Seleção RANDOM usa contexto/histórico válido e não fica presa na primeira categoria | P6-D | D |
| QUEST-02 | P1 | Fallback procedural tem variedade testável e validação obrigatória | P6-D | D |
| MODEL-01 | P1 se LLM local continuar requisito V1 | Fonte/distribuição, integridade, instalação, ativação e UX honesta; sem modelo no APK atual | P6-E | E/F |
| UI-04 | P2 | Proporções atualizadas de temas, empty-garden e outros assets em Compose | P6-B | B |
| UX-01 | P2 | Escala de fonte e strings longas em telas compactas/tema claro e escuro | P6-C/E | C/E |
| CAT-01 | P2 | Decisão explícita de mapeamento de todos os nove assets às categorias ou superfície neutra intencional | P6-E | E |
| TYPE-01 | P2 | MS Boli só após licença/procedência e validação da legibilidade | P6-E | E |
| DOC-01 | P2 | Documentos refletem a branch, os testes reais e as decisões; nenhum status histórico contraditório | P6-A/F | A/F |
| QA-01 | P1 para encerramento | Matriz de testes integrada, release e screenshots do SHA final | P6-Final | F |

## 7. Definition of Ready e Definition of Done

### Definition of Ready (DoR)

Uma tarefa pode entrar num sprint quando:
- o comportamento esperado e a autoridade de estado estão claros;
- arquivo(s) e consumidor(es) envolvidos estão identificados;
- há evidência: screenshot, teste que falha, divergência de dimensões ou regra de produto;
- dependências de arte, licença, modelo, API ou decisão do proprietário estão explícitas;
- o teste de aceitação descreve um resultado observável;
- a mudança não exige hardcode específico de aparelho nem um fluxo paralelo.

### Definition of Done (DoD)

Uma tarefa só é DONE quando:
- o comportamento atende aos critérios de aceite;
- testes relevantes foram escritos/atualizados e passaram;
- o verificador de assets, se afetado, passou;
- os fluxos/temas/escala relevantes foram verificados em runtime e há screenshots ligadas ao SHA;
- documentação ligada foi atualizada no mesmo incremento ou numa tarefa documental dependente;
- logs não expõem conteúdo pessoal e não há falso sucesso;
- o código foi revisto e a branch permanece compilável.

“Compilou” não substitui “visualmente validado”; “screenshot boa” não substitui teste funcional.

## 8. Matriz mínima de testes

| Área | Teste automático | Teste visual/manual | Critério mínimo |
|---|---|---|---|
| Assets | :app:verifyCanonicalUiAssets | Inspecionar aliases e dimensões | Arquivos e aliases completos/sem colisão |
| Build | :app:assembleDebug e release final | Instalar APK ligado ao SHA | Instalação e navegação sem crash |
| Botões/navbar | Compose click/semantics tests | Home, Settings e barra global dia/noite | Sem duplicação; rótulos legíveis; hit targets acessíveis |
| Home cards | Layout/screenshot test | Tema dia/noite | Texto não invade arte nem duplica ícones |
| Garden | Estados hidden/hinted/revealed/collected; filtros | Coleção com nove slots e rolagem | Sem clipping, filtro funcional e itens não cobertos |
| Onboarding | Teste de nome/tag/CTA | Teclado aberto e tela compacta | Todos os passos acessíveis sem fechar teclado forçadamente |
| Quest planner | Testes com recentCategories/contexto/filtros | Série de sorteios | Não fica preso na primeira categoria; filtros respeitados |
| Procedural composer | Testes de variedade com escolha controlada | Revisão humana de 20 gerações | Saídas distintas úteis; sem fixtures de teste ou texto absurdo |
| LiteRT-LM | Catálogo, hash, download parcial, instalação, runtime/fallback | Aparelho sem modelo e com modelo, se disponível | Sem ativação parcial; estado honesto e fallback funcional |
| Tipografia | Render/overflow tests onde viável | Escala 1.0/1.3, tema claro/escuro | Texto não cortado; contraste aceitável |
| Fluxos | Testes unitários/instrumentados existentes | Onboarding → Quest → concluir → jardim; settings/backup | Estado persistido consistente e sem recompensa duplicada |

Comandos mínimos na raiz do checkout:

    ./gradlew :app:verifyCanonicalUiAssets
    ./gradlew :app:assembleDebug
    ./gradlew test
    ./gradlew lint

Para fechamento de release, executar também o build de release e os testes instrumentados disponíveis no ambiente. Registrar exatamente quais comandos foram executados e seus resultados; não relatar testes não executados como PASS.

## 9. Política de distribuição LiteRT-LM

O binário de modelo não deve ser incluído no GitHub apenas para fazer o APK parecer completo. O projeto precisa de uma decisão explícita entre disponibilização separada verificável e outra forma aprovada de distribuição.

Se for usado pacote externo:
- usar URL imutável/versionada e fonte oficial ou controlada;
- fornecer manifesto autenticado, com assinatura ou mecanismo equivalente de confiança;
- validar tamanho, SHA-256 e metadados antes de ativar;
- baixar para arquivo temporário .part e promover atomicamente após verificação;
- preservar o modelo anterior até confirmar o novo;
- mostrar tamanho estimado, licença, estado de instalação, retry e erro;
- tornar explícito quando a geração usa fallback procedural;
- executar inferência local, sem enviar conteúdo pessoal de perfil/quests para o servidor do modelo;
- não aceitar valores do manifesto sem validação nem URL arbitrária controlada por conteúdo não confiável.

Se ainda não houver artefato, licença ou origem confiável, MODEL-01 fica BLOCKED. O fallback pode continuar disponível, mas o requisito de LLM local não pode ser declarado satisfeito.

## 10. Estratégia de integração e rollback

- Trabalhar na branch feature/p6-total-ui-refactor e preservar main.
- Antes de cada sprint, registrar HEAD, status e CI de base.
- Separar commits por eixo: componente visual, layout/tela, core de geração, distribuição de modelo e documentação.
- Não alterar vários layouts simultaneamente sem screenshots de regressão.
- Ao tocar num contrato/alias, atualizar catálogo, mapeamento Gradle, consumidores e testes como uma unidade coerente.
- Se uma correção visual muda os hit targets, semântica, scroll ou estado de domínio, não considerar apenas estética: executar testes funcionais.
- Reverter somente a alteração que introduziu regressão; não reduzir gates nem relaxar validações para fazer CI passar.
- Não criar commits em main automaticamente. Pull request e merge são passos explícitos de integração.

## 11. Estado dos gates nesta revisão

| Gate | Estado na publicação deste protocolo | Motivo / condição de saída |
|---|---|---|
| P6-A — baseline/assets/docs | **INVENTÁRIO RECONCILIADO; CI BASE CONCLUÍDO** | O verificador/build do snapshot aceito para P6-B passou em [CI #38081485398](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38081485398); o documento atual ainda será validado pelo CI após este commit. |
| P6-B — composição visual | **CONCLUÍDO NESTA RODADA** | Aceitação provisória do proprietário e [CI #38081485398](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38081485398) verde no SHA `ee42ea2748e3b8e79d46d9d37983153619b83d8f`; polimento e matriz de release continuam pendentes. |
| P6-C — layouts responsivos | **EM ANDAMENTO — CI ATUAL PENDENTE** | Correção do import `dp` publicada no SHA `42426c2f6667c86c3f7c70f99b50ca20543d4f64`; consultar [CI #38093650095](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38093650095) e validar IME, escala 1.0/1.3, temas e safe areas em aparelho antes do PASS. |
| P6-D — geração diversa | **EM ANDAMENTO — CI PENDENTE** | Histórico persistido, seleção variada, composições procedurais, fallback validado e diagnóstico redigido implementados no código; CI do HEAD mais recente ainda não concluiu e a validação integrada em aparelho permanece aberta |
| P6-E — modelo/tipografia/categorias | **MODELO PUBLICADO E INTEGRADO; GATE COMPLETO PENDENTE** | CR-7.4 está em GitHub Releases com tamanho/hash fixados e catálogo/instalação explícita; inferência fresca no HEAD atual, mapeamento semântico dos nove assets, MS Boli e escala/contraste ainda exigem validação |
| P6-Final — release | **BLOCKED até todos os P1 passarem** | Exige build de release e evidência visual/funcional ligados ao SHA final |

Este quadro distingue aceitação visual provisória de P6-B, implementação ainda em progresso de P6-C e gate final de release; documentação não substitui evidência de runtime.

### P6-D implementation checkpoint — 2026-10-10

Commits published on `feature/p6-total-ui-refactor`:

- `7f55caa` — injected bounded choice source; RANDOM prefers categories absent from persisted recent history and no longer selects the first unseen category by list order.
- `f7c1023` — three bounded procedural compositions per category, including three distinct CODING angles; the game category still requires a valid game candidate.
- `131d574` — unsatisfiable category filters and missing game candidates converge to explicit `Unavailable`, not a silently substituted quest.
- `aebc23c`, `e4d28dd`, `b286d0b`, `718db63`, `24196fc` — persisted recent category history is exposed through the repository, Room DAO and production context provider.
- `4f6f3b5`, `09d09fe` — deterministic tests cover twenty validated RANDOM generations, recent/fully-covered history, hard filters and three distinct procedural CODING variants.
- `a0031cb`, `1d730ad`, `ca847f1` — state transitions update quest status without rewriting the persisted generation record, preserving its history ordering.
- `cab428a`, `85299e8`, `d31815f`, `dda9f4e` — composers expose composition origin and bounded fallback reasons; the admission-aware wrapper reports model absence, busy state and admission denial without logging generated content.
- `45ca11a`, `d92fc05`, `4744166`, `bd05a3e`, `9ea2dfc`, `ce98722` — the engine emits redacted generation diagnostics, validates a procedural fallback after invalid model-originated drafts, shows an explicit user-facing message for incompatible filters, and tests the diagnostic/fallback path.

### Current evidence and limitations

- Android CI was triggered for the implementation commits. The latest run must be checked by exact SHA before this sprint can be marked green.
- No CI result is represented as PASS while its run is queued or in progress.
- The core now exposes actual composition origin, result status and enum-like fallback reason through a non-sensitive observer; the Android graph logs only these bounded fields. A deterministic unit test covers invalid model-originated output converging to a validated procedural fallback. Device-level evidence that the current installed CR-7.4 model is used on the production UI path, plus a fresh CI result for the final SHA, remains required before closing P6-D.
- The 20-generation test is deterministic through an injected choice source; it does not rely on a probabilistic expectation.
- History is read from persisted quest records. Quest state transitions now update the state column in place so accepting, starting, abandoning or rejecting a quest does not reorder generation history.

## 12. Registro de decisões em aberto

1. **Distribuição LiteRT-LM:** confirmar a fonte do modelo, licença, tamanho, hash, formato/runtime suportado e mecanismo de confiança. Sem esses dados, não inventar URL nem adicionar binário ao Git.
2. **MS Boli:** confirmar arquivo legítimo e licença antes de adicionar a fonte.
3. **Mapeamento visual de categorias:** resolvido visualmente pelo proprietário; os nove aliases estão documentados em `CANONICAL_GRAPHICS.md` e `P6_UI_UX_INTERACTION_CONTRACT.md`. Os IDs e afinidades de domínio não foram alterados.
4. **Navbar:** confirmar visualmente a posição do rótulo em relação aos ícones incorporados na arte e manter uma única barra global.
5. **Critério de diversidade:** definir limite aceitável de repetição junto com testes determinísticos, sem requisito impossível de nunca repetir texto.
6. **Escopo de P6-Final:** se o proprietário deliberadamente retirar o modelo local do requisito V1, documentar a decisão de produto antes de rebaixar MODEL-01.

## 13. Atualização documental obrigatória ao fim de cada sprint

Ao fechar P6-A, B, C, D ou E, registrar:
- data e sprint;
- branch e SHA exato;
- resumo dos arquivos alterados;
- comandos e testes executados, com resultado literal;
- execução CI vinculada;
- screenshots antes/depois e cenário;
- itens não resolvidos, riscos e gates ainda abertos.

Ao fechar P6-Final, registrar o SHA do APK/release, checks, matriz visual, estado do modelo, decisão de fonte/categorias e aprovação dos gates. O README e o documento de estado só devem ser atualizados depois que esse registro tiver evidência. Nenhum gate se fecha automaticamente porque um sprint seguinte começou.

## Referências operacionais

- [Branch feature/p6-total-ui-refactor](https://github.com/sasandralean-prog/PinhoQuest/tree/feature/p6-total-ui-refactor)
- [Execução Android CI #158](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38008684377)
- [app/build.gradle.kts](https://github.com/sasandralean-prog/PinhoQuest/blob/feature/p6-total-ui-refactor/app/build.gradle.kts)
- [ASSET_CATALOG.md](https://github.com/sasandralean-prog/PinhoQuest/blob/feature/p6-total-ui-refactor/docs/design/ASSET_CATALOG.md)
- [CANONICAL_GRAPHICS.md](https://github.com/sasandralean-prog/PinhoQuest/blob/feature/p6-total-ui-refactor/docs/design/CANONICAL_GRAPHICS.md)
- [P6_UI_UX_INTERACTION_CONTRACT.md](https://github.com/sasandralean-prog/PinhoQuest/blob/feature/p6-total-ui-refactor/docs/design/P6_UI_UX_INTERACTION_CONTRACT.md)
- [P6-A baseline visual e inventário de evidências](https://github.com/sasandralean-prog/PinhoQuest/blob/feature/p6-total-ui-refactor/docs/evidence/p6-a-2026-10-10-baseline.md)
- [PINHO_QUEST_VISUAL_IDENTITY_GENOME.md](https://github.com/sasandralean-prog/PinhoQuest/blob/main/docs/identity/PINHO_QUEST_VISUAL_IDENTITY_GENOME.md)
