# PinhoQuest — Planejamento P4 → P7

**Documento:** Planejamento.md  
**Projeto:** PinhoQuest  
**Status:** planejamento formal consolidado  
**Escopo:** P4, P5, P6 e P7  
**Regra de publicação:** este documento é um artefato de planejamento local. Não publicar automaticamente no GitHub.

---

## 1. Propósito

Este documento formaliza o planejamento que vinha sendo construído de maneira incremental e informal para as próximas fronteiras do PinhoQuest.

A sequência conceitual é:

> **P4 descobre. P5 termina. P6 refina. P7 torna memorável.**

E, na formulação que guia o produto:

> **P5 termina o aplicativo.**  
> **P6 faz ele ficar gostoso.**  
> **P7 faz ele ficar memorável.**

Cada fase possui responsabilidade própria, fronteira arquitetural, critérios de validação e gate antes de avançar.

---

# 2. Visão do produto

PinhoQuest é uma aplicação de microaventuras para momentos de tédio ou baixa energia.

O objetivo não é maximizar retenção, produtividade ou pressão comportamental. É oferecer uma pequena atividade interessante, leve e segura, com sensação de descoberta e recompensa tranquila.

### Princípios

- baixa fricção;
- descoberta sem pressão;
- interface positiva e informal;
- sensação de pequeno refúgio;
- jardim como espaço de continuidade;
- quests curtas e compreensíveis;
- ausência de FOMO;
- ausência de streaks punitivos;
- ausência de lootboxes;
- ausência de mecânicas manipulativas;
- internet para descoberta/enriquecimento;
- LLM como compositor criativo, não autoridade factual;
- Room como autoridade do estado persistente;
- backup como snapshot, não segunda base;
- comportamento adaptativo em vez de hacks por dispositivo;
- evidência antes de declarar uma fase concluída.

---

# 3. Arquitetura conceitual

```text
INTERNET
   │
   ▼
RESEARCH LAYER
jogos / flores / fontes
   │
   ▼
DOMAIN / ROOM
estado / catálogo / progresso
   │
   ├───────────────┐
   ▼               ▼
QUEST COMPOSER    GARDEN
LLM criativo      estado + flores
                     │
                     ▼
              FLOWER ARTWORK
              determinístico
              IA opcional
```

### Autoridades

| Domínio | Autoridade |
|---|---|
| Estado persistente | Room |
| Backup | snapshot validado |
| Fatos de pesquisa | evidências/proveniência |
| Composição textual | LLM |
| Regras de domínio | código determinístico |
| Arte de flores | `FlowerArtworkGenerator` |
| Aparência/configuração | preferências persistentes |
| E2E | comportamento observável |

O LLM não inventa fatos para preencher lacunas. A arte não vira autoridade do domínio. O backup não vira um segundo banco.

---

# 4. Roadmap

## P4 — Web Research

**Objetivo:** descobrir e enriquecer conteúdo real através da web sem transformar a web em dependência frágil ou autoridade sem validação.

## P5 — Finish the App

**Objetivo:** transformar as funcionalidades em uma V1 operacional, persistente, recuperável, compreensível e validada.

## P6 — Make It Feel Good

**Objetivo:** transformar uma aplicação funcional em uma experiência agradável, coerente e emocionalmente alinhada com a proposta.

## P7 — Living Garden

**Objetivo:** transformar o jardim em uma experiência memorável de descoberta e coleção baseada em flores reais, pesquisa, proveniência e pixel art.

---

# 5. P4 — Web Research

## 5.1 Fluxo geral

```text
pedido de descoberta
      ↓
pesquisa
      ↓
resultado bruto
      ↓
validação semântica
      ↓
proveniência
      ↓
normalização
      ↓
deduplicação
      ↓
catálogo persistente
      ↓
seleção
      ↓
quest
```

Pesquisa é fonte externa e pode falhar.

## 5.2 Pesquisa de jogos

Fluxo:

```text
Quest online
  ↓
Game Research
  ↓
Adapter(s)
  ↓
resultado normalizado
  ↓
validação
  ↓
provenance + timestamp
  ↓
dedupe
  ↓
GameDiscoveryCatalog
  ↓
seleção
  ↓
quest
```

