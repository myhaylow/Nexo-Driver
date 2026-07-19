package com.myhaylow.nexodriver.profile

import com.myhaylow.nexodriver.finance.OperatingCosts
import java.time.LocalDateTime

object RuntimeConfiguration {
    @Volatile private var repository: ProfileRepository? = null
    fun initialize(repository: ProfileRepository) { this.repository = repository }

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
