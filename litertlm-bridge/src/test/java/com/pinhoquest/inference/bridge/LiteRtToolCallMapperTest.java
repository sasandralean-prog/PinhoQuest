package com.pinhoquest.inference.bridge;

import com.google.ai.edge.litertlm.Contents;
import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.ToolCall;
import com.pinhoquest.core.inference.InferenceOutcome;
import com.pinhoquest.core.inference.micro.MicroQuestToolContract;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LiteRtToolCallMapperTest {
    private static ToolCall canonicalCall() {
        return new ToolCall(
                MicroQuestToolContract.NAME,
                Map.of(
                        MicroQuestToolContract.TITLE, "Detalhe escondido",
                        MicroQuestToolContract.DESCRIPTION_FIELD,
                        "Encontre algo curioso ao seu redor.",
                        MicroQuestToolContract.OBJECTIVES,
                        List.of("Observe", "Registre")));
    }

    private static Message message(List<ToolCall> calls) {
        return Message.Companion.model(
                Contents.Companion.of(""),
                calls,
                Collections.emptyMap());
    }

    @Test
    public void mapsExactlyOneCanonicalToolCall() {
        ToolCall call = canonicalCall();
        InferenceOutcome outcome = LiteRtToolCallMapper.map(message(Collections.singletonList(call)));

        assertEquals(new InferenceOutcome.ToolCall(MicroQuestToolContract.NAME, call.getArguments()), outcome);
    }

    @Test
    public void rejectsZeroAndMultipleCalls() {
        assertEquals(InferenceOutcome.InvalidOutput.INSTANCE, LiteRtToolCallMapper.map(message(Collections.emptyList())));
        ToolCall call = canonicalCall();
        assertEquals(
                InferenceOutcome.InvalidOutput.INSTANCE,
                LiteRtToolCallMapper.map(message(Arrays.asList(call, call))));
    }

    @Test
    public void rejectsUnexpectedName() {
        ToolCall wrong = new ToolCall(
                "other_tool",
                canonicalCall().getArguments());

        assertEquals(InferenceOutcome.InvalidOutput.INSTANCE, LiteRtToolCallMapper.map(message(Collections.singletonList(wrong))));
    }

    @Test
    public void rejectsMissingExtraAndNullArguments() {
        Map<String, Object> canonical = canonicalCall().getArguments();
        Map<String, Object> missing = Map.of(
                MicroQuestToolContract.TITLE, canonical.get(MicroQuestToolContract.TITLE),
                MicroQuestToolContract.DESCRIPTION_FIELD, canonical.get(MicroQuestToolContract.DESCRIPTION_FIELD));
        Map<String, Object> extra = Map.of(
                MicroQuestToolContract.TITLE, canonical.get(MicroQuestToolContract.TITLE),
                MicroQuestToolContract.DESCRIPTION_FIELD, canonical.get(MicroQuestToolContract.DESCRIPTION_FIELD),
                MicroQuestToolContract.OBJECTIVES, canonical.get(MicroQuestToolContract.OBJECTIVES),
                "category", "EXPLORATION");

        assertEquals(InferenceOutcome.InvalidOutput.INSTANCE, LiteRtToolCallMapper.map(message(
                Collections.singletonList(new ToolCall(MicroQuestToolContract.NAME, missing)))));
        assertEquals(InferenceOutcome.InvalidOutput.INSTANCE, LiteRtToolCallMapper.map(message(
                Collections.singletonList(new ToolCall(MicroQuestToolContract.NAME, extra)))));
        assertEquals(InferenceOutcome.InvalidOutput.INSTANCE, LiteRtToolCallMapper.map(message(
                Collections.singletonList(new ToolCall(MicroQuestToolContract.NAME, null)))));
    }
}
