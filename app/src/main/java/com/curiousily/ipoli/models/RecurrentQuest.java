package com.curiousily.ipoli.models;

import com.curiousily.ipoli.quest.Quest;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 7/31/15.
 */
public class RecurrentQuest extends Quest {

    public final WeeklyRecurrence weeklyRecurrence;

    public RecurrentQuest(String name, String description, String time, int duration, Context context) {
        super(name, description, time, duration, context);
        this.weeklyRecurrence = new WeeklyRecurrence(2, 3);
    }

    public static class WeeklyRecurrence {
        public final int timesCompleted;
        public final int totalTimes;

        public WeeklyRecurrence(int timesCompleted, int totalTimes) {
            this.timesCompleted = timesCompleted;
            this.totalTimes = totalTimes;
        }
    }
}
