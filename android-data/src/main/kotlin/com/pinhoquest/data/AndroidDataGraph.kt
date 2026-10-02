package com.pinhoquest.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.bootstrap.ProgressionBootstrapper
import com.pinhoquest.data.completion.RoomQuestCompletionStore
import com.pinhoquest.data.db.migration.MIGRATION_1_2
import com.pinhoquest.data.garden.RoomFlowerInvestigationStore
import com.pinhoquest.data.garden.RoomGardenRepository
import com.pinhoquest.data.repository.RoomProfileRepository
import com.pinhoquest.data.repository.RoomQuestRepository
import com.pinhoquest.data.repository.RoomQuestSessionRepository
import com.pinhoquest.data.repository.RoomTagRepository
import com.pinhoquest.data.settings.AppPreferencesStore

class AndroidDataGraph(context: Context) {
    private val appContext = context.applicationContext

    private val database: PinhoQuestDatabase = Room.databaseBuilder(
        appContext,
        PinhoQuestDatabase::class.java,
        "pinho-quest.db",
    )
        .addMigrations(MIGRATION_1_2)
        .build()

    val profileRepository = RoomProfileRepository(database.profileDao())
    val tagRepository = RoomTagRepository(database.tagDao())
    val questRepository = RoomQuestRepository(database.questDao())
    val sessionRepository = RoomQuestSessionRepository(database.questSessionDao())
    val bootstrapper = ProgressionBootstrapper(database)
    val gardenRepository = RoomGardenRepository(database)
    val investigationStore = RoomFlowerInvestigationStore(database)
    val completionStore = RoomQuestCompletionStore(database)

    val preferencesStore = AppPreferencesStore(
        PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile("app") },
        ),
    )
}
