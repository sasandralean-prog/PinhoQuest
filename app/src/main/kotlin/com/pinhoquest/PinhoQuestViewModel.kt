package com.pinhoquest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pinhoquest.core.session.SessionCommandResult
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.domain.profile.UserProfile
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.copy.UserFacingCopy
import com.pinhoquest.ui.navigation.MainTab
import com.pinhoquest.ui.navigation.PinhoQuestUiState
import com.pinhoquest.ui.tags.BuiltinTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PinhoQuestViewModel(
    private val graph: PinhoQuestAppGraph,
) : ViewModel() {
    private val _state = MutableStateFlow(PinhoQuestUiState())
    val state: StateFlow<PinhoQuestUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { loadCanonicalState() }
    }

    fun completeOnboarding(rawName: String, selectedTagIds: Set<String>) {
        viewModelScope.launch {
            val gardenName = GardenOwnerName.create(rawName).getOrElse {
                _state.update { state ->
                    state.copy(message = "Escolha um nome de até 20 caracteres.")
                }
                return@launch
            }
            val profile = UserProfile(
                id = graph.profileId,
                gardenOwnerName = gardenName,
                createdAtEpochMillis = System.currentTimeMillis(),
            )
            graph.profileRepository.upsert(profile)
            BuiltinTags.all.forEach { tag ->
                graph.tagRepository.upsert(
                    graph.profileId,
                    tag.copy(enabled = tag.id.value in selectedTagIds),
                )
            }
            loadCanonicalState()
        }
    }

    fun selectTab(tab: MainTab) {
        _state.update { it.copy(selectedTab = tab, message = null) }
    }

    fun generateQuest(mode: QuestMode) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            when (val result = graph.sessionService.generate(QuestRequest(mode))) {
                is SessionCommandResult.Success -> {
                    _state.update {
                        it.copy(
                            currentQuest = result.value,
                            activeSession = null,
                            loading = false,
                        )
                    }
                }
                else -> _state.update {
                    it.copy(
                        loading = false,
                        message = UserFacingCopy.forSessionResult(result),
                    )
                }
            }
        }
    }

    fun startCurrentQuest() {
        viewModelScope.launch {
            val quest = _state.value.currentQuest ?: return@launch
            val accepted = graph.sessionService.accept(quest.id)
            if (accepted !is SessionCommandResult.Success) {
                _state.update { it.copy(message = UserFacingCopy.forSessionResult(accepted)) }
                return@launch
            }
            when (val started = graph.sessionService.start(accepted.value.id)) {
                is SessionCommandResult.Success -> {
                    val persistedQuest = graph.questRepository.get(quest.id) ?: quest
                    _state.update {
                        it.copy(
                            currentQuest = persistedQuest,
                            activeSession = started.value,
                            message = null,
                        )
                    }
                }
                else -> _state.update {
                    it.copy(message = UserFacingCopy.forSessionResult(started))
                }
            }
        }
    }

    fun abandonCurrentQuest() {
        viewModelScope.launch {
            val session = _state.value.activeSession ?: return@launch
            when (val result = graph.sessionService.abandon(session.id)) {
                is SessionCommandResult.Success -> _state.update {
                    it.copy(
                        currentQuest = null,
                        activeSession = null,
                        message = "Essa não rolou hoje. Tudo bem — quando quiser, sorteamos outra.",
                    )
                }
                else -> _state.update {
                    it.copy(message = UserFacingCopy.forSessionResult(result))
                }
            }
        }
    }

    fun setTagEnabled(tagId: TagId, enabled: Boolean) {
        viewModelScope.launch {
            val tag = _state.value.tags.firstOrNull { it.id == tagId } ?: return@launch
            graph.tagRepository.upsert(graph.profileId, tag.copy(enabled = enabled))
            _state.update { state ->
                state.copy(
                    tags = state.tags.map {
                        if (it.id == tagId) it.copy(enabled = enabled) else it
                    },
                )
            }
        }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            graph.preferencesStore.setTheme(theme)
            _state.update { it.copy(theme = theme) }
        }
    }

    fun setFontScale(fontScale: Float) {
        viewModelScope.launch {
            graph.preferencesStore.setFontScale(fontScale)
            _state.update { it.copy(fontScale = fontScale) }
        }
    }

    private suspend fun loadCanonicalState() {
        val preferences = graph.preferencesStore.preferences.first()
        val profile = graph.profileRepository.get(graph.profileId)
        if (profile == null) {
            _state.value = PinhoQuestUiState(
                onboardingRequired = true,
                theme = preferences.theme,
                fontScale = preferences.fontScale,
            )
            return
        }

        val tags = graph.tagRepository.list(graph.profileId)
        val activeSession = graph.sessionRepository.active()
        val activeQuest = activeSession?.let { graph.questRepository.get(it.questId) }
        _state.value = PinhoQuestUiState(
            onboardingRequired = false,
            ownerName = profile.gardenOwnerName.value,
            selectedTab = MainTab.QUESTS,
            currentQuest = activeQuest,
            activeSession = activeSession,
            tags = tags,
            theme = preferences.theme,
            fontScale = preferences.fontScale,
        )
    }

    class Factory(
        private val graph: PinhoQuestAppGraph,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PinhoQuestViewModel::class.java))
            return PinhoQuestViewModel(graph) as T
        }
    }
}
