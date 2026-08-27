package com.reevan.reevzmealz.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.util.RETAINED_MONTHS
import com.reevan.reevzmealz.util.formatPaise

/**
 * What a chosen day, week, month or year cost: groceries from Bought Items, food eaten outside,
 * and the two added together. Defaults to the current month.
 */
@Composable
fun MoneySpentScreen(
    modifier: Modifier = Modifier,
    viewModel: MoneySpentViewModel = viewModel(factory = MoneySpentViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
            onBack = viewModel::goBack,
            onForward = viewModel::goForward,
        )
        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                )
            }

            SplitBar(
                boughtItemsShare = state.boughtItemsShare,
                hasSpend = state.totalPaise > 0L,
            )

            SpendRow(
                label = "Bought Items",
                detail = "Groceries and ingredients",
                paise = state.boughtItemsPaise,
                accent = MaterialTheme.colorScheme.primary,
            )
            SpendRow(
                label = "Outside food",
                detail = "Meals not cooked at home",
                paise = state.outsideFoodPaise,
                accent = MaterialTheme.colorScheme.error,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!state.canGoBack) {
                Text(
                    text = "Only the last $RETAINED_MONTHS months are kept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PeriodNavigator(
    label: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack, enabled = canGoBack) {
            Text("‹")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onForward, enabled = canGoForward) {
            Text("›")
        }
    }
}

/** Proportion of the total from each stream, so the split reads at a glance. */
@Composable
private fun SplitBar(boughtItemsShare: Float, hasSpend: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
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
        if (boughtItemsShare > 0f) {
            Box(
                modifier = Modifier
                    .weight(boughtItemsShare)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        if (outsideShare > 0f) {
            Box(
                modifier = Modifier
                    .weight(outsideShare)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error),
            )
        }
    }
}

@Composable
private fun SpendRow(label: String, detail: String, paise: Long, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail,
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
