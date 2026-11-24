# PinhoQuest — Próximas atualizações: Jardim Vivo, áudio, flores, IA e acabamento

**Decisão de produto:** 2026-11-24 · **Status:** `planned / paused` · **Autoridade:** intenção expressa do criador, condicionada à anatomia, implementação e testes reais · **Branch de referência:** `feature/p6-garden-delight`.

Este é um **backlog de retomada**, não um relato de implementação nem autorização para lançar. O PinhoQuest está pausado enquanto o foco principal está em outro projeto. Preservar o V1 aprovado, a filosofia anti-tédio (convite curto, acolhimento, sem retenção punitiva), o plano P6 de polimento e o P7 Living Garden já estabelecidos; este documento detalha atualizações dentro dessas trilhas, **sem criar uma nova arquitetura paralela**.

## 1. Produto desejado e estilo visual

- O cenário aprovado atualmente é **ilustrado e suave**, desenvolvido visualmente no Gemini Pro; manter essa direção como baseline. O desejo original de um jardim inteiro em sprites/pixel art pode ser explorado futuramente, **não é requisito deste pacote**.
- As **flores colecionáveis** continuam com seu pipeline de arte pixelizada próprio, independente do fundo ilustrado. Refinar recorte/transparência e qualidade visual sem fingir que o jardim inteiro já é pixel art.
- Animações discretas, atmosfera calma, som com controle, interações claras, visual coerente, geração de quests melhor calibrada e apoio voluntário ao criador. O app **não** vira um jogo competitivo ou um serviço de tempo de tela.

## 2. Inventário verificado na branch corrente (não reimplementar)

| Área | O que já existe no repositório | O que ainda deve ser investigado/aperfeiçoado |
| --- | --- | --- |
| Música ambiente | `app/src/main/kotlin/com/pinhoquest/ui/music/PinhoQuestMusicHost.kt`: playlist Media3/ExoPlayer com **quatro faixas locais**, repetição, fade, lifecycle, audio focus. `MainActivity.kt` monta host acima da navegação | Escuta no dispositivo real, transições, foco/silêncio, eventual falha de asset e consumo; **não construir outro player global** |
| Volume da música | `SettingsScreen.kt`, `PinhoQuestViewModel.kt` e `AppPreferencesStore.kt`: `musicEnabled` e `musicVolume` persistidos. Configuração atual: 18% por padrão, slider até 35% do master | Separar canais/controles de música e efeitos, labels compreensíveis, acessibilidade, preferências e restauração de backups; manter compatibilidade |
| Jardim animado parcialmente | `GardenScreen.kt` usa `GardenVideoBackground` no tema noturno e `PinhoFireflies` como overlay; fundos ilustrados dia/noite existem | Acrescentar movimento sutil da vegetação/flores e feedback de interação sem duplicar as animações já existentes; tema diurno e modo de movimento reduzido |
| Flores / recorte | `FlowerPixelArtProcessor.PIPELINE_VERSION = flower-pixel-v6`, fluxo de Worker/cache/validação de sprites; imagens transparentes 96×96 e proveniência | v6 está **com gate visual/CI pendente** no registro `CURRENT_STATE.md`; testar cortes difíceis e aceitar somente sprites reconhecíveis antes de novas alterações |
| IA de quests | `QuestPlanner`, `QuestEngine`, `QuestValidator`, `MicroQuestComposer` e LiteRT-LM local com fallback procedural | Medir qualidade, relevância, diversidade e falsos resultados antes de escolher prompt tuning, ajustes de parâmetros, SFT ou novo artefato |
| Doação | `SettingsScreen.kt` apresenta o botão e `MainActivity.kt` associa `onDonate` a um `Intent.ACTION_VIEW` com URL Stripe já configurada | **Confirmar com o criador que é o link correto** e validar abertura em dispositivo/navegador, falhas, segurança e comportamento sem navegador; não inventar/substituir link |

## 3. Pacotes de trabalho propostos

### NEXT-0 — Baseline e documentação de retomada

- Verificar branch/HEAD, mudanças não publicadas, `CURRENT_STATE.md`, `Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md`, `Engineering_Genome/Planejamento.md`, especificação V1 e owners reais.
- Capturar screenshots/vídeos curtos dia/noite, composição do jardim vazio/com flores, estado de som nas Configurações e o percurso do botão de apoio. Separar observação de hipótese.
- Preservar o resultado de P6-C/P6-E aprovado, sem revisitar todos os pixels arbitrariamente. Recuperar estado de CI para pipeline v6 e limitação de licencia/redistribuição.

### NEXT-1 — Verificação de doação e acabamento de baixo risco

