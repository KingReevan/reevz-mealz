package com.reevan.reevzmealz.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.SlotFood
import com.reevan.reevzmealz.util.formatPaise

/**
 * One meal slot: its name and cost centred on a single line, then the foods in it.
 *
 * Full-bleed and rectangular, with a divider closing it off, so the four slots read as one
 * continuous list rather than four floating cards. That is deliberate: all four slots have to fit
 * on screen at once, and a Card's gaps and rounded insets cost height without adding information.
 *
 * Shared by Today and Plan Meal so the two cannot drift apart visually. The screens differ only
 * in their [actions] and in whether removal is offered, which is what those parameters are for.
 *
 * [canRemove] must be false whenever the rows on screen did not come from the table the remove
 * callback writes to — on Today that means before the day is logged.
 */
@Composable
fun MealSlotCard(
    slot: PlanSlot,
    canRemove: Boolean,
    onRemoveFood: (Long) -> Unit,
    modifier: Modifier = Modifier,
    supportingLine: String? = null,
    emptyText: String = "Nothing yet",
    actions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Name and cost share a line; stacked, they alone were taller than a whole slot needs.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = slot.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                SlotCostPill(slot = slot)
            }

            if (supportingLine != null) {
                Text(
                    text = supportingLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (slot.isEmpty) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                slot.foods.forEach { food ->
                    SlotFoodRow(
                        food = food,
                        canRemove = canRemove,
                        onRemove = { onRemoveFood(food.entryId) },
                    )
                }
            }

            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actions()
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * The slot's headline figure, sitting beside its name. A slot of only homecooked food has no price
 * to show, so it says so rather than showing a zero.
 */
@Composable
private fun SlotCostPill(slot: PlanSlot) {
    if (slot.isEmpty) return

    val homecookedOnly = slot.isFullyHomecooked
    Surface(
        color = if (homecookedOnly) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(start = 10.dp),
    ) {
        Text(
            text = if (homecookedOnly) "All homecooked" else formatPaise(slot.costPaise),
            style = MaterialTheme.typography.labelMedium,
            color = if (homecookedOnly) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** One food inside a slot: name, then a chip carrying its price or that it was cooked at home. */
@Composable
private fun SlotFoodRow(food: SlotFood, canRemove: Boolean, onRemove: () -> Unit) {
    val isOutside = food.source == MealPlace.OUT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = food.name,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Surface(
            color = if (isOutside) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.padding(start = 10.dp),
        ) {
            Text(
                text = if (isOutside) formatPaise((food.pricePaise ?: 0).toLong()) else "Home",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOutside) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (canRemove) {
            TextButton(
                onClick = onRemove,
                modifier = Modifier.padding(start = 2.dp),
            ) {
                Text(text = "✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The "+ Add food" action, shared so both screens word and size it identically. */
@Composable
fun AddFoodAction(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text("+ Add food")
    }
}

/** A destructive slot action — "Clear" when planning, "Ate nothing" when recording. */
@Composable
fun ClearSlotAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(text = label, color = MaterialTheme.colorScheme.error)
    }
}
