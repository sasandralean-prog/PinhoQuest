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
import com.pinhoquest.core.inference.InferenceOutcome;

import java.util.Collections;
import java.util.List;

public final class LiteRtLmRuntime implements AutoCloseable {
    private final Engine engine;
    private volatile boolean initialized = false;

    public LiteRtLmRuntime(String modelPath, String cacheDir, int maxNumTokens) {
        this.engine = new Engine(new EngineConfig(
                modelPath,
                new Backend.CPU(),
                null,
                null,
                maxNumTokens,
                null,
                cacheDir));
    }

    public InferenceOutcome generate(GenerationRequest request) {
        try {
            ensureInitialized();
            try (Conversation conversation = createConversation()) {
                Message response = conversation.sendMessage(
                        Message.Companion.user(request.getPrompt()),
                        Collections.emptyMap(),
                        null,
                        null,
                        null,
                        request.getMaxOutputTokens(),
                        new ThinkingConfig(false, 0),
                        null);
                return LiteRtToolCallMapper.map(response);
            }
        } catch (Throwable error) {
            return classify(error);
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
