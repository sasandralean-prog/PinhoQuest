# P6 — Protocolo Oficial de Implementação e Transição

**Projeto:** PinhoQuest  
**Branch de referência:** `feature/p6-total-ui-refactor`  
**Estado:** protocolo normativo; não significa que gates de implementação estejam fechados.  
**Última revisão documental:** 2026-10-09  
**Escopo desta revisão:** documentação e contratos. Código Kotlin, assets binários, scripts de build e testes não são alterados por esta etapa.

> Este documento define como o P6 deve ser executado, verificado e encerrado. Evidência técnica prevalece sobre expectativa, screenshot ou descrição histórica. Uma hipótese só vira diagnóstico confirmado quando existe reprodução ou evidência rastreável.

## 1. Objetivo e resultado esperado

O P6 refina a experiência visual e de interação do PinhoQuest sem alterar silenciosamente as regras do produto nem criar uma segunda autoridade para navegação, estado de domínio, persistência ou geração de quests.

O resultado de P6 precisa demonstrar, com evidência, que:

1. os assets gráficos têm inventário canônico e mapeamentos Android rastreáveis;
2. as telas recompõem layouts de referência com controles reais e acessíveis;
3. backgrounds, botões e cards preservam proporções e não interceptam interação;
4. o Jardim distingue vazio, jardim em arte e coleção de nove posições, refletindo dados reais;
5. a geração de quests permanece sob contrato de domínio, recebe contexto permitido e tem diagnóstico suficiente para investigar variedade;
6. a navegação global e os fluxos internos seguem uma hierarquia inequívoca;
7. testes automatizados e inspeção visual cobrem o comportamento alterado;
8. o estado publicado diferencia implementação, validação limitada, bloqueio e gate concluído.

P6 não será declarado concluído por uma compilação verde, por um screenshot isolado ou porque uma tela parece correta. Cada gate precisa satisfazer critérios de saída.

## 2. Escopo e limites

### Incluído

- Inventário, nomenclatura, classificação e mapeamento de assets em `docs/design/BackGround/`, `docs/design/Button/` e referências em `docs/design/Screen/`.
- Reconstituição visual de Home/Quest, onboarding/nome e tags, Jardim/coleção, Perfil, Configurações e navegação.
- Comportamentos visuais de dia/noite, controles selecionados, escala tipográfica, estados disabled/loading/failure e acessibilidade.
- Estados visuais do Jardim derivados do estado de domínio e navegação entre composições.
- Auditoria do fluxo de geração de quests para determinar se contexto/tags, planner, composer, modelo/fallback e validação mantêm seus contratos.
- Cobertura de testes unitários, testes de UI/instrumentados, E2E produtivo aplicável e inspeção visual.
- Correção e sincronização de documentos canônicos e do estado operacional.

### Fora do escopo desta etapa documental

- Alterações em código de runtime, regras de domínio, persistência, assets binários, Gradle/workflows e testes.
- Fechar qualquer gate apenas porque ele está descrito neste documento.
- Alterar a semântica de temas/tags, política de recompensa, identidade botânica, progressão ou contrato do modelo.
- Remover recursos antigos antes de provar que nenhum consumidor depende deles.
- Alegar execução de teste sem saída de ferramenta, workflow ou evidência preservada.

## 3. Snapshot e qualidade da evidência

A branch remota confirmada é `feature/p6-total-ui-refactor`. Os arquivos documentais foram lidos pelo GitHub Connector nessa branch. A API usada nesta revisão não forneceu um SHA de HEAD de branch confirmado; portanto este protocolo não fixa um hash-base fictício. Ao realizar o próximo checkpoint, registrar o SHA real de HEAD observado e atualizar `CURRENT_STATE.md`.

### Estados obrigatórios para afirmações

- **CONFIRMADO:** leitura direta de código/documento, teste executado com resultado, workflow identificado ou reprodução documentada.
- **HIPÓTESE:** explicação plausível, mas sem prova suficiente para estabelecer causalidade.
- **PENDENTE DE VERIFICAÇÃO:** não foi possível observar ou validar no snapshot analisado.
- **DECISÃO NORMATIVA:** regra aprovada para orientar implementação futura; não implica que esteja implementada.
- **SUPERADO:** afirmação antiga substituída por evidência ou contrato mais recente. Preservar a razão da supersessão quando relevante.

Toda atualização de estado deve indicar data, branch, SHA, escopo do teste e evidência. “CI verde” sem identificar workflow e comandos não é evidência suficiente.

