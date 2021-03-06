package io.ipoli.android.challenge.list

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.support.annotation.ColorRes
import android.support.v4.widget.TextViewCompat
import android.view.View
import android.widget.TextView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.ionicons_typeface_library.Ionicons
import io.ipoli.android.R
import io.ipoli.android.challenge.entity.Challenge
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.datetime.daysUntil
import io.ipoli.android.common.navigation.Navigator
import io.ipoli.android.common.text.DateFormatter
import io.ipoli.android.common.text.DurationFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.MultiViewRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import kotlinx.android.synthetic.main.item_challenge.view.*
import kotlinx.android.synthetic.main.item_complete_challenge.view.*
import org.threeten.bp.LocalDate

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 7/17/18.
 */
sealed class ChallengeItem {

    data class Incomplete(val challenge: Challenge) : ChallengeItem()

    object CompleteLabel : ChallengeItem()
    data class Complete(val challenge: Challenge) : ChallengeItem()
}

fun createChallengeItems(challenges: List<Challenge>): List<ChallengeItem> {
    val (incomplete, complete) = challenges.partition { it.completedAtDate == null }

    val incompleteItems = incomplete.map { ChallengeItem.Incomplete(it) }
    return when {
        complete.isEmpty() -> incompleteItems
        else -> incompleteItems +
            ChallengeItem.CompleteLabel +
            complete.sortedByDescending { it.completedAtDate!! }.map { ChallengeItem.Complete(it) }
    }
}

const val INCOMPLETE_ITEM_VIEW_TYPE = 0
const val COMPLETE_LABEL_ITEM_VIEW_TYPE = 1
const val COMPLETE_ITEM_VIEW_TYPE = 2

class ChallengeAdapter : MultiViewRecyclerViewAdapter<ChallengeItemViewModel>() {
    override fun onRegisterItemBinders() {
        registerBinder<ChallengeItemViewModel.Incomplete>(
            INCOMPLETE_ITEM_VIEW_TYPE,
            R.layout.item_challenge
        ) { vm, view, _ ->
            bindIncompleteChallenge(view, vm)
        }

        registerBinder<ChallengeItemViewModel.CompleteLabel>(
            COMPLETE_LABEL_ITEM_VIEW_TYPE,
            R.layout.item_list_section
        ) { vm, view, _ ->
            bindCompleteLabel(view, vm)
        }

        registerBinder<ChallengeItemViewModel.Complete>(
            COMPLETE_ITEM_VIEW_TYPE,
            R.layout.item_complete_challenge
        ) { vm, view, _ ->
            bindCompleteChallenge(view, vm)
        }

    }

    private fun bindCompleteChallenge(
        view: View,
        vm: ChallengeItemViewModel.Complete
    ) {
        view.ccName.text = vm.name

        view.ccIcon.backgroundTintList =
            ColorStateList.valueOf(view.context.colorRes(vm.color))
        view.ccIcon.setImageDrawable(
            IconicsDrawable(view.context).listItemIcon(vm.icon)
        )

        view.ccStart.text = vm.start
        view.ccComplete.text = vm.complete

        view.setOnClickListener {
            vm.clickListener()
        }
    }

    private fun bindCompleteLabel(
        view: View,
        vm: ChallengeItemViewModel.CompleteLabel
    ) {
        (view as TextView).text = vm.label
    }

    private fun bindIncompleteChallenge(
        view: View,
        vm: ChallengeItemViewModel.Incomplete
    ) {
        view.cName.text = vm.name

        view.cIcon.backgroundTintList =
            ColorStateList.valueOf(view.context.colorRes(vm.color))
        view.cIcon.setImageDrawable(
            IconicsDrawable(view.context).listItemIcon(vm.icon)
        )

        if (vm.tags.isNotEmpty()) {
            view.cTagName.visible()
            renderTag(view, vm.tags.first())
        } else {
            view.cTagName.gone()
        }

        view.cNext.text = vm.next
        view.cEnd.text = vm.end

        view.cProgress.max = vm.progressMax
        view.cProgress.progress = vm.progress

        view.setOnClickListener {
            vm.clickListener()
        }
    }

    private fun renderTag(view: View, tag: TagViewModel) {
        view.cTagName.text = tag.name
        TextViewCompat.setTextAppearance(
            view.cTagName,
            R.style.TextAppearance_AppCompat_Caption
        )

        val indicator = view.cTagName.compoundDrawablesRelative[0] as GradientDrawable
        indicator.mutate()
        val size = ViewUtils.dpToPx(8f, view.context).toInt()
        indicator.setSize(size, size)
        indicator.setColor(view.context.colorRes(tag.color))
        view.cTagName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            indicator,
            null,
            null,
            null
        )
    }
}

