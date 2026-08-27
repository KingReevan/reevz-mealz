package com.reevan.reevzmealz.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reevan.reevzmealz.util.addDays
import com.reevan.reevzmealz.util.addMonths
import com.reevan.reevzmealz.util.formatDayOfMonth
import com.reevan.reevzmealz.util.formatMonthAndYear
import com.reevan.reevzmealz.util.formatWeekdayShort
import com.reevan.reevzmealz.util.isSameDay
import com.reevan.reevzmealz.util.monthGridOf
import com.reevan.reevzmealz.util.weekDaysOf
import com.reevan.reevzmealz.util.weekdayHeadings

enum class PickerMode(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
}

/**
 * Day picker for planning, in either a one-week strip or a month grid.
 *
 * [plannedDays] are day-starts that already have something planned; they get a marker so a month
 * of planning is legible at a glance.
 */
@Composable
fun DayPicker(
    mode: PickerMode,
    onModeChange: (PickerMode) -> Unit,
    selectedDay: Long,
    onSelectDay: (Long) -> Unit,
    anchorDay: Long,
    onAnchorChange: (Long) -> Unit,
    plannedDays: Set<Long>,
    today: Long,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
        ) {
            PickerMode.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = mode == entry,
                    onClick = { onModeChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PickerMode.entries.size,
                    ),
                    label = { Text(entry.label) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    onAnchorChange(
                        if (mode == PickerMode.WEEK) {
                            addDays(anchorDay, -DAYS_IN_WEEK)
                        } else {
                            addMonths(anchorDay, -1)
                        },
                    )
                },
            ) {
                Text("‹")
            }
            Text(
                text = formatMonthAndYear(anchorDay),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    onAnchorChange(
                        if (mode == PickerMode.WEEK) {
                            addDays(anchorDay, DAYS_IN_WEEK)
                        } else {
                            addMonths(anchorDay, 1)
                        },
                    )
                },
            ) {
                Text("›")
            }
        }

        when (mode) {
            PickerMode.WEEK -> WeekStrip(
                anchorDay = anchorDay,
                selectedDay = selectedDay,
                onSelectDay = onSelectDay,
                plannedDays = plannedDays,
                today = today,
            )

            PickerMode.MONTH -> MonthGrid(
                anchorDay = anchorDay,
                selectedDay = selectedDay,
                onSelectDay = onSelectDay,
                plannedDays = plannedDays,
                today = today,
            )
        }
    }
}

@Composable
private fun WeekStrip(
    anchorDay: Long,
    selectedDay: Long,
    onSelectDay: (Long) -> Unit,
    plannedDays: Set<Long>,
    today: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        weekDaysOf(anchorDay).forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatWeekdayShort(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DayCell(
                    day = day,
                    selected = isSameDay(day, selectedDay),
                    isToday = isSameDay(day, today),
                    hasPlan = plannedDays.contains(day),
                    onClick = { onSelectDay(day) },
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    anchorDay: Long,
    selectedDay: Long,
    onSelectDay: (Long) -> Unit,
    plannedDays: Set<Long>,
    today: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayHeadings().forEach { heading ->
                Text(
                    text = heading,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        monthGridOf(anchorDay).chunked(DAYS_IN_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                selected = isSameDay(day, selectedDay),
                                isToday = isSameDay(day, today),
                                hasPlan = plannedDays.contains(day),
                                onClick = { onSelectDay(day) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Long,
    selected: Boolean,
    isToday: Boolean,
    hasPlan: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            .padding(2.dp)
            .size(CELL_SIZE)
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDayOfMonth(day),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                // Marker for a day that already has a plan.
                Box(
                    modifier = Modifier
                        .size(PLAN_DOT_SIZE)
                        .clip(CircleShape),
                ) {
                    if (hasPlan) {
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(PLAN_DOT_SIZE),
                            content = {},
                        )
                    }
                }
            }
        }
    }
}

private const val DAYS_IN_WEEK = 7
private val CELL_SIZE = 44.dp
private val PLAN_DOT_SIZE = 4.dp