## 4. Mapa de autoridade documental

Os documentos têm papéis diferentes; não devem competir pela mesma autoridade.

1. **Identidade visual:** `docs/identity/PINHO_QUEST_VISUAL_IDENTITY_GENOME.md` e `docs/identity/PINHO_QUEST_GARDEN_PIXEL_ART_GENOME.md`. Uma alteração de identidade atualiza primeiro a autoridade correspondente.
2. **Contrato de implementação visual:** `docs/design/UI_DESIGN_CONTRACT.md`. Define limites de implementação, responsividade, acessibilidade e autoridade da UI.
3. **Referências classificadas:** `docs/design/CANONICAL_GRAPHICS.md`. Indexa composição, referência visual, papel e status sem substituir o inventário de arquivos.
4. **Inventário e aliases de recursos:** `docs/design/ASSET_CATALOG.md`. Referência operacional de nomes de origem e nomes de recurso gerado; a configuração real do build é a prova final do mapeamento.
5. **Interação de produto P6:** `docs/design/P6_UI_UX_INTERACTION_CONTRACT.md`. Define comportamento esperado de ações, temas/tags, Jardim e coleção.
6. **Este protocolo:** `Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md`. Governa ordem dos gates, critérios de validação, checkpoints e classificação de evidências. Não redefine identidade visual nem contratos de domínio.
7. **Estado operacional:** `CURRENT_STATE.md`. Registra o que existe e foi validado na branch/commit observados; nunca converte metas futuras em fatos atuais.
8. **Genome de entrada e handoff:** `Engineering_Genome/00_START_HERE.md`, `01_ENGINEERING_PHILOSOPHY.md`, `02_AUTHORITY_MAP.md` e `10_RAFA_LUCIO_HANDOFF.md`. Devem apontar para os contratos vigentes e evitar copiar especificações completas.

### Regra de precedência e reconciliação

Em caso de conflito, respeitar a autoridade específica do domínio acima. Se dois documentos do mesmo nível divergirem, não escolher silenciosamente: abrir alteração documental pequena, explicar qual regra prevalece, corrigir os dois lados e registrar a decisão. O catálogo não decide qual tela deve usar cada background; ele registra assets e aliases. O registry visual classifica referências; não substitui o inventário.

## 5. Diagnóstico documental e técnico inicial

Esta seção separa observações do snapshot de explicações ainda por confirmar.

### 5.1 Assets e composição visual

**CONFIRMADO por inspeção documental:**

- `docs/design/ASSET_CATALOG.md` contém tabelas de assets de `BackGround/` e `Button/` com aliases de recursos gerados por `app/build.gradle.kts`.
- O catálogo declara `verifyCanonicalUiAssets` como dependência de `preBuild` e determina reter recursos antigos até que consumidores sejam migrados e regressões verificadas.
- `docs/design/CANONICAL_GRAPHICS.md` preserva nomes e papéis antigos, como `BtnStart.png`, `BtnSortQuest.png` e `BtnBack.png`, e contém uma nota contraditória sobre `docs/design/Button/`: uma seção diz que o caminho foi normalizado, outra afirma que o caminho publicado ainda tem espaço final.
- Os contratos visual e de interação afirmam que screenshots são referências de composição, não telas estáticas que substituem controles Compose.
- `docs/design/Screen/1791405120200.jpg` é um quadro/folha de referência de composição, não um background de produção.
- `docs/evidence/p6-v05-ui-ux.md` documenta um checkpoint histórico V0.5 que usou templates compostos como backgrounds e reporta testes daquele checkpoint. Isso não demonstra, por si só, a validade do layout/código atual da branch P6.

**HIPÓTESE a verificar no código/runtime:**

- Parte das telas pode ainda consumir recursos legados em vez dos aliases canônicos.
- `ContentScale.FillBounds` pode deformar botões quando a proporção do contentor não coincide com a imagem. A ocorrência e os elementos afetados devem ser confirmados no código da branch antes de atribuir impacto visual concreto.
- A integração visual pode estar incompleta em onboarding, configurações, Jardim e barra inferior.

**DECISÃO NORMATIVA:**

