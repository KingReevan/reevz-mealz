package com.reevan.reevzmealz.ui.money

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.ui.common.QuantityTag
import com.reevan.reevzmealz.util.RETAINED_MONTHS
import com.reevan.reevzmealz.util.formatDayHeading
import com.reevan.reevzmealz.util.formatPaise

/**
 * What a chosen day, week, month or year cost: groceries from Bought Items, food eaten outside,
 * the two added together, and then **every item behind that number**.
 *
 * The itemised list is the point of the screen. Two totals alone told you that ₹365 went somewhere
 * without telling you where, which is no use for deciding what to cut.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoneySpentScreen(
    modifier: Modifier = Modifier,
    viewModel: MoneySpentViewModel = viewModel(factory = MoneySpentViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshNow()
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(48.dp),
        ) {
            SpendPeriod.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = state.period == entry,
                    onClick = { viewModel.selectPeriod(entry) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = SpendPeriod.entries.size,
                    ),
                    label = { Text(entry.label) },
                )
            }
        }

        PeriodNavigator(
            label = state.label,
            canGoBack = state.canGoBack,
            canGoForward = state.canGoForward,
            isCurrent = state.isCurrent,
            onBack = viewModel::goBack,
            onForward = viewModel::goForward,
            onToday = viewModel::goToCurrent,
        )
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "summary") {
                Summary(state = state)
            }

            if (state.isEmpty) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Nothing spent yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                item(key = "breakdown-heading") {
                    Text(
                        text = "WHERE IT WENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 8.dp,
                        ),
                    )
                }

                // Day headings would only repeat what the navigator and the headline total
                // already say when the whole window *is* one day, so the Day view stays flat.
                val showDayHeadings = state.period != SpendPeriod.DAY

                state.days.forEach { section ->
                    if (showDayHeadings) {
                        stickyHeader(key = "day-${section.dayStart}") {
                            DayHeader(
                                label = formatDayHeading(section.dayStart),
                                totalPaise = section.totalPaise,
                            )
                        }
                    }

                    items(
                        count = section.lines.size,
                        key = { index -> "line-${section.dayStart}-$index" },
                    ) { index ->
                        // Between rows within a day only. The heading already separates one day
                        // from the next, and a rule under the last row would box the group in.
                        if (index > 0) {
                            SpendDivider()
                        }
                        val line = section.lines[index]
                        SpendItemRow(
                            name = line.name,
                            quantity = line.quantity,
                            detail = line.detail,
                            paise = line.pricePaise,
                            // Blue for groceries, not pink: pink sat 25 degrees of hue from the
                            // red with identical lightness, so at 3dp the two stripes were
                            // indistinguishable. Blue is 156 degrees away, and already means
                            // "home" on the meal slots — which is what bought items are.
                            accent = when (line.stream) {
                                SpendStream.OUTSIDE_FOOD -> MaterialTheme.colorScheme.error
                                SpendStream.BOUGHT_ITEM -> MaterialTheme.colorScheme.secondary
                            },
                        )
                    }
                }
            }

            if (!state.canGoBack) {
                item(key = "retention") {
                    Text(
                        text = "Only the last $RETAINED_MONTHS months are kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

/** The headline figure, the split between the two streams, and each stream's total. */
@Composable
private fun Summary(state: MoneySpentUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                text = "Total spent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatPaise(state.totalPaise),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        SplitBar(
            boughtItemsShare = state.boughtItemsShare,
            hasSpend = state.totalPaise > 0L,
        )

        StreamTotal(
            label = "Outside food",
            count = state.outsideFoods.size,
            paise = state.outsideFoodPaise,
            accent = MaterialTheme.colorScheme.error,
        )
        StreamTotal(
            label = "Bought items",
            count = state.boughtItems.size,
            paise = state.boughtItemsPaise,
            accent = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun PeriodNavigator(
    label: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isCurrent: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack, enabled = canGoBack) {
            Text("‹")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            // Only offered when it would actually move: otherwise it is a button that does nothing.
            if (!isCurrent) {
                TextButton(onClick = onToday) {
                    Text("Back to now", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        TextButton(onClick = onForward, enabled = canGoForward) {
            Text("›")
        }
    }
}

/**
 * The hairline between two breakdown rows.
 *
 * Its own composable because the two `items` blocks both draw it and the breakdown has to look
 * like one continuous list across the seam between them.
 */
@Composable
private fun SpendDivider() {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** Proportion of the total from each stream, so the split reads at a glance. */
@Composable
private fun SplitBar(boughtItemsShare: Float, hasSpend: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        if (!hasSpend) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            return@Row
        }
        val outsideShare = 1f - boughtItemsShare
        if (outsideShare > 0f) {
            Box(
                modifier = Modifier
                    .weight(outsideShare)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error),
            )
        }
        if (boughtItemsShare > 0f) {
            Box(
                modifier = Modifier
                    .weight(boughtItemsShare)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary),
            )
        }
    }
}

/** One stream's total, with how many items are behind it. */
@Composable
private fun StreamTotal(label: String, count: Int, paise: Long, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 28.dp)
                .background(accent),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (count == 1) "1 item" else "$count items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatPaise(paise),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * One day's spending as a heading, with what the day came to.
 *
 * Sticky, and the same shape as Bought Items' month heading — both are "a period, and its total",
 * so they should not look like two different ideas. Filled with `surface` rather than left
 * transparent because it scrolls over the rows beneath it and has to stay opaque while it does.
 */
@Composable
private fun DayHeader(label: String, totalPaise: Long) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatPaise(totalPaise),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            // The same 2dp seam the meal slots use, so a day reads as one block of the HUD.
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** One line of the breakdown: what, where from, how much. The day is on the heading above it. */
@Composable
private fun SpendItemRow(
    name: String,
    quantity: Int,
    detail: String?,
    paise: Long,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 28.dp)
                .background(accent),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            // The tag beside the name is what explains a doubled figure on the right.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                QuantityTag(quantity = quantity)
            }
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = formatPaise(paise),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