- **Item já ligado:** preservar callback `onDonate` e URL existente, confirmar o destino desejado com o criador; caso diferente, exigir novo link explícito.
- Validar abertura por `ACTION_VIEW` com navegador, ausência de navegador, desconexão e retorno ao app; mostrar feedback verdadeiro e não afirmar que houve pagamento apenas porque um link foi aberto.
- Doação sempre opcional, sem bloqueio de quests, recompensas, XP ou pressão emocional. Não inserir credenciais de pagamento no app.

**Gate:** instrumentação/manual do botão real em aparelho; URI confirmada; nenhum “sucesso de doação” falso.

### NEXT-2 — Áudio ambiente, sons de clique e acessibilidade sonora

- **Aprimorar** o host global de música existente; preservar as quatro faixas, estado persistente, foco de áudio e transições entre telas. Testar áudio em aparelhos e com fones, interrupção e retomada.
- Introduzir feedback curto de clique **apenas em ações apropriadas** (botões, seleção, conclusão), com um canal/camada de efeitos proprietário, limitado e desacoplado dos ViewModels de domínio. Evitar múltiplos tocadores globais e reproduções duplicadas em recomposições Compose.
- Adicionar `effectsEnabled` e `effectsVolume` persistidos em DataStore, independentes de `musicEnabled`/`musicVolume`; avaliar mute geral se fizer sentido, mas **não apagar** a configuração existente.
- No controle acessível: label claro para cada volume, indicação de mudo, controles navegáveis via TalkBack/teclado, compatibilidade com escala de fonte e tema; respeitar modo silencioso/volume do sistema e foco da mídia. Sons não podem ser exigidos para entender nenhuma ação.
- Definir volume inicial de efeitos após teste de percepção no dispositivo, sem presumir que a intensidade de música equivale à intensidade de SFX. Modo sem som integral deve funcionar.
- Manter o histórico: o host atual aplica fades sequenciais em player único; **não chamá-lo de crossfade de duas fontes sobrepostas** sem implementar/provar.

**Gate:** eventos de UI tocam uma vez sem travar; volumes e toggles persistem e ficam independentes; backup/restore antigo é compatível; TalkBack, audio focus, fones, background e silêncio testados.

### NEXT-3 — Jardim vivo: animação suave sobre a arte aprovada

- Animar folhas, balanço de flores, brisa e pequenas reações ao toque; preservar o fundo ilustrado autoral e os efeitos existentes de vídeo noturno/vaga-lumes, sem redesenho integral em sprites.
- Apresentação consome `GardenUiState`/Room: animação **não** altera estado canônico, XP, raridade, coleção nem desbloqueio. Evitar animação permanente em telas não visíveis; pausar em background e respeitar economia de energia.
- Respeitar acessibilidade de movimento reduzido, preferência opcional de animação e alternativa estática. Garantir que elementos decorativos não interceptem toque, não escondam rótulos nem provoquem flashes ou movimentos bruscos.
- Controlar quantidade de partículas, loops, CPU/GPU e overdraw. Compor temas diurno/noturno coerentes e testar em aparelhos modestos.

**Gate:** animações fluidas em cenário vazio e populado, estático correto com redução de movimento, sem degradação de scroll, bateria, memória, toque ou leitura.

### NEXT-4 — Refinamento do recorte e pixelização das flores

- **Primeiro validar o gate pendente do pipeline `flower-pixel-v6`**, com artefatos de imagem real e revisão humana; não promover resultado baseado só em build ou contagem numérica.
- Tratar falhas de recorte de flores brancas/claras e verdes sobre fundo semelhante, silhueta central, folhas/caule próximos, transparência e ruídos de borda. Não sacrificar pétalas para produzir alpha limpo, nem preservar um retângulo de fundo como “folhagem”.
- Manter renderer determinístico e `FlowerArtwork` versionado, hash/caches `pipelineVersion`, proveniência, licença e fallback de placeholder para fonte inadequada. Mudança de algoritmo exige atualização dos testes e invalidação segura de cache.
- Comparar original/corte/sprite e renderização no card real; fotografias difíceis e diferentes cores/posições, fixtures licenciadas e evidência visual. A pixelização desejada inclui silhueta legível, paleta com profundidade e acabamento consistente em 96×96; variação de resolução requer decisão separada.
- Uma espécie sem fonte confiável permanece indisponível/placeholder; não inventar correspondência botânica.

**Gate:** evidência visual real aceita, testes de identidade/proveniência/backup/cache, pipeline sem regressão em coleções antigas.

### NEXT-5 — Refinamento de design, consistência e acessibilidade geral

