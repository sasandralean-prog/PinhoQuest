package com.pinhoquest.inference.bridge;

import com.google.ai.edge.litertlm.Tool;
import com.google.ai.edge.litertlm.ToolParam;
import com.google.ai.edge.litertlm.ToolSet;
import com.pinhoquest.core.inference.micro.MicroQuestToolContract;

public final class P3LiteRtToolSet implements ToolSet {
    @Tool(description = MicroQuestToolContract.DESCRIPTION)
    public java.util.Map<String, Object> composeQuestText(
            @ToolParam(description = MicroQuestToolContract.TITLE_DESCRIPTION)
            String title,
            @ToolParam(description = MicroQuestToolContract.DESCRIPTION_DESCRIPTION)
            String description,
            @ToolParam(description = MicroQuestToolContract.OBJECTIVES_DESCRIPTION)
            java.util.List<String> objectives) {
        return java.util.Map.of(
                MicroQuestToolContract.TITLE, title,
                MicroQuestToolContract.DESCRIPTION_FIELD, description,
                MicroQuestToolContract.OBJECTIVES, objectives);
    }
}
