# P6-A — Registro de baseline visual e inventário de evidências

**Data do registro:** 2026-10-10  
**Branch do app informada pelo proprietário:** `feature/p6-total-ui-refactor`  
**Origem das capturas:** screenshots do APK funcional enviadas na conversa em 2026-10-10, com nomes de arquivo datados de 2026-10-09.  
**Limite:** as imagens foram usadas para diagnóstico, mas seus binários não foram adicionados a este commit. A correspondência do APK a um SHA exato continua sem hash de pacote informado.

## Evidências recebidas

| Arquivo recebido | Estado observado | Área de código a inspecionar |
|---|---|---|
| `Screenshot_20261009-212943.png` | Abertura com background 9:16 e botão Começar | `OnboardingScreen.kt` / `PinhoQuestBackground` / `PinhoGraphicButton` |
| `Screenshot_20261009-212959.png` | Onboarding para nome do jardim; name bar e texto | `OnboardingScreen.kt` |
| `Screenshot_20261009-213031.png` | Onboarding de nome com teclado aberto e CTA inferior | `OnboardingScreen.kt`, IME e área rolável |
| `Screenshot_20261009-213108.png` | Seleção de nove preferências; CTA próximo da última linha | `OnboardingScreen.kt` / `PinhoTagGraphicButton` |
| `Screenshot_20261009-213135.png` | Home em tema noite; motivos do card e texto sobreposto | `QuestScreen.kt` / `GardenSummaryCard` |
| `Screenshot_20261009-213227.png` | Jardim em arte, resumo e acesso à coleção | `GardenScreen.kt` / `GardenArtState` |
| `Screenshot_20261009-213243.png` | Coleção com labels duplicados, filtros e grade próximos da navbar | `GardenScreen.kt` / `GardenFlowerCard` / `GardenFilterChip` |
| `Screenshot_20261009-213316.png` | Perfil com painel de preferências, tags ilustradas e neutras | `ProfileScreen.kt` / `PinhoTagGraphicButton` |
| `Screenshot_20261009-213329.png` | Configurações; labels sobre temas, fonte e botões | `SettingsScreen.kt` / `PinhoGraphicButton` |
| `Screenshot_20261009-213428.png` | Quest gerada; texto sobre a marca incorporada ao background e label do CTA | `QuestScreen.kt` / `GeneratedQuestContent` |

## Sintomas visuais priorizados para comparação futura

1. Card Home: ícones/emojis flor e estrela desenhados em Compose na mesma área de motivos já presentes na arte; texto pode avançar para a região ilustrada à direita.
2. Botões: label invade a folha decorativa de `btn_confirm.png` e outros assets com área esquerda ilustrada.
3. Navbar: casa/flor/livro são desenhados no PNG e novamente pelo componente Compose.
4. Coleção: o card de flor desconhecida contém “?” e lettering de desconhecida, mas o componente volta a desenhá-los; cards são mais altos/largos de modo incompatível com canvas original e a grade pode disputar altura com os filtros.
5. Onboarding: o layout usa grandes espaçadores proporcionais; teclado aberto e última linha de categorias podem empurrar o CTA para fora da área útil.
6. Home/Quest: o background start contém a marca; o conteúdo de quest gerada/ativa precisa preservar essa zona.
7. Settings/empty-garden: parâmetros de proporção no Compose continuam baseados em dimensões antigas em pelo menos alguns consumidores.
8. Categorias: o código mapeia cinco IDs às imagens; outras preferências têm superfícies neutras e não devem ser associadas por aproximação lexical.

## Estado da evidência

- A inspeção de código para os componentes citados foi feita na branch P6.
- Estas capturas funcionam como baseline sintomático, fornecido pelo proprietário; não provam sozinhas que o binário corresponde a um SHA específico.
- Os arquivos de imagem não foram copiados para o repositório neste commit documental. Para regressão reproduzível de P6-B/P6-C, gerar e armazenar novas capturas do APK produzido pelo CI/checkout com SHA explícito e anotar resolução, escala de fonte, tema e estado de dados.
- Não há alegação de que qualquer defeito desta lista já foi corrigido. O código de runtime permanece inalterado nesta etapa.
