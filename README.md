# Pinho Quest

Pinho Quest é um gerador pessoal de microaventuras para quando bater o “tô entediado”.

A proposta é simples: sortear algo legal para fazer agora — programar, jogar, criar, explorar, aprender ou experimentar alguma coisa diferente — com personalização local, quests offline, progressão por XP e um jardim de flores reais colecionáveis.

## Estado atual

O **design V1 foi aprovado** e a implementação funcional de Pinho Quest já está em andamento avançado. P5 foi fechado no gate funcional em 2026-10-05, com testes de unidade, Android-data e 10/10 testes de UI no Pixel 4 API 33. A integração local LiteRT-LM/CR-9 também passou o E2E produtivo. O próximo trabalho deve seguir `CURRENT_STATE.md` e o Engineering Genome; o README não substitui os gates técnicos.

## Leia primeiro

- [Design V1 canônico](docs/superpowers/specs/2026-10-01-pinho-quest-v1-design.md)
- [Roadmap de implementação V1](docs/superpowers/plans/2026-10-01-pinho-quest-v1-roadmap.md)
- [Mapa de arquitetura](architecture/PINHO_QUEST_V1_ARCHITECTURE.md)
- [Engineering Genome](Engineering_Genome/00_START_HERE.md)
- [Mapa de autoridades](Engineering_Genome/02_AUTHORITY_MAP.md)
- [Handoff Rafa–Lúcio](Engineering_Genome/10_RAFA_LUCIO_HANDOFF.md)

## Princípios rápidos

- Android primeiro, core Kotlin/JVM portátil para futuro Windows.
- LLM local como compositor criativo, com fallback procedural.
- Internet desde a V1 para descoberta de jogos e pesquisa de flores reais.
- Room como autoridade do estado vivo.
- Backups privados e públicos como snapshots.
- Nada de streak, FOMO, lootbox ou retenção punitiva.
- UX afável, otimista e verdadeira.

## Modelo LiteRT-LM (CR-7.4)

O APK normal contém a runtime JNI LiteRT-LM, mas não contém os 284,7 MB do
modelo criativo. O pacote CR-7.4 é uma distribuição versionada de release,
instalada somente após consentimento explícito em **Configurações**. Consulte o
[registro do artefato CR-7.4](docs/model-artifacts/CR74_SEMANTIC_ISOLATION.md)
para URL imutável, SHA-256, tamanho, licença, recuperação em checkout limpo e
limites de validação. Não substitua o pacote por um arquivo de mesmo nome: o
instalador valida tamanho e SHA-256 antes de ativá-lo.

## Engenharia do P6

O ponto oficial de continuidade da refatoração visual é
[`Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md`](Engineering_Genome/P6_IMPLEMENTATION_AND_TRANSITION_PROTOCOL.md).
Ele define os gates P6-A..P6-E, a ordem documental, critérios de validação e política de checkpoints. A existência do protocolo não significa que a implementação ou os gates estejam concluídos.

Para referência visual e de implementação, consulte também:
- [Catálogo canônico de assets](docs/design/ASSET_CATALOG.md)
- [Registry de referências visuais](docs/design/CANONICAL_GRAPHICS.md)
- [Contrato visual de implementação](docs/design/UI_DESIGN_CONTRACT.md)
- [Contrato de interação P6](docs/design/P6_UI_UX_INTERACTION_CONTRACT.md)