- PNG/JPG de origem não será renomeado ou modificado apenas para cumprir regras Android.
- Aliases de recursos são gerados a partir de um único mapeamento verificável. Não manter uma segunda tabela manual que possa divergir do build.
- Uma referência completa de tela nunca pode ser o único elemento de renderização de um fluxo interativo.
- Imagens devem manter proporção. Usar `Fit`, dimensões com razão explícita ou estratégia documentada; `FillBounds` somente quando a deformação for deliberada e aprovada para o asset.
- Recursos legados só podem ser removidos após busca de consumidores, migração, build, testes de regressão e checkpoint de remoção.

### 5.2 Estados do Jardim

**CONFIRMADO por contrato:** o Jardim tem uma dimensão de espaço visual e uma experiência de coleção; a coleção inicial tem nove flores, com estados de descoberta e coleta definidos pelo domínio. A UI não inventa identidade, raridade, pertencimento à coleção ou progresso.

**PENDENTE DE VERIFICAÇÃO:** expressão atual de estado em `GardenScreen.kt`, semântica de `collectedCount`, representação de slots vazios e completude dos testes na branch/HEAD atual precisam ser verificadas durante P6-C. Não tratar nomes de campos presumidos como contrato definitivo.

**DECISÃO NORMATIVA — três composições:**

1. **Jardim vazio:** nenhuma flor foi coletada no perfil. Explica o primeiro passo e leva para quests; não deve fingir que existem flores coletadas.
2. **Jardim em arte:** pelo menos uma flor foi coletada e a coleção não está aberta. Mostra a composição do lugar e o progresso fornecido pelo domínio.
3. **Coleção:** o usuário abriu explicitamente a coleção. Mostra nove posições e filtros/estados apoiados em dados governados (desconhecida, pesquisada/revelada e coletada conforme o modelo vigente).

O estado de domínio determina se o jardim já floresceu; a seleção entre “arte” e “coleção” é estado de navegação/presentação. Essa seleção visual não altera coleção, recompensa ou persistência. Abrir coleção não gera coleção, gasta XP nem modifica o domínio. Voltar à arte apenas retorna à composição anterior.

Os critérios devem cobrir perfil com zero flores, uma flor, coleção com posições ainda desconhecidas, estados mistos e transição/retorno. Não introduzir fixtures incompatíveis com o modelo atual sem declará-las.

### 5.3 Geração e variedade de quests

**CONFIRMADO por contrato de arquitetura:** planner e validação pertencem ao core/domínio; o composer produz conteúdo criativo; o compositor local é opcional sob política de admissão; o fallback procedural permanece governado; a UI não constrói quests diretamente. O `CURRENT_STATE.md` consultado afirma que, naquele checkpoint documentado, a geração de produção ainda seguia caminho determinístico/procedural e não devia ser confundida com o trabalho experimental de FunctionGemma.

**HIPÓTESES a investigar, não diagnósticos concluídos:**

- Repetição pode decorrer de categorias selecionadas deterministicamente, contexto recente vazio, tags que não chegam ao composer ou templates do fallback.
- Pode haver mais de uma causa concorrente; observar duas quests repetidas não prova causalidade.
- A origem `LOCAL_MODEL` versus `PROCEDURAL_FALLBACK` pode não estar exposta de forma suficiente no limite público necessário ao diagnóstico.

**Protocolo normativo para P6-D:**

1. Reproduzir repetição com semente/contexto/configuração documentados ou teste determinístico de regressão.
2. Rastrear o pedido: seleção de tema/tags → contexto aprovado → planner → plano → composer/admission/modelo ou fallback → validador → resultado exibido.
3. Em cada fronteira, registrar entrada/saída tipada e limitada; não registrar prompts livres, dados pessoais nem payloads brutos do modelo.
4. Confirmar por contrato quais tags podem ser passadas; usar somente labels permitidos, normalizados e limitados. Não enviar entidades Room, histórico bruto, afinidades internas, HTML/URLs ou pesquisa não filtrada ao modelo.
5. Tornar observável a origem do resultado por resultado tipado/telemetria de diagnóstico, sem acoplar core a Android `Log` se isso violar fronteiras de módulo.
6. Garantir que resultados do modelo e fallback convergem para o mesmo `QuestValidator`. Saída inválida não pode contornar o validador.
7. Testar variedade ao longo de uma sequência, distribuição de categorias quando aplicável, influência de tags, indisponibilidade do modelo, admissão negada, resultado inválido e esgotamento de candidatos.
8. Corrigir a causa identificada por camada; não adicionar aleatoriedade cega se a repetição resultar de contexto/tag não propagados ou contrato quebrado.

