# P3 Micro Quest Composer

Date: 2026-10-02
Branch: `feature/p3-micro-quest-composer`
Baseline: `647ffc7`

## Architecture
`QuestGenerationPlan` remains the source of truth for category, environment, difficulty and duration.
`MicroQuestComposer` delegates only humanized text to a local model.
`FunctionGemmaResponseSanitizer` accepts only a tightly bounded text payload.
`MicroQuestRenderer` reconstructs a trusted `QuestDraft` from model text plus the original plan.
`ProceduralComposer` remains the complete fallback.

## Contract
Model-owned fields: `title`, `description`, `objectives`.
Deterministic fields: `category`, `environment`, `difficulty`, `estimatedDuration`, XP/rewards and future governed metadata.
Examples are supplied dynamically from approved procedural quests; this is few-shot guidance, not fine-tuning.

## Runtime compatibility gate
LiteRT-LM 0.17.1 requires a Java 21-compatible build and reports Kotlin metadata 2.4.0.
The PinhoQuest production project uses Kotlin 2.1.21 and Java 17.
Directly adding the LiteRT-LM AAR to production caused Kotlin metadata/compiler failure, so the dependency was removed again.
The isolated harness compiles with Android Studio JBR 21.0.10 and remains the approved experimental runtime vehicle.

## FunctionGemma smoke — 2026-10-02
Artifact: `todolist-functiongemma_q8_ekv1024.litertlm`.
Five fixtures completed with zero runtime exceptions.
Observed latency: 4536–5302 ms per fixture.
Observed PSS during generation: 643137–702423 KiB.
Model initialization: 2655 ms; initial PSS 344920 KiB.

However, all observed outputs were protocol/function-oriented rather than clean quest text.
Responses contained `<start_of_turn>model`, `<escape>`, malformed argument-like fragments and repeated function-declaration content.
The attempted JSON `ResponseFormat` did not make FunctionGemma behave as a general JSON composer.

## Decision
Do not wire FunctionGemma into `PinhoQuestAppGraph` yet.
Do not broaden the sanitizer into generic regex repair.
The runtime/memory profile is promising, but the model contract must align with FunctionGemma's native function-calling semantics.

## Next gate
Benchmark a native function-call contract with one explicit function such as `compose_quest_text(title, description, objectives)` and extract only its validated arguments.
Use deterministic procedural examples as few-shot context.
Only after native extraction succeeds consistently should the Android runtime adapter and QuestEngine path be considered for production integration.