Cada item deve possuir informação suficiente para saber de onde veio, quando foi pesquisado, plataforma, gênero, disponibilidade relevante e identidade para deduplicação.

## 5.3 Estados semânticos

Usar:

```text
Success
Unavailable
RateLimited
InvalidResponse
UnsupportedSource
TechnicalFailure
```

Não confundir "não encontrei nada" com "a fonte falhou".

## 5.4 Fluxo offline

```text
OFFLINE
  ↓
catálogo persistido
  ↓
opções elegíveis
  ↓
seleção
  ↓
quest
```

Catálogo sem opção nova deve produzir resultado semântico de **no new options**, não erro técnico.

---

# 6. P4 — Pesquisa de flores

O objetivo não é simplesmente encontrar imagens, mas obter entidades botânicas identificáveis e evidências suficientes para sustentar o que o aplicativo afirma.

Quando possível, registrar:

- nome;
- nome científico;
- fonte;
- timestamp;
- licença/proveniência;
- dados relevantes para catalogação;
- estado de confiança.

Quando não houver certeza:

> **UNKNOWN é preferível a uma afirmação inventada.**

---

# 7. P4 — Garden expansion

State machine:

```text
NOT_REQUIRED
     ↓
  PENDING
     ↓
RESEARCHING
   ↙       ↘
READY   FAILED_RETRYABLE
```

Uma falha recuperável não destrói o catálogo válido anterior.

---

# 8. P4 — CatalogPack

Catálogo base de aproximadamente 20–30 flores reais, versionado e suficientemente estável para funcionar sem nova pesquisa a cada abertura.

---

# 9. P4 — Gate

### Jogos

- [ ] adapters funcionando;
- [ ] resultados normalizados;
- [ ] provenance e timestamp;
- [ ] deduplicação;
- [ ] estados semânticos;
- [ ] fluxo online;
- [ ] fluxo offline;
- [ ] catálogo persistente;
- [ ] catálogo vazio tratado semanticamente.

### Flores

- [ ] pesquisa baseada em evidências;
- [ ] UNKNOWN quando necessário;
- [ ] provenance preservada;
- [ ] catálogo inicial definido;
- [ ] state machine de expansão;
- [ ] falha recuperável preserva estado válido.

### Gate P4

**Só avançar quando os fluxos online/offline e o catálogo de flores estiverem semanticamente validados.**

---

# 10. P5 — Finish the App

A pergunta de P5 é:

> "Se eu instalar hoje, consigo usar, salvar, fechar, voltar, fazer backup, restaurar e entender o que aconteceu quando algo falhar?"

P5 fecha a fundação. Não é a fase de grande embelezamento.

---

# 11. P5 — Backup

Backup é **snapshot**, não segunda base de dados.

```text
ROOM
  ↓
snapshot
  ↓
JSON / ZIP
```

Restore:

```text
backup
  ↓
reconhecimento
  ↓
validação
  ↓
integridade
  ↓
RestorePlanner
  ↓
RoomRestoreService
  ↓
Room
```

Manifesto conceitual:

- `formatVersion`
- `schemaVersion`
- `profileId`
- `datasetRevision`
- `createdAt`
- `appVersion`
- `catalogVersions`
- `integrityHash`

---

# 12. P5 — Backup privado e público

### Privado

Rotação local de snapshots.

### Público/manual

Exportação via SAF para o destino escolhido pelo usuário.

O aplicativo pode gerar um ZIP reconhecível como backup PinhoQuest e o usuário pode armazená-lo no Google Drive ou outro local. Não é necessário transformar o app em cliente de Google Drive.

---

# 13. P5 — Restore

Antes de alterar Room:

```text
arquivo
  ↓
parse
  ↓
validar formato
  ↓
validar schema
  ↓
validar versões
  ↓
validar hash
  ↓
RestorePlanner
  ↓
decisão
  ↓
RoomRestoreService
```

Backup inválido não pode modificar o estado atual.

---

# 14. P5 — Configurações e acessibilidade

Fechar a V1 com opções reais e persistentes, incluindo:

- tema;
- tamanho de fonte;
- cor/highlight quando aplicável;
- preferências de aparência;
- legibilidade.

