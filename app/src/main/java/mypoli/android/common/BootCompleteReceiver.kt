package mypoli.android.common

import android.content.Intent
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/03/2018.
 */
class BootCompleteReceiver : AsyncBroadcastReceiver() {

    private val playerRepository by required { playerRepository }
    private val petStatsChangeScheduler by required { lowerPetStatsScheduler }
    private val saveQuestsForRepeatingQuestScheduler by required { saveQuestsForRepeatingQuestScheduler }

    override fun onReceiveAsync(intent: Intent) {
        if(playerRepository.hasPlayer()) {
            petStatsChangeScheduler.schedule()
            saveQuestsForRepeatingQuestScheduler.schedule()
        }
    }

}