data class TagViewModel(val name: String, @ColorRes val color: Int)

sealed class ChallengeItemViewModel(override val id: String) : RecyclerViewViewModel {

    data class Incomplete(
        override val id: String,
        val name: String,
        val tags: List<TagViewModel>,
        val icon: IIcon,
        @ColorRes val color: Int,
        val next: String,
        val end: String,
        val progress: Int,
        val progressMax: Int,
        val clickListener : () -> Unit = {}

    ) : ChallengeItemViewModel(id)

    data class CompleteLabel(val label: String) : ChallengeItemViewModel(label)

    data class Complete(
        override val id: String,
        val name: String,
        val tags: List<TagViewModel>,
        val icon: IIcon,
        @ColorRes val color: Int,
        val start: String,
        val complete: String,
        val clickListener : () -> Unit = {}
    ) : ChallengeItemViewModel(id)
}

fun toChallengeViewModels(
    context: Context,
    challenges: List<ChallengeItem>,
    shouldUse24HourFormat: Boolean,
    navigator: Navigator? = null
): List<ChallengeItemViewModel> {
    return challenges.map {
        when (it) {
            is ChallengeItem.Incomplete -> {
                val c = it.challenge
                val next = when {
                    c.nextDate != null -> {
                        var res = context.stringRes(
                            R.string.repeating_quest_next,
                            DateFormatter.format(context, c.nextDate)
                        )
                        res += if (c.nextStartTime != null) {
                            " ${c.nextStartTime.toString(shouldUse24HourFormat)} - ${c.nextEndTime!!.toString(
                                shouldUse24HourFormat
                            )}"
                        } else {
                            " " + context.stringRes(
                                R.string.for_time,
                                DurationFormatter.formatShort(context, c.nextDuration!!)
                            )
                        }
                        res
                    }
                    else -> context.stringRes(
                        R.string.repeating_quest_next,
                        context.stringRes(R.string.unscheduled)
                    )
                }

                val daysUntilComplete = LocalDate.now().daysUntil(c.endDate)

                val end = when {
                    daysUntilComplete < 0L -> context.stringRes(
                        R.string.inbox_overdue_by,
                        Math.abs(daysUntilComplete),
                        context.stringRes(R.string.days).toLowerCase()
                    )
                    daysUntilComplete == 0L -> context.stringRes(R.string.ends_today)
                    daysUntilComplete <= 7 -> context.stringRes(
                        R.string.ends_in_days,
                        daysUntilComplete
                    )
                    else -> context.stringRes(
                        R.string.ends_at_date,
                        DateFormatter.formatWithoutYear(context, c.endDate)
                    )
                }

                ChallengeItemViewModel.Incomplete(
                    id = c.id,
                    name = c.name,
                    tags = c.tags.map { TagViewModel(it.name, AndroidColor.valueOf(it.color.name).color500) },
                    color = AndroidColor.valueOf(c.color.name).color500,
                    icon = c.icon?.let { AndroidIcon.valueOf(it.name).icon }
                        ?: Ionicons.Icon.ion_android_clipboard,
                    next = next,
                    end = end,
                    progress = c.progress.completedCount,
                    progressMax = c.progress.allCount,
                    clickListener = {
                        navigator?.toChallenge(c.id)
                    }
                )
            }

            is ChallengeItem.CompleteLabel ->
                ChallengeItemViewModel.CompleteLabel(context.stringRes(R.string.completed))

            is ChallengeItem.Complete ->
                with(it.challenge) {
                    ChallengeItemViewModel.Complete(
                        id = id,
                        name = name,
                        tags = tags.map {
                            TagViewModel(
                                it.name,
                                AndroidColor.valueOf(it.color.name).color500
                            )
                        },
                        color = AndroidColor.valueOf(color.name).color500,
                        icon = icon?.let { AndroidIcon.valueOf(it.name).icon }
                            ?: Ionicons.Icon.ion_android_clipboard,
                        start = context.stringRes(
                            R.string.started_at_date,
                            DateFormatter.format(context, startDate)
                        ),
                        complete = context.stringRes(
                            R.string.completed_at_date,
                            DateFormatter.format(context, completedAtDate)
                        ),
                        clickListener = {
                            navigator?.toChallenge(id)
                        }
                    )
                }
        }


    }
}