P6-D não autoriza refazer a integração LiteRT-LM experimental nem violar os gates de runtime P3/CR. Alterações no runtime local seguem o contrato de autoridade do Engineering Genome e o gate CR vigente.

### 5.4 Navegação e destinos

**CONFIRMADO por contratos visuais P6 consultados:** a composição canônica atual de Home prevê exatamente três destinos globais na barra inferior — **Início, Jardim, Perfil**. Configurações é um destino acessado por fluxo interno, não uma quarta aba. O `Engineering_Genome/00_START_HERE.md` ainda contém uma formulação legada de quatro abas (Quests, Tags, Jardim, Configurações), portanto existe divergência documental a reconciliar com a especificação V1 e contratos atuais.

**DECISÃO NORMATIVA para P6-B/P6-E:**

- Documentos atuais distinguem destinos globais de subtelas/fluxos internos. A barra global apresenta apenas os três destinos aprovados até que uma decisão explícita de produto altere os contratos canônicos.
- Seletor de tema, detalhes de flor, configurações e onboarding são fluxos/subtelas; só aparecem como destinos globais se uma decisão explícita atualizar primeiro os contratos canônicos.
- Cada navegação tem política previsível para back da UI e do sistema. Configurações retorna ao chamador/destino de origem conforme back stack; não deve sair do app por acidente.
- A visibilidade da barra durante onboarding e subtelas segue o contrato de cada tipo de destino, não uma condição ad hoc espalhada na UI.
- Não forçar a barra a estar sempre visível: cada fluxo define onde ela é apropriada. Testar transições reais, inclusive o seletor de tema.

### 5.5 Testes, CI e acessibilidade

**CONFIRMADO por documentação lida:** o checkpoint V0.5 reporta testes instrumentados executados naquele checkpoint. `CURRENT_STATE.md` também registra resultados de gates anteriores e limitações de lint/toolchain em determinado momento. Esses registros históricos não provam o estado de CI ou instrumentação no HEAD atual.

**PENDENTE DE VERIFICAÇÃO:** conferir workflow atual e seus comandos antes de dizer que executa `:app:connectedDebugAndroidTest`, E2E de modelo, screenshots ou matriz dia/noite. A mera presença de arquivos `androidTest` não prova cobertura.

Regras:

- CI executa testes unitários por módulo, lint/build configurados e instrumentados somente quando o workflow os invocar explicitamente.
- Teste instrumentado só é considerado executado com resultado registrado (workflow/job/log ou evidência local reproduzível).
- E2E deve atravessar o fluxo real especificado; unitário, montagem de APK e teste isolado de componente não substituem E2E.
- Controles acessíveis oferecem semântica estável (rótulo/descrição/testTag quando adequado) e touch targets reais. Não resolver falhas reduzindo acessibilidade nem usando coordenadas arbitrárias sem justificativa.
- Regressão visual registra dispositivo/API, escala tipográfica, tema, fixtures e critério de comparação; diferenciar screenshot aprovado de mera captura produzida.
- Erros do pipeline são triados por camada e registrados como bloqueio, não escondidos por desativar testes ou diminuir cobertura.

## 6. Gates oficiais do P6

Os gates são sequenciais quando houver dependência. Cada gate termina em `PASS`, `PASS_BOUNDED`, `BLOCKED` ou `NOT_STARTED`. `IMPLEMENTED` sozinho não equivale a validado.

### P6-A — Canonização documental e inventário de assets

**Objetivo:** uma única autoridade operacional para nomes de origem → aliases e classificação coerente de referências.

**Entrada:** branch/SHA observados; catálogo, registry, build mapping e diretórios de assets disponíveis.

**Trabalho:** conferir cada path/nome real; reconciliar `ASSET_CATALOG.md` e `CANONICAL_GRAPHICS.md`; retirar listas obsoletas ou marcá-las como histórico; classificar referências como canônica, secundária/duplicada, decorativa, não canônica ou não classificada; manter `UI_DESIGN_CONTRACT.md` como autoridade de implementação e o contrato P6 como autoridade de interação; atualizar Genome/README/CURRENT_STATE com links e precedência.

**Saída obrigatória:** inventário sem aliases duplicados e com paths reais; nenhum conflito não declarado entre path, catálogo e registry; decisão para imagens timestamp e nomenclaturas contraditórias; `verifyCanonicalUiAssets` executado com evidência ou status bloqueado.

**Bloqueadores:** arquivo citado inexistente; alias/path ambíguo; afirmação de validação do build sem evidência.

