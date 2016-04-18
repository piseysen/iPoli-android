package io.ipoli.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;
import com.squareup.otto.Subscribe;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.ipoli.android.app.BaseActivity;
import io.ipoli.android.app.events.ScreenShownEvent;
import io.ipoli.android.quest.QuestContext;
import io.ipoli.android.quest.activities.QuestActivity;
import io.ipoli.android.quest.activities.QuestCompleteActivity;
import io.ipoli.android.quest.events.ColorLayoutEvent;
import io.ipoli.android.quest.events.CompleteQuestRequestEvent;
import io.ipoli.android.quest.events.ShowQuestEvent;
import io.ipoli.android.quest.fragments.AddQuestFragment;
import io.ipoli.android.quest.fragments.CalendarDayFragment;
import io.ipoli.android.quest.fragments.HabitsFragment;
import io.ipoli.android.quest.fragments.InboxFragment;
import io.ipoli.android.quest.fragments.OverviewFragment;
import io.ipoli.android.tutorial.TutorialActivity;

public class MainActivity extends BaseActivity {
    public static final int CALENDAR_TAB_INDEX = 0;
    public static final int OVERVIEW_TAB_INDEX = 1;
    public static final int ADD_QUEST_TAB_INDEX = 2;
    public static final int INBOX_TAB_INDEX = 3;
    public static final int HABITS_TAB_INDEX = 4;
    private static final int TUTORIAL_REQUEST_CODE = 1234;

    @IdRes
    private int currentSelectedItem = 0;

    private BottomBar bottomBar;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        startTutorial();

        initBottomBar(savedInstanceState);

    }

    public void startTutorial(){
        Intent intent = new Intent(this, TutorialActivity.class);
        startActivity(intent);
    }

    private void initBottomBar(Bundle savedInstanceState) {
        bottomBar = BottomBar.attachShy((CoordinatorLayout) findViewById(R.id.root_container),
                findViewById(R.id.content_container), savedInstanceState);
        bottomBar.setItemsFromMenu(R.menu.bottom_bar_menu, new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(@IdRes int menuItemId) {
                String screenName = "";
                resetLayoutColors();

                switch (menuItemId) {
                    case R.id.calendar:
                        screenName = "calendar";
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.content_container, new CalendarDayFragment()).commit();
                        break;
                    case R.id.overview:
                        screenName = "overview";
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.content_container, new OverviewFragment()).commit();
                        break;
                    case R.id.add_quest:
                        screenName = "add_quest";
                        boolean isForToday = currentSelectedItem == R.id.calendar;
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.content_container, AddQuestFragment.newInstance(isForToday)).commit();
                        break;
                    case R.id.inbox:
                        screenName = "inbox";
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.content_container, new InboxFragment()).commit();
                        break;
                    case R.id.habits:
                        screenName = "habits";
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.content_container, new HabitsFragment()).commit();
                        break;
                }
                currentSelectedItem = menuItemId;
                eventBus.post(new ScreenShownEvent(screenName));
            }

            @Override
            public void onMenuTabReSelected(@IdRes int menuItemId) {

            }
        });


        bottomBar.mapColorForTab(CALENDAR_TAB_INDEX, ContextCompat.getColor(this, R.color.colorPrimary));
        bottomBar.mapColorForTab(OVERVIEW_TAB_INDEX, ContextCompat.getColor(this, R.color.colorPrimary));
        bottomBar.mapColorForTab(ADD_QUEST_TAB_INDEX, ContextCompat.getColor(this, R.color.colorPrimary));
        bottomBar.mapColorForTab(INBOX_TAB_INDEX, ContextCompat.getColor(this, R.color.colorPrimary));
        bottomBar.mapColorForTab(HABITS_TAB_INDEX, ContextCompat.getColor(this, R.color.colorPrimary));
    }

    private void resetLayoutColors() {
        colorLayout(QuestContext.LEARNING);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        bottomBar.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        eventBus.register(this);
    }

    @Override
    public void onPause() {
        eventBus.unregister(this);
        super.onPause();
    }

    @Subscribe
    public void onColorLayout(ColorLayoutEvent e) {
        QuestContext context = e.questContext;
        colorLayout(context);
    }

    private void colorLayout(QuestContext context) {
        toolbar.setBackgroundColor(ContextCompat.getColor(this, context.resLightColor));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, context.resLightColor));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, context.resDarkColor));
        View view = bottomBar.findViewById(R.id.bb_bottom_bar_outer_container);
        view.setBackgroundColor(ContextCompat.getColor(this, context.resLightColor));
    }

    @Subscribe
    public void onShowQuestEvent(ShowQuestEvent e) {
        Intent i = new Intent(this, QuestActivity.class);
        i.putExtra(Constants.QUEST_ID_EXTRA_KEY, e.quest.getId());
        startActivity(i);
    }

    @Subscribe
    public void onQuestCompleteRequest(CompleteQuestRequestEvent e) {
        Intent i = new Intent(this, QuestCompleteActivity.class);
        i.putExtra(Constants.QUEST_ID_EXTRA_KEY, e.quest.getId());
        startActivityForResult(i, Constants.COMPLETE_QUEST_RESULT_REQUEST_CODE);
    }
}