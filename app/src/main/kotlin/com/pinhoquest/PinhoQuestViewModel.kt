package com.pinhoquest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pinhoquest.core.completion.CompletionResult
import com.pinhoquest.core.garden.FlowerInvestigationCostPolicyV1
import com.pinhoquest.core.garden.InvestigationResult
import com.pinhoquest.core.progression.LevelPolicyV1
import com.pinhoquest.core.session.SessionCommandResult
import com.pinhoquest.data.garden.GardenSnapshot
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.domain.profile.UserProfile
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.reward.RewardResolution
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.copy.UserFacingCopy
import com.pinhoquest.ui.garden.GardenFlowerUi
import com.pinhoquest.ui.garden.GardenUiState
import com.pinhoquest.ui.navigation.MainTab
import com.pinhoquest.ui.navigation.PinhoQuestUiState
import com.pinhoquest.ui.quests.QuestCompletionUi
import com.pinhoquest.core.tag.SystemTagCatalog
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
                id = graph.profileIdFactory.newId(),
                gardenOwnerName = gardenName,
                createdAtEpochMillis = System.currentTimeMillis(),
            )
            graph.profileRepository.upsert(profile)
            SystemTagCatalog.all.forEach { tag ->
                graph.tagRepository.upsert(
                    profile.id,
                    tag.copy(enabled = tag.id.value in selectedTagIds),
                )
            }
            graph.bootstrapper.ensureSeeded(profile.id)
            loadCanonicalState()
        }
    }

    fun selectTab(tab: MainTab) {
        _state.update { it.copy(selectedTab = tab, message = null) }
    }

    fun showMessage(message: String) {
        _state.update { it.copy(message = message) }
    }

    fun openQuestThemeSelection() {
        _state.update { it.copy(questThemeSelectionOpen = true, message = null) }
    }

    fun closeQuestThemeSelection() {
        _state.update { it.copy(questThemeSelectionOpen = false) }
    }

    fun confirmQuestThemeSelection() {
        _state.update { it.copy(questThemeSelectionOpen = false) }
        generateQuest(QuestMode.NORMAL)
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

    fun completeCurrentQuest() {
        viewModelScope.launch {
            val current = _state.value
            val session = current.activeSession ?: return@launch
            val quest = current.currentQuest ?: return@launch
            val completedObjectives = quest.objectives
                .filterNot { it.optional }
                .map { it.id }
                .toSet()

            when (val result = graph.completionService.complete(session.id, completedObjectives)) {
                is CompletionResult.Success -> {
                    val profile = graph.profileRepository.current() ?: return@launch
                    val garden = buildGardenState(
                        ownerName = profile.gardenOwnerName.value,
                        snapshot = graph.gardenRepository.snapshot(profile.id),
                        selectedFlowerId = null,
                    )
                    val awardedId = (result.receipt.rewardResolution as? RewardResolution.Awarded)
                        ?.flowerId
                        ?.value
                    val awarded = awardedId?.let { id ->
                        garden.flowers.firstOrNull { it.id == id }
                    }
                    _state.update {
                        it.copy(
                            currentQuest = null,
                            activeSession = null,
                            garden = garden,
                            completionDialog = QuestCompletionUi(
                                questTitle = quest.title,
                                xpAward = result.receipt.completion.xpAward,
                                flowerId = awardedId,
                                flowerName = awarded?.commonName,
                                flowerRarityLabel = awarded?.rarity?.displayLabel(),
                            ),
                            message = null,
                        )
                    }
                }
                CompletionResult.NotFound -> {
                    _state.update { it.copy(message = "Não encontrei essa quest para concluir.") }
                }
                is CompletionResult.InvalidSessionState -> {
                    _state.update { it.copy(message = "Essa quest já mudou de estado. Vou manter o progresso salvo.") }
                }
                is CompletionResult.UnknownObjectives,
                is CompletionResult.MissingRequiredObjectives,
                -> {
                    _state.update { it.copy(message = "Ainda falta confirmar um objetivo antes de concluir.") }
                }
                CompletionResult.Conflict -> {
                    _state.update { it.copy(message = "Quase lá — tente concluir novamente em um instante.") }
                }
                is CompletionResult.TechnicalFailure -> {
                    _state.update { it.copy(message = "Não consegui concluir agora. Seu progresso atual continua salvo.") }
                }
            }
        }
    }

    fun selectFlower(flowerId: String) {
        _state.update { state ->
            state.copy(garden = state.garden?.copy(selectedFlowerId = flowerId))
        }
    }

    fun dismissFlower() {
        _state.update { state ->
            state.copy(garden = state.garden?.copy(selectedFlowerId = null))
        }
    }

    fun investigateFlower(flowerId: String) {
        viewModelScope.launch {
            val profile = graph.profileRepository.current() ?: return@launch
            when (val result = graph.investigationService.investigate(
                profileId = profile.id,
                flowerId = com.pinhoquest.domain.garden.FlowerId(flowerId),
            )) {
                is InvestigationResult.Advanced -> {
                    val garden = buildGardenState(
                        ownerName = profile.gardenOwnerName.value,
                        snapshot = graph.gardenRepository.snapshot(profile.id),
                        selectedFlowerId = flowerId,
                    )
                    _state.update {
                        it.copy(
                            garden = garden,
                            message = if (result.discovery.state.name == "REVEALED") {
                                "Agora você já sabe qual flor está esperando por você. 🌷"
                            } else {
                                "Descobri uma pista nova sobre essa flor. 🌱"
                            },
                        )
                    }
                }
                is InvestigationResult.InsufficientXp -> {
                    _state.update {
                        it.copy(
                            message = "Faltam " + (result.required - result.available) +
                                " XP para investigar esta flor.",
                        )
                    }
                }
                InvestigationResult.AlreadyRevealed -> {
                    _state.update { it.copy(message = "Você já descobriu tudo que dá para investigar aqui.") }
                }
                InvestigationResult.AlreadyCollected -> {
                    _state.update { it.copy(message = "Essa flor já faz parte do seu jardim. 🌷") }
                }
                InvestigationResult.NotFound -> {
                    _state.update { it.copy(message = "Não encontrei essa flor no jardim atual.") }
                }
                InvestigationResult.Conflict -> {
                    _state.update { it.copy(message = "O jardim mudou enquanto eu investigava. Tente mais uma vez.") }
                }
            }
        }
    }

    fun dismissCompletion() {
        _state.update { it.copy(completionDialog = null) }
    }

    fun openGardenFromCompletion() {
        _state.update { state ->
            val flowerId = state.completionDialog?.flowerId
            state.copy(
                selectedTab = MainTab.GARDEN,
                completionDialog = null,
                garden = state.garden?.copy(selectedFlowerId = flowerId),
            )
        }
    }

    fun setTagEnabled(tagId: TagId, enabled: Boolean) {
        viewModelScope.launch {
            val tag = _state.value.tags.firstOrNull { it.id == tagId } ?: return@launch
            val profile = graph.profileRepository.current() ?: return@launch
            graph.tagRepository.upsert(profile.id, tag.copy(enabled = enabled))
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
        val profile = graph.profileRepository.current()
        if (profile == null) {
            _state.value = PinhoQuestUiState(
                initializing = false,
                onboardingRequired = true,
                theme = preferences.theme,
                fontScale = preferences.fontScale,
            )
            return
        }

        graph.bootstrapper.ensureSeeded(profile.id)
        val tags = graph.tagRepository.list(profile.id)
        val activeSession = graph.sessionRepository.active()
        val activeQuest = activeSession?.let { graph.questRepository.get(it.questId) }
        val garden = buildGardenState(
            ownerName = profile.gardenOwnerName.value,
            snapshot = graph.gardenRepository.snapshot(profile.id),
            selectedFlowerId = null,
        )
        _state.value = PinhoQuestUiState(
            initializing = false,
            onboardingRequired = false,
            ownerName = profile.gardenOwnerName.value,
            selectedTab = MainTab.QUESTS,
            currentQuest = activeQuest,
            activeSession = activeSession,
            tags = tags,
            garden = garden,
            theme = preferences.theme,
            fontScale = preferences.fontScale,
        )
    }

    private fun buildGardenState(
        ownerName: String,
        snapshot: GardenSnapshot,
        selectedFlowerId: String?,
    ): GardenUiState {
        val lifetimeXp = snapshot.xpLedger.lifetimeXp()
        val selected = selectedFlowerId?.takeIf { id ->
            snapshot.flowers.any { it.definition.id.value == id }
        }
        val flowers = snapshot.flowers.map { flower ->
            GardenFlowerUi(
                id = flower.definition.id.value,
                commonName = flower.definition.commonName,
                scientificName = flower.definition.scientificName,
                description = flower.definition.description,
                rarity = flower.definition.rarity,
                discoveryState = flower.discoveryState,
                xpAward = flower.acquisition?.xpAward,
                acquiredAtEpochMillis = flower.acquisition?.acquiredAtEpochMillis,
                questTitle = flower.questTitle,
                investigationCost = when (flower.discoveryState) {
                    com.pinhoquest.domain.garden.FlowerDiscoveryState.HIDDEN,
                    com.pinhoquest.domain.garden.FlowerDiscoveryState.HINTED,
                    -> FlowerInvestigationCostPolicyV1.costFor(flower.discoveryState)
                    com.pinhoquest.domain.garden.FlowerDiscoveryState.REVEALED,
                    com.pinhoquest.domain.garden.FlowerDiscoveryState.COLLECTED,
                    -> null
                },
            )
        }
        return GardenUiState(
            ownerName = ownerName,
            lifetimeXp = lifetimeXp,
            spendableXp = snapshot.xpLedger.spendableXp(),
            level = LevelPolicyV1.levelFor(lifetimeXp),
            collectedCount = flowers.count {
                it.discoveryState == com.pinhoquest.domain.garden.FlowerDiscoveryState.COLLECTED
            },
            totalCount = flowers.size,
            flowers = flowers,
            selectedFlowerId = selected,
        )
    }

    private fun FlowerRarity.displayLabel(): String = when (this) {
        FlowerRarity.COMMON -> "Comum"
        FlowerRarity.UNCOMMON -> "Incomum"
        FlowerRarity.RARE -> "Rara"
        FlowerRarity.RAREST -> "Raríssima"
        FlowerRarity.UNKNOWN -> "???"
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
