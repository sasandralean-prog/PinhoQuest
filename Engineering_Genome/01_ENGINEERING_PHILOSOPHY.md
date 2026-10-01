# Engineering Genome — 01 Engineering Philosophy

## 1. Work from authority, not convenience

Before implementing or modifying behavior:

1. identify the canonical owner;
2. identify the input contract;
3. identify the output contract;
4. identify persistence authority;
5. identify failure semantics;
6. identify the tests that protect behavior.

If two components appear to own the same responsibility, stop and resolve ownership before adding another path.

## 2. Investigation protocol

Use this sequence for defects and regressions:

```text
Observation
→ Evidence
→ Reproduction
→ Flow anatomy
→ Hypothesis
→ Test
→ Intervention
→ Regression
→ E2E
```

Always distinguish:
- observed fact;
- inference;
- hypothesis.

Trace information from origin to consumption. Ask where it is transformed, duplicated, discarded or silently replaced.

## 3. Refactoring protocol

Before decomposition:
- inventory current responsibilities;
- inventory user-visible behavior;
- add characterization tests when behavior is not already protected;
- identify implicit state and dependencies;
- preserve canonical contracts.

Do not use refactoring as permission to silently delete behavior.

## 4. Hardcode policy

Hardcodes are not automatically defects.

Classify them:

- **policy constant** — intentional and versioned;
- **protocol constant** — required by an external contract;
- **UI copy/default** — localized/configured appropriately;
- **historical workaround** — investigate before preserving;
- **device/provider special case** — suspect unless justified by a real contract.

Prefer runtime capability probes, versioned policies and configuration over device-name branching.

Do not replace an intentional bounded policy with “dynamic” behavior unless equivalence and benefit are demonstrated.

## 5. No bypass rule

Do not solve a blocked flow by:
- skipping validation;
- creating a second path around the canonical owner;
- writing directly from UI to persistence;
- letting the LLM manufacture a domain object that bypasses validation;
- treating a fallback as a new independent runtime.

A fallback must converge back into the same validation/persistence authorities.

## 6. Data truth

Pinho Quest distinguishes:
- generated creative text;
- user-provided data;
- external observed facts;
- derived policy outputs.

External facts retain provenance.

Creative text may enrich facts but must not overwrite or impersonate evidence.

## 7. Tests

Tests protect behavior and contracts.

Prefer:
- unit tests for policies and domain state;
- integration tests for adapters and transactions;
- characterization tests before risky refactors;
- E2E tests for the complete user-visible flow.

Do not contort architecture merely to satisfy a brittle test. If a test contradicts the intended contract, fix the test after documenting why.

## 8. Checkpoints

Prefer small, verifiable checkpoints.

Each meaningful checkpoint should state:
- objective;
- changed authority/contract;
- evidence;
- tests;
- known limitation;
- next frontier.

Do not call a checkpoint validated when its required runtime evidence is missing.

## 9. User experience as architecture

Humanized UI is not permission to hide system truth.

The user-facing layer should translate technical state into plain language while preserving meaning.

Example:

Bad:
> Tudo certo!

when backup failed.

Good:
> Seu jardim continua salvo neste aparelho. Não consegui atualizar a cópia na pasta escolhida desta vez.

## 10. Product restraint

Pinho Quest exists to suggest something to do, not to capture attention.

Do not add engagement mechanics merely because they are common in games.

Any future mechanic that increases retention pressure must be justified against the product intent in the canonical spec.