- Revisar alinhamento, tipografia, espaçamento, cartões, navbar, botões, cabeçalhos e composição dia/noite a partir do baseline visual **já aprovado**; consolidar ajustes menores em um único polimento, em vez de microcorreções dispersas.
- Preservar as telas e fluxos Início/Jardim/Perfil + Configurações internas; o design deve continuar acolhedor, sem competição, urgência ou notificações manipulativas.
- Validar telas compactas, fontes ampliadas, contraste, áreas de toque, leitor de tela, estados vazios, erro/loading, IME, safe areas e animações desligadas. Controles de som e flores devem ser totalmente compreensíveis sem efeitos especiais.
- Distinguir animações de UI de elementos de arte exportada (Gemini Pro). Se houver novos assets, atualizar `docs/design/ASSET_CATALOG.md` / `CANONICAL_GRAPHICS.md` com proveniência e dimensões reais.

**Gate:** capturas comparativas e testes de interação; arte/navegação/estados canônicos intactos, regressões de P6-C/P6-E não reaparecem.

### NEXT-6 — Fine-tuning da IA do app, guiado por evidência

- Primeiro levantar métricas de quests geradas: adequação às tags/mode, diversidade, clareza, duração, segurança, repetição e taxa de fallback; construir bateria semântica com cenários normais, extremos e offline.
- Classificar causa antes de alterar pesos: planejador, facts assembly/prompt, codec, modelo LiteRT-LM, filtro, validator ou catalog research. `QuestPlanner` continua autoridade de categoria/ambiente; `QuestValidator` aprova a Quest; o LLM **só redige** sugestões dentro dos fatos e restrições aceitos.
- Ajustar prompts/parâmetros ou realizar fine-tuning/SFT e nova distribuição de artefato **somente se a medição demonstrar necessidade e houver dataset/licença adequados**. Não assumir que “fine-tuning” significa treinar o modelo dentro do Android.
- Manter geração local, fallback procedural, observabilidade e privacidade. Não usar conversas/hábitos identificáveis como dados de treino sem consentimento explícito.
- Comparar antes/depois com fixtures fixas, avaliação humana e métricas de latência/memória/qualidade; não melhorar variedade degradando verdade factual, compatibilidade ou segurança.

**Gate:** melhora demonstrada em bateria semântica, sem regressão em categoria, validação, catálogo, offline, performance e fallback.

## 4. Ordem recomendada e condições de passagem

```text
NEXT-0 diagnóstico
    ├─ NEXT-1 doação (verificação rápida, se link confirmado)
    ├─ NEXT-2 música + SFX + controles acessíveis
    ├─ NEXT-3 animação do jardim
    └─ NEXT-4 flores pixelizadas / recorte (começar pelo gate v6)
          ↓
NEXT-5 polimento integrado de UX
          ↓
NEXT-6 fine-tuning IA guiado por benchmark (pode iniciar baseline em paralelo)
          ↓
Gate de regressão e publicação
```

Cada pacote necessita branch/worktree isolada quando houver código, testes antes/depois, status honesto e decisão de aceite. Os itens NEXT-3/NEXT-4 pertencem tematicamente à evolução **P7 Living Garden**; NEXT-2/NEXT-5 são polimento P6; NEXT-6 integra os contratos da geração P3/P6; NEXT-1 é acabamento de release. Não renomear ou fechar os sprints canônicos pelo simples fato de existir este plano.

## 5. Gates cruzados e documentação

- Mínimo: Kotlin/JVM/Android-data unit tests, Compose UI, Android emulador, lint, build e testes em dispositivo para som, toque, animação e imagem; separar testes com mock de validação visual/física.
- Não alterar domínio, XP, progressão, quest tags, flora real, garantias de provenance, backup/restore ou estado Room só para produzir um efeito visual.
- Para áudio e doação, testar sem serviços externos e sem navegador, e depois o caminho de usuário real. Documentar licenças/redistribuição dos MP3 gerados em Suno e da fonte Ihlimia antes do release público.
- Observabilidade explícita: `planned`, `implemented_unvalidated`, `validated_bounded`, `blocked_external`, `validated_runtime`; nunca afirmar que “funciona” por leitura de fonte/CI desatualizada.
- Após implementação efetiva atualizar `CURRENT_STATE.md`, P6/P7 protocolos, `docs/design/UI_DESIGN_CONTRACT.md`, catálogo de assets e provas por SHA. Não substituir o histórico dos checkpoints.

## 6. Decisões pendentes do responsável

1. Prioridade relativa das seis frentes e janela de retomada (proposta acima pode ser reordenada).
2. Se a animação deve afetar as flores expostas individualmente, o plano de fundo ou ambos; intensidade padrão/limite de bateria.
3. Eventos que recebem SFX, intensidade inicial e se haverá controle geral de áudio adicional aos dois sliders independentes.
4. Confirmação do URL de doação já conectado (ou fornecimento explícito de outro destino).
5. Critério subjetivo de aceitação de flores pixelizadas após revisar original e sprite, sem perder proveniência.
6. Escopo concreto do “fine-tuning”: experimentação com prompts/geração primeiro ou também treinamento/exportação de novo modelo, conforme benchmark.

**Este arquivo é compromisso de planejamento, não prova de entrega.**
