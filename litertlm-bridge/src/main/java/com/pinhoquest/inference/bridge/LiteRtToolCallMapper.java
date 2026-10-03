package com.pinhoquest.inference.bridge;

import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.ToolCall;
import com.pinhoquest.core.inference.InferenceOutcome;
import com.pinhoquest.core.inference.micro.MicroQuestToolContract;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LiteRtToolCallMapper {
    private LiteRtToolCallMapper() {}

    public static InferenceOutcome map(Message message) {
        List<ToolCall> calls = message.getToolCalls();
        if (calls == null || calls.size() != 1) {
            return InferenceOutcome.InvalidOutput.INSTANCE;
        }

        ToolCall call = calls.get(0);
        if (!MicroQuestToolContract.NAME.equals(call.getName())) {
            return InferenceOutcome.InvalidOutput.INSTANCE;
        }

        Set<String> expected = new HashSet<>(
                MicroQuestToolContract.INSTANCE.getREQUIRED_ARGUMENTS());
        Map<String, Object> arguments = call.getArguments();
        if (arguments == null || !expected.equals(arguments.keySet())) {
            return InferenceOutcome.InvalidOutput.INSTANCE;
        }

        return new InferenceOutcome.ToolCall(call.getName(), arguments);
    }
}