### P6-B — Composição visual e controles

**Objetivo:** reconstruir telas com referências canônicas e interações reais.

**Entrada:** P6-A validado para assets consumidos no slice de tela.

**Trabalho:** migrar uma tela por checkpoint; ligar cenário e skin aos aliases existentes; preservar callback, estado, navegação, copy e acessibilidade; validar proporções e recorte em telas/fontes representativas.

**Saída obrigatória:** build/lint disponível executados; testes de UI pertinentes; capturas dia/noite e escala de fonte quando relevantes; verificação de toque/teclado/back; regressão conhecida triada.

**Bloqueadores:** referência de tela usada como UI estática interativa; botão sem callback; campo sem foco/teclado; deformação não intencional; controle inacessível.

### P6-C — Máquina de estados do Jardim

**Objetivo:** três composições refletem a autoridade de domínio e mantêm a coleção intacta.

**Entrada:** contrato de interação/identidade visual reconciliados; fonte dos dados de coleta identificada.

**Testes obrigatórios:** Jardim vazio; primeira flor coletada; coleção aberta; nove posições; estados mistos de descoberta; filtro; detalhe de flor; retorno da coleção; recriação/process death se houver dependência de estado persistido.

**Saída obrigatória:** regra de transição documentada e testes verdes; troca de composição não muta domínio; fixtures compatíveis com nove slots e modelo vigente.

**Bloqueadores:** usar lista vazia como substituto de contagem de coleta quando não equivalentes; perder coleções, inventar flores ou regenerar coleção ao abrir tela.

### P6-D — Contexto, variedade e fallback de quests

**Objetivo:** diagnosticar e corrigir repetição com evidência, não aleatoriedade paliativa.

**Entrada:** baseline reproduzível e fronteiras de planner/composer/validator identificadas.

**Testes obrigatórios:** tags permitidas influenciam geração conforme contrato; sequência mede repetição; caminhos local e procedural; indisponibilidade/saída inválida; admissão negada; validador aplicado em todos os caminhos; esgotamento de candidatos.

**Saída obrigatória:** causa(s) demonstrada(s), correção por camada, regressão reproduzível e origem do resultado diagnosticável no limite apropriado. Sem causa comprovada, manter diagnóstico parcial e gate aberto.

**Bloqueadores:** raw model output chega à UI/domínio/persistência; tags ignoradas sem análise; logs expõem payload pessoal; fallback contorna validação; mudança P3 runtime sem respeitar CR gates.

### P6-E — Regressão funcional, visual e fechamento

**Objetivo:** provar que todos os gates anteriores coexistem no fluxo integrado.

**Entrada:** P6-A a P6-D concluídos ou exceção explicitamente limitada e registrada.

**Validação obrigatória:** testes unitários/UI pertinentes; Android instrumentado/E2E com ambiente identificado; onboarding/nome/teclado; Home, navegação e configurações/temas; três estados do Jardim, nove posições e progresso; geração de quests/fallback; fonte ampliada, acessibilidade e dia/noite; screenshots com manifesto do dispositivo/configuração; inspeção do diff, logs e workflow pós-commit.

**Saída:** checklist preenchido com links de runs/artefatos; limitações explícitas; `CURRENT_STATE.md` com SHA real; decisão PASS somente quando evidência cobre critérios.

**Bloqueadores:** gate anterior não demonstrado; CI inexistente para testes alegados; evidência histórica apresentada como atual; screenshot sem configuração reprodutível; alterações de runtime/dados fora do escopo sem regressão.

## 7. Protocolo de checkpoints e commits

1. **Congelar baseline:** registrar branch, SHA e CI antes de cada grupo de alterações.
2. **Mudança vertical pequena:** um contrato/documento ou uma tela/fluxo por checkpoint; evitar misturar inventário, UI, domínio e pipeline num único commit.
3. **Declarar invariantes afetados:** navegação, tags, persistência, acessibilidade, identidade dos assets e origem de quests.
4. **Executar a menor verificação útil** e ampliá-la pelo impacto. Commit só documental pode validar links/paths por inspeção; não declarar testes de runtime executados se não foram.
5. **Inspecionar diff completo:** paths inesperados, conteúdo acidental, arquivos gerados/binários e inconsistências de nomes.
6. **Atualizar CURRENT_STATE:** status, SHA real e evidência exata.
7. **Revisar antes de avançar:** não empilhar gate dependente sobre outro bloqueado.
8. **Rollback:** reverter commit isolado que introduziu a regressão; não apagar recursos antigos como limpeza antes de confirmar consumidores.
9. **Sem checkpoint inflado:** não misturar causas não relacionadas nem marcar sprint inteira concluída com validação parcial.
10. **Sem sucesso ficcional:** se uma ferramenta não consegue editar ou validar, registrar a limitação.