Não criar opções apenas como fachada.

---

# 15. P5 — Erros humanizados

Separar:

```text
falha técnica/log
        +
mensagem humana
```

O usuário deve entender o que ocorreu, se perdeu algo, se pode tentar novamente e se existe alternativa offline.

---

# 16. P5 — E2E

O gate deve atravessar o comportamento observável:

```text
abrir
 ↓
criar/receber quest
 ↓
interagir
 ↓
alterar progresso
 ↓
alterar jardim
 ↓
persistir
 ↓
fechar/reabrir
 ↓
verificar
 ↓
exportar backup
 ↓
validar
 ↓
restaurar
 ↓
verificar novamente
```

Não basta validar apenas métodos isolados.

---

# 17. P5 — Gate

- [ ] backup local;
- [ ] exportação SAF;
- [ ] manifesto;
- [ ] schema/versionamento;
- [ ] integridade;
- [ ] restore planner;
- [ ] restore seguro;
- [ ] preferências persistentes;
- [ ] acessibilidade;
- [ ] erros humanizados;
- [ ] E2E principal;
- [ ] E2E de persistência;
- [ ] E2E de backup/restore;
- [ ] documentação baseada em evidência.

### Gate P5

> **V1 funcional fechada.**

Depois disso, novas mudanças são tratadas principalmente como refinamento/produto, não como tentativa de terminar a fundação.

---

# 18. P6 — Make It Feel Good

A pergunta muda para:

> **"É gostoso usar?"**

P6 não deve alterar arquitetura por estética. O objetivo é melhorar a experiência preservando contratos funcionais.

---

# 19. P6 — Direção visual

A identidade deve transmitir:

- suavidade;
- paz;
- jardinagem;
- aconchego;
- descoberta;
- leveza;
- amizade;
- segurança;
- sensação de pequeno refúgio.

O aplicativo não deve parecer painel corporativo ou ferramenta de produtividade.

---

# 20. P6 — UX

### Quest

- hierarquia textual;
- leitura confortável;
- missão clara;
- tempo/dificuldade fáceis de entender;
- convite, não obrigação.

### Tags

- organização simples;
- visual leve;
- feedback claro.

### Garden

- espaço vivo;
- descoberta;
- continuidade;
- progresso sem pressão.

### Settings

- linguagem humana;
- controles compreensíveis;
- coerência visual.

---

# 21. P6 — Microinterações

Podem incluir:

- transições suaves;
- feedback de conclusão;
- pequenas animações;
- estados vazios acolhedores;
- carregamento menos técnico;
- feedback visual de progresso;
- pequenas celebrações.

Nenhuma microinteração deve criar pressão comportamental.

---

# 22. P6 — Doações

A área de doações pode entrar na productização.

Princípios:

- opcional;
- transparente;
- sem bloquear funcionalidades;
- sem culpa;
- sem pressão;
- coerente com projeto independente.

---

# 23. P6 — Distribuição

Preparar:

- identidade;
- screenshots;
- descrição;
- ícone;
- acabamento de telas;
- textos;
- consistência da primeira impressão.

---

# 24. P6 — Gate

- [ ] identidade visual consistente;
- [ ] telas principais refinadas;
- [ ] estados vazios;
- [ ] loading/error/success;
- [ ] tipografia/espaçamento;
- [ ] acessibilidade preservada;
- [ ] microinterações validadas;
- [ ] doações isoladas e opcionais;
- [ ] fluxo completo continua funcional;
- [ ] sem regressão funcional;
- [ ] material de distribuição representa a versão real;
- [ ] revisão UX concluída.

### Gate P6

> **O PinhoQuest funciona e transmite a sensação que deveria transmitir.**

---

# 25. P7 — Living Garden

A ideia:

> **flores reais → descoberta → pesquisa → identidade → pixel art → coleção → jardim**

A flor deixa de ser apenas um ícone e passa a representar uma pequena descoberta do mundo real.

---

# 26. P7 — Fluxo da flor

```text
flor real
  ↓
pesquisa
  ↓
identificação
  ↓
proveniência
  ↓
imagem de referência
  ↓
normalização
  ↓
pixelização
  ↓
FlowerArtwork
  ↓
Garden
```

