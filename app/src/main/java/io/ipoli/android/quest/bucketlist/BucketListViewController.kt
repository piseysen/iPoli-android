package io.ipoli.android.quest.bucketlist

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.ionicons_typeface_library.Ionicons
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.datetime.daysUntil
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.text.DateFormatter
import io.ipoli.android.common.text.QuestStartTimeFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.MultiViewRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.common.view.recyclerview.SimpleSwipeCallback
import io.ipoli.android.quest.CompletedQuestViewController
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.bucketlist.usecase.CreateBucketListItemsUseCase
import io.ipoli.android.quest.schedule.addquest.AddQuestAnimationHelper
import io.ipoli.android.quest.show.QuestViewController
import kotlinx.android.synthetic.main.animation_empty_list.view.*
import kotlinx.android.synthetic.main.controller_bucket_list.view.*
import kotlinx.android.synthetic.main.item_agenda_quest.view.*
import kotlinx.android.synthetic.main.view_loader.view.*
import org.threeten.bp.LocalDate

class BucketListViewController(args: Bundle? = null) :
    ReduxViewController<BucketListAction, BucketListViewState, BucketListReducer>(args) {

    override val reducer = BucketListReducer

    private lateinit var addQuestAnimationHelper: AddQuestAnimationHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = container.inflate(R.layout.controller_bucket_list)

        view.questList.layoutManager = LinearLayoutManager(activity!!)
        view.questList.adapter = QuestAdapter()

        val swipeHandler = object : SimpleSwipeCallback(
            view.context,
            R.drawable.ic_done_white_24dp,
            R.color.md_green_500,
            R.drawable.ic_event_white_24dp,
            R.color.md_blue_500
        ) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.END) {
                    dispatch(BucketListAction.CompleteQuest(questId(viewHolder)))
                } else if (direction == ItemTouchHelper.START) {
                    dispatch(BucketListAction.ScheduleForToday(questId(viewHolder)))
                    showShortToast(R.string.quest_scheduled_for_today)
                }
            }

            private fun questId(holder: RecyclerView.ViewHolder): String {
                val adapter = view.questList.adapter as QuestAdapter
                return if (holder.itemViewType == ViewType.QUEST.value) {
                    val item = adapter.getItemAt<ItemViewModel.QuestItem>(holder.adapterPosition)
                    item.id
                } else {
                    val item =
                        adapter.getItemAt<ItemViewModel.CompletedQuestItem>(holder.adapterPosition)
                    item.id
                }

            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) = when {
                viewHolder.itemViewType == ViewType.QUEST.value -> (ItemTouchHelper.END or ItemTouchHelper.START)
                else -> 0
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(view.questList)

        initEmptyView(view)

        initAddQuest(view)

        return view
    }

    private fun initAddQuest(view: View) {
        addQuestAnimationHelper = AddQuestAnimationHelper(
            controller = this,
            addContainer = view.addContainer,
            fab = view.addQuest,
            background = view.addContainerBackground
        )

        view.addContainerBackground.setOnClickListener {
            addContainerRouter(view).popCurrentController()
            ViewUtils.hideKeyboard(view)
            addQuestAnimationHelper.closeAddContainer()
        }

        view.addQuest.setOnClickListener {
            addQuestAnimationHelper.openAddContainer(null)
        }
    }

    private fun addContainerRouter(view: View) =
        getChildRouter(view.addContainer, "add-quest")

    private fun initEmptyView(view: View) {
        view.emptyAnimation.setAnimation("empty_bucket_list.json")
        view.emptyTitle.setText(R.string.empty_bucket_list_title)
        view.emptyText.setText(R.string.empty_bucket_list_text)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        toolbarTitle = stringRes(R.string.title_bucket_list)
    }

    override fun onCreateLoadAction() = BucketListAction.Load

    override fun render(state: BucketListViewState, view: View) {
        when (state) {

            BucketListViewState.Empty ->
                renderEmpty(view)

            is BucketListViewState.Changed ->
                renderQuests(state, view)
        }
    }

    private fun renderEmpty(view: View) {
        view.loader.gone()
        view.questList.gone()
        view.emptyContainer.visible()
        view.emptyAnimation.playAnimation()
    }

    private fun renderQuests(
        state: BucketListViewState.Changed,
        view: View
    ) {
        view.loader.gone()
        view.emptyContainer.gone()
        view.emptyAnimation.pauseAnimation()
        view.questList.visible()
        (view.questList.adapter as QuestAdapter).updateAll(state.itemViewModels)
    }

    sealed class ItemViewModel(override val id: String) : RecyclerViewViewModel {

        data class SectionItem(val text: String) : ItemViewModel(text)

        data class TagViewModel(val name: String, @ColorRes val color: Int)

        data class QuestItem(
            override val id: String,
            val name: String,
            val dueDate: String,
            @ColorRes val color: Int,
            val tags: List<TagViewModel>,
            val icon: IIcon,
            val isRepeating: Boolean,
            val isFromChallenge: Boolean
        ) : ItemViewModel(id)

        data class CompletedQuestItem(
            override val id: String,
            val name: String,
            val startTime: String,
            @ColorRes val color: Int,
            val tags: List<TagViewModel>,
            val icon: IIcon,
            val isRepeating: Boolean,
            val isFromChallenge: Boolean
        ) : ItemViewModel(id)
    }

    enum class ViewType(val value: Int) {
        SECTION(0),
        QUEST(1),
        COMPLETED_QUEST(2)
    }

    inner class QuestAdapter : MultiViewRecyclerViewAdapter<ItemViewModel>() {

        override fun onRegisterItemBinders() {

            registerBinder<ItemViewModel.SectionItem>(
                ViewType.SECTION.value,
                R.layout.item_list_section,
                { vm, view ->
                    (view as TextView).text = vm.text
                }
            )

            registerBinder<ItemViewModel.QuestItem>(
                ViewType.QUEST.value,
                R.layout.item_agenda_quest,
                { vm, view ->
                    view.questName.text = vm.name

                    view.questIcon.backgroundTintList =
                            ColorStateList.valueOf(colorRes(vm.color))

                    view.questIcon.setImageDrawable(listItemIcon(vm.icon))

                    view.questStartTime.text = vm.dueDate

                    view.questRepeatIndicator.visibility =
                            if (vm.isRepeating) View.VISIBLE else View.GONE
                    view.questChallengeIndicator.visibility =
                            if (vm.isFromChallenge) View.VISIBLE else View.GONE

                    if (vm.tags.isNotEmpty()) {
                        view.questTagName.visible()
                        renderTag(view, vm.tags.first())
                    } else {
                        view.questTagName.gone()
                    }

                    view.setOnClickListener {
                        rootRouter.pushController(
                            QuestViewController.routerTransaction(vm.id)
                        )
                    }
                }
            )

            registerBinder<ItemViewModel.CompletedQuestItem>(
                ViewType.COMPLETED_QUEST.value,
                R.layout.item_agenda_quest,
                { vm, view ->
                    val span = SpannableString(vm.name)
                    span.setSpan(StrikethroughSpan(), 0, vm.name.length, 0)

                    view.questName.text = span

                    view.questIcon.backgroundTintList =
                            ColorStateList.valueOf(colorRes(vm.color))
                    view.questIcon.setImageDrawable(listItemIcon(vm.icon))

                    view.questStartTime.text = vm.startTime

                    view.questRepeatIndicator.visibility =
                            if (vm.isRepeating) View.VISIBLE else View.GONE
                    view.questChallengeIndicator.visibility =
                            if (vm.isFromChallenge) View.VISIBLE else View.GONE

                    if (vm.tags.isNotEmpty()) {
                        view.questTagName.visible()
                        renderTag(view, vm.tags.first())
                    } else {
                        view.questTagName.gone()
                    }

                    view.setOnClickListener {
                        val handler = FadeChangeHandler()
                        rootRouter.pushController(
                            RouterTransaction
                                .with(CompletedQuestViewController(vm.id))
                                .pushChangeHandler(handler)
                                .popChangeHandler(handler)
                        )
                    }

                })
        }
    }

    private fun renderTag(view: View, tag: ItemViewModel.TagViewModel) {
        view.questTagName.text = tag.name
        TextViewCompat.setTextAppearance(
            view.questTagName,
            R.style.TextAppearance_AppCompat_Caption
        )

        val indicator = view.questTagName.compoundDrawablesRelative[0] as GradientDrawable
        indicator.mutate()
        val size = ViewUtils.dpToPx(8f, view.context).toInt()
        indicator.setSize(size, size)
        indicator.setColor(colorRes(tag.color))
        view.questTagName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            indicator,
            null,
            null,
            null
        )
    }

    private val BucketListViewState.Changed.itemViewModels: List<ItemViewModel>
        get() = items.map {
            when (it) {
                is CreateBucketListItemsUseCase.BucketListItem.QuestItem -> {

                    val q = it.quest

                    val color = if (q.isCompleted)
                        R.color.md_grey_500
                    else
                        q.color.androidColor.color500

                    if (q.isCompleted) {
                        ItemViewModel.CompletedQuestItem(
                            id = q.id,
                            name = q.name,
                            startTime = QuestStartTimeFormatter.formatWithDuration(q, activity!!, shouldUse24HourFormat),
                            color = color,
                            icon = q.icon?.androidIcon?.icon
                                    ?: Ionicons.Icon.ion_android_clipboard,
                            tags = q.tags.map {
                                ItemViewModel.TagViewModel(
                                    it.name,
                                    AndroidColor.valueOf(it.color.name).color500
                                )
                            },
                            isRepeating = q.isFromRepeatingQuest,
                            isFromChallenge = q.isFromChallenge
                        )
                    } else {

                        ItemViewModel.QuestItem(
                            id = q.id,
                            name = q.name,
                            dueDate = formatDueDate(q),
                            color = color,
                            icon = q.icon?.androidIcon?.icon
                                    ?: Ionicons.Icon.ion_android_clipboard,
                            tags = q.tags.map {
                                ItemViewModel.TagViewModel(
                                    it.name,
                                    AndroidColor.valueOf(it.color.name).color500
                                )
                            },
                            isRepeating = q.isFromRepeatingQuest,
                            isFromChallenge = q.isFromChallenge
                        )
                    }
                }

                is CreateBucketListItemsUseCase.BucketListItem.Today ->
                    ItemViewModel.SectionItem(stringRes(R.string.today))

                is CreateBucketListItemsUseCase.BucketListItem.Tomorrow ->
                    ItemViewModel.SectionItem(stringRes(R.string.tomorrow))

                is CreateBucketListItemsUseCase.BucketListItem.Upcoming ->
                    ItemViewModel.SectionItem(stringRes(R.string.upcoming))

                is CreateBucketListItemsUseCase.BucketListItem.Completed ->
                    ItemViewModel.SectionItem(stringRes(R.string.completed))

                is CreateBucketListItemsUseCase.BucketListItem.Overdue ->
                    ItemViewModel.SectionItem(stringRes(R.string.overdue))

                CreateBucketListItemsUseCase.BucketListItem.SomeDay ->
                    ItemViewModel.SectionItem(stringRes(R.string.someday))
            }

        }

    private fun formatDueDate(quest: Quest): String {
        if (quest.dueDate == null) {
            return QuestStartTimeFormatter.formatWithDuration(quest, activity!!, shouldUse24HourFormat)
        }
        val dueDate = quest.dueDate
        val today = LocalDate.now()
        return if (dueDate.isBefore(today)) {
            val overdueDays = dueDate.daysUntil(today)
            "Overdue by $overdueDays days"
        } else {
            "Due " + DateFormatter.formatWithoutYear(
                activity!!,
                dueDate
            ) + QuestStartTimeFormatter.formatWithDuration(quest, activity!!, shouldUse24HourFormat)
        }
    }
}