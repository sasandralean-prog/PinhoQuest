# Trilha sonora original do PinhoQuest

Este diretório preserva os quatro MP3s originais fornecidos pelo responsável pelo projeto. Os nomes são mantidos como enviados; cópias com nomes estáveis são empacotadas em `app/src/main/assets/music/`.

## Mapeamento de reprodução

| Ordem | Arquivo original | Arquivo no app | Papel musical |
|---|---|---|---|
| 1 | `PinhoQuest - jardim.mp3` | `jardim.mp3` | Jardim Sereno — tema principal |
| 2 | `PinhoQuest - exploração .mp3` | `exploracao.mp3` | Trilhas de Musgo — exploração |
| 3 | `PinhoQuest - magia.mp3` | `segredo_folhas.mp3` | Segredo entre as Folhas — descoberta mágica |
| 4 | `PinhoQuest - garden.mp3` | `jardim_descansa.mp3` | O Jardim Descansa — repouso |

A playlist percorre as quatro faixas em ordem e reinicia ao chegar ao fim. O player global não é recriado ao trocar de tela. O áudio usa AndroidX Media3/ExoPlayer, respeita o ciclo de vida da Activity, solicita foco de áudio, reage à desconexão de fones e oferece chave liga/desliga e controle de volume persistentes nas Configurações.

## Formato e integração

Os quatro originais foram verificados como MP3 estéreo, 48 kHz; duração aproximada por faixa conforme o mapeamento acima: 2:16, 2:25, 2:47 e 2:52. A cópia para os assets usa os mesmos bytes dos originais. O volume inicial do jogo é 18% (limite configurado de 35%) para manter a trilha de fundo discreta.

Os fades presentes nas próprias faixas podem se somar suavemente à transição do app. O player reduz o volume nos últimos 1,5 s de cada faixa e aumenta o volume nos primeiros 1,5 s da próxima; também faz fade ao iniciar/retomar ou desativar/pausar a reprodução. A playlist avança e repete automaticamente. A transição é fade-out/fade-in sem sobreposição dos dois áudios (não é um crossfade com duas faixas tocando simultaneamente).

## Licença e procedência

As faixas foram geradas e fornecidas pelo responsável pelo PinhoQuest usando Suno Pro. Antes de distribuir publicamente ou monetizar uma versão do jogo, confirme que os termos comerciais aplicáveis à conta e à data de geração permitem o uso e a redistribuição dos arquivos incorporados no APK. Esta nota registra a procedência informada, não constitui garantia jurídica de licença.