Fato e arte são responsabilidades diferentes.

---

# 27. P7 — Direitos e proveniência

Google Images/public visibility não significa domínio público.

Preferir fontes com licença clara, domínio público ou permissão compatível.

Registrar, quando aplicável:

- fonte;
- origem;
- autor;
- licença;
- atribuição;
- timestamp;
- identificador da referência.

Transformar uma imagem em pixel art não elimina automaticamente obrigações de copyright.

---

# 28. P7 — Pixel art determinística

Pipeline:

```text
imagem
  ↓
crop / normalize
  ↓
resize
  ↓
palette reduction
  ↓
quantization
  ↓
dithering
  ↓
pixelization
  ↓
FlowerArtwork
```

Mesma entrada + mesma configuração = mesmo resultado.

Benefícios:

- testes;
- cache;
- reprodução;
- versionamento;
- comparação;
- debugging.

---

# 29. P7 — Renderer

Contrato:

```text
FlowerArtworkGenerator
        │
        ├── DeterministicPixelRenderer
        │
        └── AiPixelRenderer (futuro/opcional)
```

O Garden consome `FlowerArtwork`, não conhece detalhes do renderer.

---

# 30. P7 — IA de imagem

IA de imagem pode ser enriquecimento futuro, não requisito da primeira versão.

Razões:

- custo;
- dependência externa;
- imprevisibilidade;
- peso;
- latência;
- reprodutibilidade;
- governança.

Regra:

> **renderer determinístico primeiro; IA opcional depois.**

---

# 31. P7 — Raridade

Catálogo inicial de aproximadamente 20–30 flores.

Categorias:

- Comum;
- Incomum;
- Rara;
- Raríssima.

A raridade deve refletir uma estimativa de ocorrência/globalidade em escala apropriada, idealmente logarítmica, e não simplesmente probabilidades iguais artificiais.

---

# 32. P7 — Progressão

O jardim pode ter:

- catálogo parcialmente visível;
- itens ocultos;
- desbloqueio por XP;
- informação progressivamente revelada;
- flores como drops de uma vez;
- coleção persistente;
- artwork associado a IDs estáveis.

O objetivo é curiosidade, não grind.

---

# 33. P7 — Garden / Room

Separação conceitual:

```text
FlowerDefinition
FlowerDiscovery
FlowerArtwork
GardenPlacement
```

A nomenclatura final pode variar.

A regra permanece: Room é autoridade do estado; renderização não deve ser embutida no domínio do jardim.

---

# 34. P7 — Gate

- [ ] catálogo real;
- [ ] provenance;
- [ ] licença/atribuição;
- [ ] artwork determinístico;
- [ ] reprodução determinística;
- [ ] `FlowerArtworkGenerator` desacoplado;
- [ ] Garden consome artwork sem conhecer renderer;
- [ ] raridade documentada;
- [ ] descoberta/progressão;
- [ ] persistência;
- [ ] backup preservado/reconstruível;
- [ ] E2E do ciclo da flor;
- [ ] IA externa não obrigatória;
- [ ] identidade visual preservada;
- [ ] experiência continua relaxante.

### Gate P7

> **Living Garden validado.**

---

# 35. Fluxo integrado P4 → P7

```text
P4 — WEB RESEARCH
      │
      ├── jogos → GameCatalog
      └── flores → CatalogPack
                    │
                    ▼
P5 — FINISH THE APP
      │
      ├── persistência
      ├── backup
      ├── restore
      └── E2E
                    │
                    ▼
P6 — MAKE IT FEEL GOOD
      │
      ├── visual
      ├── UX
      ├── microinterações
      └── distribuição
                    │
                    ▼
P7 — LIVING GARDEN
      │
      ├── flores reais
      ├── provenance
      ├── pixel art
      └── descoberta
```

---

# 36. Filosofia de validação

Cada gate deve responder:

1. A funcionalidade existe?
2. O contrato está correto?
3. O comportamento real corresponde ao contrato?
4. Estados de erro têm semântica?
5. Persistência continua íntegra?
6. O fluxo funciona ponta a ponta?
7. Nenhuma segunda autoridade foi criada?
8. A documentação corresponde ao estado real?

---

# 37. Tipos de validação

## Unit

