package com.pinhoquest.inference.bridge;

import com.pinhoquest.core.inference.micro.MicroQuestToolContract;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class P3LiteRtToolSetTest {
    @Test
    public void schemaUsesCanonicalContractMetadata() {
        String schema = new P3LiteRtToolSet().getToolDescriptionJsonString();
        assertTrue(schema.contains("\"name\": \"" + MicroQuestToolContract.NAME + "\""));
        assertTrue(schema.contains("\"parameters\""));
        assertTrue(schema.contains("\"" + MicroQuestToolContract.TITLE + "\""));
        assertTrue(schema.contains("\"" + MicroQuestToolContract.DESCRIPTION_FIELD + "\""));
        assertTrue(schema.contains("\"" + MicroQuestToolContract.OBJECTIVES + "\""));
        assertTrue(schema.contains("\"required\""));
        assertEquals("{\"title\":\"x\"}", new P3LiteRtToolSet().execute("{\"title\":\"x\"}"));
    }
}
