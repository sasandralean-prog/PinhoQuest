# P6-E — Auditoria visual, tipografia e acessibilidade (2026-10-14)

**Branch de trabalho:** `feature/p6-total-ui-refactor`  
**Status:** a rodada de screenshots de 15/10 foi implementada no checkpoint de código `c2e8d05adaf0cff34e58ea81d88718ba74412440`, validado por [Android CI #38690256884](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38690256884) (BUILD SUCCESSFUL, 0 erros/38 avisos). A validação de runtime em aparelho ainda está pendente.  
**Gate:** P6-E continua aberto. P6-C também continua aberto até validação em aparelho. Follow-up de 2026-10-15 registrado na seção 7.  
**Fonte de observação:** quatro screenshots fornecidas por Rafa em 14/10/2026. Os screenshots não vieram associados a um SHA de APK, portanto mostram sintomas reais, mas não identificam por si só o commit instalado.

## 1. Resumo executivo

A principal causa dos problemas não é simplesmente “fonte grande”. O projeto já multiplica a escala de fonte do sistema por `state.fontScale` no `LocalDensity` da `MainActivity`. O problema é que algumas telas colocavam cada texto em offsets percentuais independentes e alguns botões não limitavam o número de linhas. À medida que o texto crescia, os componentes continuavam na posição original e passavam a se sobrepor.

Os prints também mostram contraste insuficiente no tema noturno das Configurações e um cabeçalho do Jardim que quebra a linha de “flores” por falta de largura flexível.

## 2. Achados nos screenshots e no código

| Área | Sintoma observado | Causa no código inspecionado | Correção inicial |
|---|---|---|---|
| Início — cartão do jardim | Métricas, nome do jardim e subtítulo colidem ou parecem pertencer à mesma linha | `GardenSummaryCard` posicionava quatro textos com offsets verticais independentes (16%, 39%, 66% da altura) | Os textos agora compartilham uma única coluna; métricas ficam numa linha e título/subtítulo fluem verticalmente |
| Início — ações | “Quest de jogo” aproxima-se do ícone; textos podem ocupar linhas demais | `PinhoGraphicButton` não impunha limite de linhas ao rótulo; o deslocamento do texto do jogo começava muito perto do ícone | O botão compartilhado limita rótulos a duas linhas; o texto de “Quest de jogo” começa mais à direita; “Quest aleatória” usa tamanho ligeiramente menor |
| Jardim | A linha de progresso quebra e “flores” fica isolada | O cabeçalho não dava peso/largura restante à coluna; XP, nível e total eram uma única frase extensa | A coluna usa o espaço restante; XP, nível e contagem são itens separados numa linha; título e subtítulo têm limites explícitos |
| Perfil | “Jardim de …” e o subtítulo invadem a região das folhas, enquanto o banner tem espaço de texto inferior | A cópia do banner ocupava muita altura e era elevada por uma margem inferior proporcional grande | O grupo foi movido para baixo, o texto foi redimensionado com o subtítulo mais legível e a área inferior de rolagem foi ampliada |
| Configurações — tema noturno | Cabeçalhos verde-escuros sobre fundo escuro e texto marrom sobre botão verde têm contraste visual fraco | As cores dos cabeçalhos e da ação de instalação eram fixas e não respeitavam `effectiveDark` | Cabeçalhos e texto auxiliar usam cores sensíveis ao tema; as ações verdes usam texto creme |
| Transversal | Rótulos de botão podem invadir ícones ou crescer verticalmente sem limite; o indicador selecionado depende em parte do brilho | Componente gráfico compartilhado permitia quebra de linha ilimitada | `PinhoGraphicButton` limita rótulos a duas linhas, com quebra e elipse; as tags visuais ficam numa linha |

Estas correções são de geometria e apresentação. Não alteram propriedade de estado de Room, lógica de geração de quests, IDs de tags ou regras de domínio.

## 3. Alterações publicadas nesta rodada

- `PinhoQuestVisuals.kt`: acrescentado `labelMaxLines` ao botão gráfico; por padrão os rótulos são limitados a duas linhas; rótulos de preferências são de uma linha com elipse.
- `QuestScreen.kt`: o cartão de resumo usa uma única coluna, os metadados têm seus próprios elementos de texto e os botões inferiores receberam margens/tamanhos menos propensos a colidir com os ícones.
- `GardenScreen.kt`: o cabeçalho usa a largura restante e organiza XP, nível e contagem em uma linha; o subtítulo tem quebra de linha limitada.
- `ProfileScreen.kt`: texto da placa do jardim é mais baixo e dimensionado para deixar a área das folhas livre; o conteúdo reserva mais espaço antes da barra inferior.
- `SettingsScreen.kt`: cor dos cabeçalhos e do texto auxiliar acompanha o tema; as ações verdes do modelo usam texto creme.
- `LargeFontLayoutTest.kt`: ampliados os asserts semânticos da Home e adicionado um teste de existência dos controles principais de Configurações numa escala de texto ampliada.

**Limite de validação:** a integração GitHub disponível compila testes instrumentados no workflow, mas o workflow não executa automaticamente `connectedDebugAndroidTest`. Os asserts adicionados protegem semântica/visibilidade de nós acessíveis à árvore Compose; não substituem screenshots e interação no aparelho.

## 4. Plano para concluir P6-E e preservar P6-C

### E-A11Y-01 — CI e regressão automatizada
Executar CI no SHA resultante das correções; corrigir falhas de compilação ou lint originadas pela mudança. Consultar o relatório completo de lint, sem silenciar avisos globalmente.

### E-A11Y-02 — Validar no dispositivo
Instalar o APK ligado ao SHA confirmado e capturar Home, Jardim, Perfil e Configurações em:
- tema claro e escuro;
- escala de fonte 1.0 e 1.3;
- viewport compacta;
- rolagem no início e no final das telas longas.

Confirmar especialmente o nome comprido do jardim, contagem alta de flores/XP, textos auxiliares e todas as ações por toque. Registrar resolução, densidade, escala, tema e SHA em cada evidência.

### E-A11Y-03 — Acessibilidade funcional
Revalidar alvos de toque de pelo menos 48 dp, nomes semânticos, estado selecionado, ordem de foco, contraste, elipse de rótulos longos, onboarding com teclado e conteúdo acima da barra inferior. P6-C fica aberto até os testes de IME, safe area, coleção e layout compacto serem observados no aparelho.

### E-A11Y-04 — Tipografia de display
A fonte enviada está identificada abaixo como bloqueada para incorporação até haver direitos explícitos de uso em aplicativo. Enquanto isso, manter a tipografia atual; não adicionar a fonte a `res/font` nem gerar APKs que a redistribuam. Quando houver uma licença que permita embedding/redistribution em apps, adicionar a fonte nos papéis de display aprovados, manter controles/labels funcionais legíveis e repetir a matriz de escala.

### E-A11Y-05 — Fechar gates por evidência
Só fechar P6-E com CI verde no SHA exato, evidência visual vinculada ao APK e matriz de acessibilidade concluída. Não confundir build/compilação com validação visual. P6-Final permanece bloqueado até P6-C e os P1 de P6-E terem evidência.

## 5. MV Boli — decisão de licença

O arquivo `docs/design/mv-boli-maisfontes.866a.zip` contém somente `mv-boli.ttf` e não inclui arquivo de licença ou autorização de incorporação. A página oficial do tipo de letra identifica MV Boli como © 2019 Microsoft Corporation e diz que é incluída exclusivamente com produtos/serviços Microsoft quando aplicável: [Microsoft Learn — MV Boli](https://learn.microsoft.com/en-us/typography/font-list/mv-boli).

A FAQ oficial de fontes da Microsoft declara que as fontes Windows não podem ser redistribuídas e que permissões de incorporação para documentos não autorizam incorporar a fonte num jogo/aplicativo: [Microsoft Learn — Font FAQ](https://learn.microsoft.com/en-us/typography/fonts/font-faq). Portanto, a simples presença do arquivo ZIP no repositório não prova direito de distribuição em APK.

**Decisão atual:** não incorporar nem empacotar MV Boli no APK nesta rodada. A integração fica **BLOCKED** até Rafa fornecer/confirmar uma licença que permita expressamente incorporação e distribuição em aplicativo, ou escolher uma fonte de licença compatível (por exemplo, uma fonte sob SIL Open Font License). O ZIP existente não é um dos 47 assets canônicos e o Gradle não deve incluí-lo por padrão.

## 6. Estado de gates após esta rodada

| Gate | Estado | Próxima evidência necessária |
|---|---|---|
| P6-B — composição visual | Aceito na rodada anterior | Não reabrir sem regressão reproduzível |
| P6-C — responsividade, IME e safe areas | **ABERTO** | Validação no aparelho em tela compacta, teclado, escalas e temas |
| P6-D — geração/fallback | Separado desta auditoria | Preservar a qualidade de modelo/fallback já documentada |
| P6-E — tipografia/acessibilidade/distribuição | **EM ANDAMENTO; Ihlimia integrada para teste; CI/runtime/licença pendentes** | CI + validação em aparelho + licença de fonte compatível |
| P6-Final | **BLOCKED** | Matriz de regressão e evidência final ligadas ao SHA |

Nenhum gate é declarado concluído por esta auditoria.


## 7. Follow-up — Ihlimia e correção da CI (2026-10-15)

### Falha reproduzida no log

A execução [#38603677089, job #115988990661](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38603677089/job/115988990661) falhou ao compilar `:app:compileDebugAndroidTestKotlin`:
- `assertExists` era importado/usado, mas não estava disponível na versão Compose UI Test configurada.
- `onNodeWithText` era chamado sem o import correspondente.

Correção publicada em `LargeFontLayoutTest.kt`: queries de coleção `onAllNodesWithText` e `onAllNodesWithContentDescription`, com `assertCountEquals`. A [Android CI #38684219225](https://github.com/sasandralean-prog/PinhoQuest/actions/runs/38684219225) passou no checkpoint de código `f67ea6501dfe657b9c3f9f421413a98d5a84eb07`; `syncDisplayFont`, compilação de AndroidTest, build do APK e lint concluíram com sucesso (0 erros, 38 avisos). Essa CI compila os testes instrumentados, mas não os executa em dispositivo.

### Fonte selecionada e aplicação

- Arquivo fonte: `docs/design/ihlimia-maisfontes.8b4f.zip`.
- Inspeção do ZIP encontrou somente `ihlimia.ttf` (413.348 bytes descompactados), sem licença/readme.
- `app/build.gradle.kts` extrai o TTF sozinho para uma pasta gerada de recursos Android; o ZIP não é copiado como recurso.
- A família `PinhoDisplayFontFamily` é aplicada apenas a títulos decorativos curtos no Início, Jardim e Perfil. Métricas, corpo de texto, quests geradas, botões e navegação mantêm as famílias anteriores.
- Ajustes de legibilidade adicionais: métricas do cartão Home aumentaram de 10sp para 11sp (line-height 13sp); rótulo do botão de backup reduziu de 18sp para 16sp e ganhou limite de duas linhas.
- Rafa identificou a fonte como livre. Como o ZIP não inclui licença, manter pendente a captura do texto de licença oficial/autor que permita explicitamente incorporar e redistribuir o arquivo binário num APK público. Uso comercial permitido por um diretório de fontes não prova necessariamente direito de redistribuir o TTF.

### Evidência ainda obrigatória

1. A CI #38684219225 passou no checkpoint de código `f67ea...`; ainda falta validação visual/funcional em aparelho em escala 1.0/1.3, temas claro/escuro, viewport compacta e fluxo IME.
2. Testar o APK dessa mesma revisão em tela compacta, tema claro/escuro e escalas 1.0/1.3.
3. Conferir nomes longos, métricas altas, alvos de toque, scroll, barra de navegação, e onboarding com IME aberto.
4. Manter P6-C, P6-E e P6-Final abertos até haver essa evidência.


## 8. Segunda rodada visual — navbar, cards e superfícies de texto (2026-10-15)

**Fonte:** sete novos screenshots enviados por Rafa após a primeira correção de tipografia. A validação visual indica melhoria geral, mas ainda mostra competição entre texto dinâmico e a arte de fundo.

### Observações dos screenshots

- **Navbar:** o texto ainda estava em fonte sans, ficava muito próximo da área de ilustração e o item selecionado usava dourado também em fundo claro, reduzindo o contraste.
- **Início:** o cartão do jardim não tinha espaço suficiente para métricas/nome/descrição; a arte incorporada incluía pequenas marcas decorativas sob o texto. O card de Quest de Jogo estava visualmente mais baixo/menor que o card adjacente.
- **Jardim:** mensagem de florescimento, XP e ação Abrir coleção estavam diretamente sobre o cenário; as cores competiam com regiões claras e escuras da ilustração conforme o tema.
- **Perfil:** o nome do jardim em fonte script ultrapassava visualmente a região inferior da placa e subia para a folhagem. A família manuscrita foi removida especificamente desse título; continua permitida em títulos curtos decorativos e na navbar conforme o requisito do proprietário.
- **Onboarding:** título, input, preview e botão estavam distribuídos sobre a paisagem sem uma superfície estável, e a posição do conteúdo aproximava o título da marca do fundo.
- **Seleção de temas:** o título “O que vamos descobrir?” invadia a área do wordmark, e a mensagem de temas selecionados aparecia sobre as flores.

### Alterações na branch

- Navbar: Ihlimia aplicada aos rótulos, alinhamento horizontal central preservado por destino e menor padding inferior para acomodar as legendas abaixo da ilustração. No tema claro, o item selecionado usa verde-escuro; no tema escuro, conserva o dourado.
- Início: o cartão do jardim agora ocupa toda a largura disponível da coluna e inclui inset de pergaminho para isolar métricas, nome e descrição. A coluna da Home reduziu margens horizontais de 22dp para 16dp. A proporção do botão de Quest de Jogo mudou de 174:86 para 174:100 e o rótulo aumentou para 15sp.
- Jardim: mensagem e métricas estão num painel de pergaminho; Abrir coleção ganhou uma superfície creme com contorno para não depender da cor do cenário.
- Perfil: título da placa usa serif legível a 16sp, 82% da largura útil e menor inset inferior.
- Onboarding: painel de pergaminho agrupa título, input, preview e confirmação; o conteúdo desceu levemente, o preview está centralizado e o botão fica centralizado independentemente.
- Seleção de temas: leve tonalização dependente do tema, título/helper em pergaminho e estado de seleção num segundo painel.
- Configurações: texto explicativo do cérebro criativo agora tem fundo de pergaminho; isso evita texto verde sobre detalhes do fundo desfocado.
- Todos os comportamentos de domínio, callbacks, validação do nome, IME Done e semântica de navegação foram preservados intencionalmente.

### Critérios de validação

- A CI #38690256884 passou no checkpoint de código `c2e8d05adaf0cff34e58ea81d88718ba74412440`, incluindo `syncDisplayFont`, testes unitários, lint, APK debug e compilação de AndroidTest. O log registrou 0 erros/38 avisos. Essa CI compila AndroidTest, mas não executa `connectedDebugAndroidTest`.
- Instalar o APK daquele SHA e recapturar Home, Garden, Profile, Settings, seleção de temas e onboarding.
- Repetir em tema claro/escuro, escala 1.0/1.3 e viewport compacta; incluir nome comprido, métricas grandes, área do teclado aberta e rolagem total.
- P6-C/P6-E continuam abertos até a execução visual/funcional no aparelho e o registro da licença de incorporação/redistribuição da Ihlimia antes do release.