## 8. Matriz de evidências

| Alegação | Evidência mínima aceita | Evidência insuficiente |
|---|---|---|
| Asset disponível | arquivo real + mapping/verify de build | linha de Markdown apenas |
| Tela migrada | referência + código consumindo alias + teste/captura | alias existe, consumidor não verificado |
| Botão funcional | semântica e ação observadas em teste/UI | screenshot do botão |
| Jardim tem três estados | teste de cada estado/transição + fonte de domínio | grade visual |
| Tags influenciam quests | teste de fluxo da tag até a saída validada | tag aparece na UI ou apenas no plano |
| Fallback seguro | teste de indisponibilidade/saída inválida passando pelo mesmo validador | classe de fallback existe |
| CI cobre instrumentação | workflow/job com comando e resultado identificáveis | workflow green sem examinar steps |
| P6 fechado | gates, evidências, limitações e SHA registrados | APK compila ou screenshot isolado |

## 9. Checklist de fechamento

### P6-A
- [ ] Inventário corresponde aos arquivos existentes em `BackGround/` e `Button/`.
- [ ] Fonte de aliases é única; build mapping e documentação não divergem.
- [ ] `CANONICAL_GRAPHICS.md` não apresenta referências antigas como atuais.
- [ ] Caminhos, duplicatas e imagens não classificadas possuem decisão explícita.
- [ ] `verifyCanonicalUiAssets` executado e evidência anexada.

### P6-B
- [ ] Cada tela migrada usa composição com controles reais.
- [ ] Proporção/recorte dos assets inspecionados em dispositivo ou screenshot reproduzível.
- [ ] Campo de nome abre teclado e confirma fluxo.
- [ ] Tema e tamanho de fonte não quebram layout.
- [ ] Back stack, hitboxes e acessibilidade verificados.

### P6-C
- [ ] Jardim vazio, Jardim arte e Coleção têm cobertura de teste.
- [ ] Nove posições e status das flores vêm do domínio.
- [ ] Abrir/fechar coleção não modifica progresso ou identidade da coleção.

### P6-D
- [ ] Repetição tem baseline reproduzível.
- [ ] Tags/contexto rastreados até o composer.
- [ ] Modelo/fallback/saída inválida convergem para o mesmo validador.
- [ ] Origem do resultado é diagnosticável sem expor conteúdo sensível.

### P6-E
- [ ] Testes unitários relevantes passaram.
- [ ] Instrumented/E2E passaram, com links e ambiente registrados.
- [ ] Dia/noite, fonte ampliada e telas críticas foram inspecionados.
- [ ] CI após último commit foi revisado.
- [ ] CURRENT_STATE atualizado e gate decidido por evidência.

## 10. Pendências documentais imediatas

1. Atualizar `CURRENT_STATE.md`: o arquivo lido contém data/branch históricos de `feature/cr-0-runtime-consolidation`. Capturar SHA real da branch no momento da atualização; não usar hash estimado.
2. Corrigir a contradição de path em `CANONICAL_GRAPHICS.md` e substituir a lista de botões antiga por referência a `ASSET_CATALOG.md` como inventário atual.
3. Atualizar `Engineering_Genome/00_START_HERE.md` e `02_AUTHORITY_MAP.md` para referenciar este protocolo e distinguir navegação visual atual dos quatro tabs legados descritos em textos de arquitetura.
4. Verificar nomes/caminhos de imagens em `CANONICAL_GRAPHICS.md` contra arquivos existentes antes de classificá-los como atuais.
5. Confirmar configuração real do CI no HEAD e comandos executados, sem extrapolar resultados de checkpoints históricos.

## 11. Estado de gate ao publicar este protocolo

Esta é a formalização inicial do protocolo. A leitura documental confirma divergência de nomenclatura/autoridade e defasagem do snapshot operacional. **Isto não declara P6-A..P6-E como PASS.** Validação em código, testes, screenshots e workflow permanece pendente até que a evidência específica seja coletada.

Regra final: **o documento descreve o caminho e os critérios; só a evidência registrada fecha o gate.**
