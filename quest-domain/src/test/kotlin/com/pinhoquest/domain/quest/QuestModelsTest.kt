package com.pinhoquest.domain.quest

import org.junit.Assert.assertEquals
import org.junit.Test

class QuestModelsTest {
    @Test
    fun questModesAreStable() {
        assertEquals(
            listOf(QuestMode.NORMAL, QuestMode.GAME, QuestMode.RANDOM),
            QuestMode.entries,
        )
    }

    @Test
    fun questStatesAreStable() {
        assertEquals(
            listOf(
                QuestState.GENERATED,
                QuestState.ACCEPTED,
                QuestState.ACTIVE,
                QuestState.COMPLETED,
                QuestState.ABANDONED,
                QuestState.REJECTED,
            ),
            QuestState.entries,
        )
    }
}