Regras isoladas:

- normalização;
- deduplicação;
- state machines;
- backup codec;
- hash;
- raridade;
- pixelização determinística.

## Integration

Fronteiras:

- pesquisa → catálogo;
- catálogo → quest;
- backup → restore;
- flower definition → artwork;
- artwork → garden.

## Semantic

Significado:

- `Unavailable` não vira `empty`;
- `UNKNOWN` não vira fato inventado;
- quest não vaza instruções técnicas;
- objectives são úteis;
- mensagens de erro são humanas.

## E2E

Comportamento real da aplicação atravessando suas fronteiras.

---

# 38. Stop conditions

Uma fase deve permanecer `implemented_unvalidated` quando a evidência não for suficiente.

Não fazer:

- "passou o teste, então deve estar certo";
- esconder saída ruim com sanitização;
- transformar falha semântica em sucesso;
- criar workaround por dispositivo;
- duplicar fluxo para contornar arquitetura;
- mudar contrato apenas para fazer teste passar;
- declarar gate fechado sem evidência.

Quando houver falha:

```text
falha
  ↓
classificar
  ↓
determinar camada responsável
  ↓
corrigir causa
  ↓
revalidar
  ↓
avançar somente com evidência
```

---

# 39. Documentação

Ao concluir cada checkpoint:

1. registrar o que mudou;
2. registrar evidências;
3. registrar testes;
4. registrar limitações;
5. registrar decisões arquiteturais;
6. atualizar Engineering Genome quando a decisão for estrutural;
7. atualizar README/contexto quando o comportamento público mudar.

A documentação não deve afirmar conclusão sem evidência.

---

# 40. Fronteira explícita

| Fase | Pergunta principal | Não é responsabilidade principal |
|---|---|---|
| P4 | Como descobrimos conteúdo real com segurança? | acabamento visual |
| P5 | O aplicativo está fechado e recuperável? | grande redesign |
| P6 | É gostoso usar? | novo subsistema factual |
| P7 | O jardim pode virar algo memorável? | reconstruir toda a arquitetura |

Essa separação existe para impedir escopo infinito.

---

# 41. Ordem de execução

```text
P4
 ↓
Gate P4
 ↓
P5
 ↓
Gate P5
 ↓
P6
 ↓
Gate P6
 ↓
P7
 ↓
Gate P7
```

Nenhuma fase fecha automaticamente porque a anterior terminou.

---

# 42. Critério de sucesso global

Ao final de P7:

### Produto

- [ ] fácil começar;
- [ ] quests leves;
- [ ] sem pressão;
- [ ] jardim transmite continuidade;
- [ ] experiência acolhedora.

### Técnica

- [ ] Room é autoridade;
- [ ] backup é snapshot;
- [ ] restore é validado;
- [ ] web possui estados semânticos;
- [ ] LLM não é autoridade factual;
- [ ] artwork é desacoplado;
- [ ] E2E valida comportamento real;
- [ ] arquitetura permanece portátil/adaptável.

### Experiência

- [ ] visual coerente;
- [ ] sensação de paz;
- [ ] jardim com identidade;
- [ ] flores parecem descobertas;
- [ ] pixel art possui personalidade;
- [ ] experiência continua agradável sem internet quando possível.

---

# 43. Resumo executivo

### P4 — DESCUBRIR
Pesquisa real, provenance, catálogo, estados semânticos, online/offline e base factual das flores.

### P5 — TERMINAR
Backup, restore, configurações, acessibilidade, erros humanos, E2E e fechamento da V1.
### P6 — GOSTAR
Visual, UX, microinterações, identidade, distribuição e doações opcionais.

### P7 — LEMBRAR
Living Garden, flores reais, provenance, pixel art determinística, raridade, descoberta e coleção.

---

# 44. Fórmula final

> **P4 dá conhecimento ao PinhoQuest.**  
> **P5 dá confiabilidade ao PinhoQuest.**  
> **P6 dá personalidade ao PinhoQuest.**  
> **P7 dá memória ao PinhoQuest.**

E a sequência maior permanece:

> **P5 termina o aplicativo.**  
> **P6 faz ele ficar gostoso.**  
> **P7 faz ele ficar memorável.**