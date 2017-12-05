package io.ipoli.android.pet.usecase

import io.ipoli.android.Constants
import io.ipoli.android.common.UseCase
import io.ipoli.android.player.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 12/5/17.
 */
class RevivePetUseCase(private val playerRepository: PlayerRepository) : UseCase<Unit, RevivePetUseCase.Result> {
    override fun execute(parameters: Unit): Result {
        val player = playerRepository.find()
        requireNotNull(player)
        val pet = player!!.pet
        require(pet.isDead)

        if (player.coins < Constants.REVIVE_PET_COST) {
            return Result.TooExpensive
        }

        val newPlayer = player.copy(
            coins = player.coins - Constants.REVIVE_PET_COST,
            pet = player.pet.setHealthAndMoodPoints(
                Constants.DEFAULT_PET_HP,
                Constants.DEFAULT_PET_MP
            )
        )

        return Result.PetRevived(playerRepository.save(newPlayer))
    }

    sealed class Result {
        data class PetRevived(val player: Player) : Result()
        object TooExpensive : Result()
    }
}