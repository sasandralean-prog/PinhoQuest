package com.pinhoquest.inference.bridge;

import com.google.ai.edge.litertlm.Tool;
import com.google.ai.edge.litertlm.ToolParam;
import com.pinhoquest.core.inference.micro.MicroQuestToolContract;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class P3LiteRtToolSetTest {
    @Test
    public void declarationUsesCanonicalContractMetadata() throws Exception {
        Method method = P3LiteRtToolSet.class.getDeclaredMethod(
                "composeQuestText", String.class, String.class, List.class);

        Tool tool = method.getAnnotation(Tool.class);
        assertNotNull(tool);
        assertEquals(MicroQuestToolContract.DESCRIPTION, tool.description());

        Annotation[][] annotations = method.getParameterAnnotations();
        assertEquals(3, annotations.length);
        assertEquals(
                MicroQuestToolContract.TITLE_DESCRIPTION,
                ((ToolParam) annotations[0][0]).description());
        assertEquals(
                MicroQuestToolContract.DESCRIPTION_DESCRIPTION,
                ((ToolParam) annotations[1][0]).description());
        assertEquals(
                MicroQuestToolContract.OBJECTIVES_DESCRIPTION,
                ((ToolParam) annotations[2][0]).description());
    }
}
