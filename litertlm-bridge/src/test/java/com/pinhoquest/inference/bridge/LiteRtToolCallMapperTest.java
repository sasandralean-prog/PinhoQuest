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
import static org.junit.Assert.assertTrue;

public class LiteRtToolCallMapperTest {
    @Test
    public void mapsExactlyOneCanonicalToolCall() {
        Message message = Message.Companion.model(
                Contents.Companion.of(""),
                Collections.singletonList(
                        new ToolCall(
                                MicroQuestToolContract.NAME,
                                Map.of(
                                        MicroQuestToolContract.TITLE, "Detalhe escondido",
                                        MicroQuestToolContract.DESCRIPTION_FIELD,
                                        "Encontre algo curioso ao seu redor.",
                                        MicroQuestToolContract.OBJECTIVES,
                                        List.of("Observe", "Registre")))),
                Collections.emptyMap());

        InferenceOutcome outcome = LiteRtToolCallMapper.map(message);

        assertEquals(
                new InferenceOutcome.ToolCall(
                        MicroQuestToolContract.NAME,
                        message.getToolCalls().get(0).getArguments()),
                outcome);
    }

    @Test
    public void rejectsMissingOrMultipleCalls() {
        assertEquals(
                InferenceOutcome.InvalidOutput.INSTANCE,
                LiteRtToolCallMapper.map(Message.Companion.model(
                Contents.Companion.of(""),
                Collections.emptyList(),
                Collections.emptyMap())));

        ToolCall call = new ToolCall(
                MicroQuestToolContract.NAME,
                Map.of(
                        MicroQuestToolContract.TITLE, "Titulo",
                        MicroQuestToolContract.DESCRIPTION_FIELD, "Descricao suficiente",
                        MicroQuestToolContract.OBJECTIVES, List.of("Acao")));

        assertEquals(
                InferenceOutcome.InvalidOutput.INSTANCE,
                LiteRtToolCallMapper.map(
                        Message.Companion.model(
                        Contents.Companion.of(""),
                        Arrays.asList(call, call),
                        Collections.emptyMap())));
    }

    @Test
    public void rejectsUnexpectedNameAndArgumentShape() {
        ToolCall wrongName = new ToolCall(
                "other_tool",
                Map.of(
                        MicroQuestToolContract.TITLE, "Titulo",
                        MicroQuestToolContract.DESCRIPTION_FIELD, "Descricao suficiente",
                        MicroQuestToolContract.OBJECTIVES, List.of("Acao")));

        ToolCall wrongArgs = new ToolCall(
                MicroQuestToolContract.NAME,
                Map.of(
                        MicroQuestToolContract.TITLE, "Titulo",
                        MicroQuestToolContract.DESCRIPTION_FIELD, "Descricao suficiente",
                        MicroQuestToolContract.OBJECTIVES, List.of("Acao"),
                        "category", "EXPLORATION"));

        assertEquals(
                InferenceOutcome.InvalidOutput.INSTANCE,
                LiteRtToolCallMapper.map(
                        Message.Companion.model(
                        Contents.Companion.of(""),
                        Collections.singletonList(wrongName),
                        Collections.emptyMap())));
        assertEquals(
                InferenceOutcome.InvalidOutput.INSTANCE,
                LiteRtToolCallMapper.map(
                        Message.Companion.model(
                        Contents.Companion.of(""),
                        Collections.singletonList(wrongArgs),
                        Collections.emptyMap())));
    }
}
