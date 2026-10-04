package com.pinhoquest.inference.bridge;

import com.google.ai.edge.litertlm.OpenApiTool;
import com.pinhoquest.core.inference.micro.MicroQuestToolContract;

public final class P3LiteRtToolSet implements OpenApiTool {
    private static final String DESCRIPTION = MicroQuestToolContract.DESCRIPTION;
    private static final String SCHEMA = String.format(java.util.Locale.ROOT, """
            {
              "name": "%s",
              "description": "%s",
              "parameters": {
                "type": "object",
                "properties": {
                  "%s": {"type": "string", "description": "%s"},
                  "%s": {"type": "string", "description": "%s"},
                  "%s": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "%s"
                  }
                },
                "required": ["%s", "%s", "%s"]
              }
            }
            """,
            MicroQuestToolContract.NAME,
            DESCRIPTION,
            MicroQuestToolContract.TITLE,
            MicroQuestToolContract.TITLE_DESCRIPTION,
            MicroQuestToolContract.DESCRIPTION_FIELD,
            MicroQuestToolContract.DESCRIPTION_DESCRIPTION,
            MicroQuestToolContract.OBJECTIVES,
            MicroQuestToolContract.OBJECTIVES_DESCRIPTION,
            MicroQuestToolContract.TITLE,
            MicroQuestToolContract.DESCRIPTION_FIELD,
            MicroQuestToolContract.OBJECTIVES);

    @Override
    public String getToolDescriptionJsonString() {
        return SCHEMA;
    }

    @Override
    public String execute(String paramsJsonString) {
        return paramsJsonString;
    }
}
