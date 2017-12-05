package io.ipoli.android.pet

import io.ipoli.android.TestUtil
import io.ipoli.android.quest.Category
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Quest
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be true`
import org.amshove.kluent.`should be`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 29.11.17.
 */
class PetSpek : Spek({
    describe("Pet") {

        val pet = Pet(
            name = "",
            avatar = PetAvatar.ELEPHANT,
            healthPoints = 0,
            moodPoints = 0
        )

        val quest = Quest(
            name = "",
            color = Color.BLUE,
            scheduledDate = LocalDate.now(),
            category = Category("", Color.BLUE_GREY),
            duration = 30,
            experience = 100,
            coins = 20
        )

        it("should not give more than max HP & MP") {
            val p = pet.copy(
                healthPoints = Pet.MAX_HP,
                moodPoints = Pet.MAX_MP,
                mood = PetMood.AWESOME
            )
            val newPet = p.rewardFor(quest)
            newPet.healthPoints.`should be equal to`(Pet.MAX_HP)
            newPet.moodPoints.`should be equal to`(Pet.MAX_MP)
        }

        it("should give 2x mood bonus when HP is high") {
            val sickPet = pet.copy(
                healthPoints = 30,
                moodPoints = 60,
                mood = PetMood.GOOD
            )

            val normalIncrease = sickPet.rewardFor(quest).moodPoints - sickPet.moodPoints
            val healthyPet = sickPet.copy(healthPoints = 90)
            val bonusIncrease = healthyPet.rewardFor(quest).moodPoints - sickPet.moodPoints
            bonusIncrease.`should be equal to`(normalIncrease * 2)
        }

        it("should have maximum bonus when mood is Awesome") {
            val newPet = pet.copy(
                healthPoints = 89,
                moodPoints = 89,
                mood = PetMood.HAPPY
            ).rewardFor(quest)
            newPet.mood.`should be`(PetMood.AWESOME)
            newPet.experienceBonus.`should be equal to`(Pet.MAX_XP_BONUS)
            newPet.coinBonus.`should be equal to`(Pet.MAX_COIN_BONUS)
            newPet.itemDropChanceBonus.`should be equal to`(Pet.MAX_UNLOCK_CHANCE_BONUS)
        }

        it("should change mood to Happy when reward for Quest is removed") {
            val newPet = pet.copy(
                healthPoints = Pet.MAX_HP,
                moodPoints = Pet.AWESOME_MIN_MOOD_POINTS,
                mood = PetMood.AWESOME
            ).removeRewardFor(quest)
            newPet.mood.`should be`(PetMood.HAPPY)
        }

        describe("update health and mood points") {

            it("should remove HP") {
                val newPet = TestUtil.player().pet.copy(
                    healthPoints = 10,
                    moodPoints = 10,
                    mood = PetMood.SAD
                ).updateHealthAndMoodPoints(-20, 0)
                newPet.healthPoints.`should be equal to`(0)
            }

            it("should remove MP") {
                val newPet = TestUtil.player().pet.copy(
                    healthPoints = Pet.MAX_HP,
                    moodPoints = 10,
                    mood = PetMood.SAD
                ).updateHealthAndMoodPoints(0, -20)
                newPet.moodPoints.`should be equal to`(0)
            }

            it("should remove HP & add MP") {
                val newPet = TestUtil.player().pet.copy(
                    healthPoints = Pet.MAX_HP,
                    moodPoints = 10,
                    mood = PetMood.SAD
                ).updateHealthAndMoodPoints(-20, 20)
                newPet.healthPoints.`should be equal to`(Pet.MAX_HP - 20)
                newPet.moodPoints.`should be equal to`(30)
            }

            it("should kill it") {
                val newPet = TestUtil.player().pet.copy(
                    healthPoints = 10,
                    moodPoints = 10,
                    mood = PetMood.SAD
                ).updateHealthAndMoodPoints(-20, 0)
                newPet.isDead.`should be true`()
                newPet.healthPoints.`should be equal to`(0)
                newPet.moodPoints.`should be equal to`(0)
                newPet.mood.`should be`(PetMood.SAD)
                newPet.experienceBonus.`should be equal to`(0f)
                newPet.coinBonus.`should be equal to`(0f)
                newPet.itemDropChanceBonus.`should be equal to`(0f)
            }
        }
    }
})