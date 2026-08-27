package com.reevan.reevzmealz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.ui.sin.SinViewModel
import com.reevan.reevzmealz.ui.bought.BoughtItemsScreen
import com.reevan.reevzmealz.ui.foods.FoodsScreen
import com.reevan.reevzmealz.ui.money.MoneySpentScreen
import com.reevan.reevzmealz.ui.plan.PlanMealScreen
import com.reevan.reevzmealz.ui.settings.SettingsScreen
import com.reevan.reevzmealz.ui.today.TodayScreen

/**
 * App shell: a single Scaffold owning the top bar and the bottom navigation bar, with the
 * selected section rendered as its content.
 *
 * Section switching is plain state rather than a navigation library. There is no nested
 * navigation, no deep linking and no argument passing yet, so a NavHost would be pure
 * ceremony. Add androidx.navigation.compose when sections actually grow sub-screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReevzMealzApp() {
    var section by rememberSaveable { mutableStateOf(AppSection.TODAY) }

    // Shared with Today and Settings: viewModel() resolves to the activity's store.
    val sinViewModel: SinViewModel = viewModel(factory = SinViewModel.Factory)
    val sinsLeft by sinViewModel.remaining.collectAsState()

    // Back from any other section returns to Today rather than leaving the app.
    BackHandler(enabled = section != AppSection.TODAY) {
        section = AppSection.TODAY
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(section.title) },
                actions = {
                    // Requirement: sins shown in the top-right corner, on every section.
                    Text(
                        text = "Sins " + sinsLeft,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (sinsLeft == 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                AppSection.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = section == entry,
                        onClick = { section = entry },
                        icon = {
                            Icon(
                                painter = painterResource(entry.icon),
                                contentDescription = entry.title,
                            )
                        },
                        label = { Text(entry.tabLabel, maxLines = 1) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (section) {
                AppSection.TODAY -> TodayScreen(
                    onGoToPlan = { section = AppSection.PLAN_MEAL },
                )
                AppSection.PLAN_MEAL -> PlanMealScreen()
                AppSection.BOUGHT_ITEMS -> BoughtItemsScreen()
                AppSection.FOODS -> FoodsScreen()
                AppSection.MONEY_SPENT -> MoneySpentScreen()
                AppSection.SETTINGS -> SettingsScreen()
            }
        }
    }
}
