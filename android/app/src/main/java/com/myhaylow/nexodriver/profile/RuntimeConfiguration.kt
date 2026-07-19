package com.myhaylow.nexodriver.profile

import com.myhaylow.nexodriver.finance.OperatingCosts
import java.time.LocalDateTime

object RuntimeConfiguration {
    @Volatile private var repository: ProfileConfigurationSource? = null
    fun initialize(repository: ProfileConfigurationSource) { this.repository = repository }
    fun initializeIfNeeded(factory: () -> ProfileConfigurationSource) {
        if (repository != null) return
        synchronized(this) { if (repository == null) repository = factory() }
    }

    internal fun resetForTest() { repository = null }

    fun current(moment: LocalDateTime = LocalDateTime.now()): Pair<ProfileSelection, OperatingCosts>? {
        val repo = repository ?: return null
        return ProfileScheduler.resolve(repo.profiles(), repo.schedules(), repo.activeProfileId(),
            repo.automaticEnabled(), moment) to repo.costs()
    }

    fun destination(): Pair<ProfileSelection, OperatingCosts>? {
        val repo = repository ?: return null
        val profile = repo.profiles().firstOrNull { it.destinationMode } ?: ProfileRepository.DESTINATION
        return ProfileSelection(profile, false) to repo.costs()
    }
}
