package com.reevan.reevzmealz.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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

/** How a slot arranges its name against its foods. */
enum class SlotLayout {
    /**
     * Name and cost centred above the foods. Today's layout — it reads as a headline, and Today
     * has the vertical room for it because it carries no day picker.
     */
    CENTRED,

    /**
     * Name and cost in a fixed left column, foods stacked as blocks down the right. Plan Meal's
     * layout: the day picker takes roughly a third of that screen, so the name and the foods
     * sharing a line instead of stacking is what keeps dinner closer to the fold.
     */
    SIDE_BY_SIDE,
}

/** Width of the name column in [SlotLayout.SIDE_BY_SIDE]; fits "Breakfast" at 18sp monospace. */
private val NAME_COLUMN_WIDTH = 136.dp

/**
 * One meal slot: its name and cost, then the foods in it.
 *
 * Full-bleed and rectangular, with a divider closing it off, so the slots read as one continuous
 * list rather than separate floating cards. A Card's gaps and rounded insets cost height without
 * adding information, and all four slots need to fit.
 *
 * Shared by Today and Plan Meal so the two cannot drift apart. They differ only through
 * [layout], [actions], [emptyText] and [supportingLine] — do not fork this file.
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
    layout: SlotLayout = SlotLayout.CENTRED,
    supportingLine: String? = null,
    emptyText: String = "Nothing yet",
    actions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        when (layout) {
            SlotLayout.CENTRED -> CentredSlot(
                slot = slot,
                canRemove = canRemove,
                onRemoveFood = onRemoveFood,
                supportingLine = supportingLine,
                emptyText = emptyText,
                actions = actions,
            )

            SlotLayout.SIDE_BY_SIDE -> SideBySideSlot(
                slot = slot,
                canRemove = canRemove,
                onRemoveFood = onRemoveFood,
                supportingLine = supportingLine,
                emptyText = emptyText,
                actions = actions,
            )
        }

        // Thick enough to read as a HUD seam between blocks rather than a hairline rule.
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun CentredSlot(
    slot: PlanSlot,
    canRemove: Boolean,
    onRemoveFood: (Long) -> Unit,
    supportingLine: String?,
    emptyText: String,
    actions: (@Composable () -> Unit)?,
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
            SlotCostPill(slot = slot, modifier = Modifier.padding(start = 10.dp))
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
}

/**
 * Name on the left, foods stacked as blocks on the right.
 *
 * The two columns are top-aligned, so the name sits level with the first block and the stack
 * grows downwards like bricks. The slot is exactly as tall as its taller column, which is the
 * saving: an empty slot is a single line of text tall instead of three stacked rows.
 */
@Composable
private fun SideBySideSlot(
    slot: PlanSlot,
    canRemove: Boolean,
    onRemoveFood: (Long) -> Unit,
    supportingLine: String?,
    emptyText: String,
    actions: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            // Sizes the row to its tallest column so the divider between the two can fill it.
            // Without this the row is wrap-content, and a full-height child has no height to fill.
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .width(NAME_COLUMN_WIDTH)
                .padding(top = 4.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = slot.type.label,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            SlotCostPill(slot = slot, compact = true)
            if (supportingLine != null) {
                Text(
                    text = supportingLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.padding(end = 10.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (slot.isEmpty) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                slot.foods.forEach { food ->
                    SlotFoodBlock(
                        food = food,
                        canRemove = canRemove,
                        onRemove = { onRemoveFood(food.entryId) },
                    )
                }
            }

            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actions()
                }
            }
        }
    }
}

/**
 * The slot's headline figure. A slot of only homecooked food has no price to show, so it says so
 * rather than showing a zero. [compact] shortens that label for the narrow name column.
 */
@Composable
private fun SlotCostPill(
    slot: PlanSlot,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (slot.isEmpty) return

    val homecookedOnly = slot.isFullyHomecooked
    Surface(
        color = if (homecookedOnly) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Text(
            text = when {
                !homecookedOnly -> formatPaise(slot.costPaise)
                compact -> "Home"
                else -> "All homecooked"
            },
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

/** One food inside a centred slot: name, then a chip carrying its price or that it was homecooked. */
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
            border = BorderStroke(
                width = 2.dp,
                color = if (isOutside) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ),
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

/**
 * One food as a brick in the stack: a bordered block carrying the name, then its price.
 *
 * Bordered in error red when the food was bought outside, so the slot's cost is visible in the
 * stack itself rather than only in the pill.
 */
@Composable
private fun SlotFoodBlock(food: SlotFood, canRemove: Boolean, onRemove: () -> Unit) {
    val isOutside = food.source == MealPlace.OUT
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(
            width = 2.dp,
            color = if (isOutside) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(start = 10.dp, end = if (canRemove) 0.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (isOutside) formatPaise((food.pricePaise ?: 0).toLong()) else "Home",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOutside) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (canRemove) {
                TextButton(onClick = onRemove) {
                    Text(text = "✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
