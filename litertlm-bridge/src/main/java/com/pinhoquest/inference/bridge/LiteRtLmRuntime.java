package com.pinhoquest.inference.bridge;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.SamplerConfig;
import com.google.ai.edge.litertlm.ThinkingConfig;
import com.google.ai.edge.litertlm.ToolKt;
import com.google.ai.edge.litertlm.ToolProvider;
import com.pinhoquest.core.inference.GenerationRequest;
import com.pinhoquest.core.inference.InferenceBudget;
import com.pinhoquest.core.inference.InferenceOutcome;
import com.pinhoquest.core.inference.InferenceStopReason;
import com.pinhoquest.core.inference.InferenceTelemetry;
import com.pinhoquest.core.inference.InferenceTelemetrySink;

import java.util.Collections;
import java.util.List;

public final class LiteRtLmRuntime implements AutoCloseable {
    private final Engine engine;
    private final InferenceBudget budget;
    private final InferenceTelemetrySink telemetrySink;
    private volatile boolean initialized = false;

    public LiteRtLmRuntime(
            String modelPath,
            String cacheDir,
            InferenceBudget budget,
            InferenceTelemetrySink telemetrySink) {
        this.budget = budget;
        this.telemetrySink = telemetrySink;
        this.engine = new Engine(new EngineConfig(
                modelPath,
                new Backend.CPU(),
                null,
                null,
                budget.getMaxContextTokens(),
                null,
                cacheDir));
    }

    public InferenceOutcome generate(GenerationRequest request) {
        int beforeTokens = -1;
        int afterTokens = -1;
        int observedToolCalls = 0;
        InferenceStopReason stopReason = InferenceStopReason.TECHNICAL_FAILURE;

        try {
            if (!budget.equals(request.getBudget())) {
                stopReason = InferenceStopReason.BUDGET_REJECTED;
                return InferenceOutcome.TechnicalFailure.INSTANCE;
            }

            ensureInitialized();
            try (Conversation conversation = createConversation()) {
                beforeTokens = conversation.getTokenCount();

                Message response = conversation.sendMessage(
                        Message.Companion.user(request.getPrompt()),
                        Collections.emptyMap(),
                        null,
                        null,
                        null,
                        request.getBudget().getMaxOutputTokens(),
                        new ThinkingConfig(false, 0),
                        null);

                afterTokens = conversation.getTokenCount();
                List<com.google.ai.edge.litertlm.ToolCall> calls = response.getToolCalls();
                observedToolCalls = calls == null ? 0 : calls.size();

                InferenceOutcome outcome = LiteRtToolCallMapper.map(response);
                stopReason = outcome instanceof InferenceOutcome.ToolCall
                        ? InferenceStopReason.NATIVE_TOOL_CALL
                        : InferenceStopReason.INVALID_NATIVE_TOOL_CALL;
                return outcome;
            }
        } catch (Throwable error) {
            InferenceOutcome outcome = classify(error);
            stopReason = stopReasonFor(outcome);
            return outcome;
        } finally {
            Integer before = beforeTokens >= 0 ? beforeTokens : null;
            Integer after = afterTokens >= 0 ? afterTokens : null;
            Integer delta = before != null && after != null
                    ? Math.max(0, after - before)
                    : null;
            telemetrySink.record(new InferenceTelemetry(
                    request.getPrompt().length(),
                    budget.getMaxPromptCharacters(),
                    budget.getMaxContextTokens(),
                    budget.getMaxOutputTokens(),
                    budget.getMaxToolCalls(),
                    before,
                    after,
                    delta,
                    observedToolCalls,
                    stopReason));
        }
    }

    private Conversation createConversation() {
        List<ToolProvider> tools = Collections.singletonList(
                ToolKt.tool(new P3LiteRtToolSet()));
        ConversationConfig config = new ConversationConfig(
                null,
                Collections.emptyList(),
                tools,
                new SamplerConfig(40, 0.9, 0.2, 42),
                false,
                Collections.emptyList(),
                Collections.emptyMap(),
                null,
                false,
                null,
                new ThinkingConfig(false, 0),
                true);
        return engine.createConversation(config);
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    engine.initialize();
                    initialized = true;
                }
            }
        }
    }

    private static InferenceOutcome classify(Throwable error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (message.contains("NOT_FOUND") || message.contains("not found")) {
            return InferenceOutcome.ModelUnavailable.INSTANCE;
        }
        if (message.contains("UNAVAILABLE") || message.contains("unavailable")) {
            return InferenceOutcome.RuntimeUnavailable.INSTANCE;
        }
        if (message.contains("RESOURCE_EXHAUSTED")
                || message.contains("resource exhausted")) {
            return InferenceOutcome.InsufficientResources.INSTANCE;
        }
        return InferenceOutcome.TechnicalFailure.INSTANCE;
    }

    private static InferenceStopReason stopReasonFor(InferenceOutcome outcome) {
        if (outcome == InferenceOutcome.ModelUnavailable.INSTANCE) {
            return InferenceStopReason.MODEL_UNAVAILABLE;
        }
        if (outcome == InferenceOutcome.InsufficientResources.INSTANCE) {
            return InferenceStopReason.INSUFFICIENT_RESOURCES;
        }
        if (outcome == InferenceOutcome.RuntimeUnavailable.INSTANCE) {
            return InferenceStopReason.RUNTIME_UNAVAILABLE;
        }
        return InferenceStopReason.TECHNICAL_FAILURE;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (initialized) {
                engine.close();
                initialized = false;
            }
        }
    }
}
