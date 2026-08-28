package com.reevan.reevzmealz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.R
import com.reevan.reevzmealz.ui.sin.SinHealthBar
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
    var paused by rememberSaveable { mutableStateOf(false) }

    // Shared with Today and Settings: viewModel() resolves to the activity's store.
    val sinViewModel: SinViewModel = viewModel(factory = SinViewModel.Factory)
    val sinState by sinViewModel.uiState.collectAsState()

    // Back closes the pause menu first, then falls back to returning to Today rather than
    // leaving the app. Without the first step, opening pause and pressing back would jump the
    // user out of whichever section they had paused from.
    BackHandler(enabled = paused || section != AppSection.TODAY) {
        if (paused) paused = false else section = AppSection.TODAY
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                // Uppercased for the arcade feel; the monospace face does the rest. While paused
                // the header names the menu, not the section underneath it, which would otherwise
                // read as the title of a screen that is not on show.
                title = {
                    Text(
                        text = if (paused) "PAUSED" else section.title.uppercase(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                // Leading, not trailing: the top-right is the sin bar's, and the two would fight
                // for the same corner.
                navigationIcon = {
                    IconButton(onClick = { paused = !paused }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = if (paused) "Close menu" else "Menu",
                        )
                    }
                },
                actions = {
                    SinHealthBar(
                        remaining = sinState.status.remaining,
                        allowance = sinState.status.allowance,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                AppSection.tabs.forEach { entry ->
                    NavigationBarItem(
                        // Nothing is selected while paused, and while sitting in a section the
                        // bar no longer carries — neither is one of these four.
                        selected = !paused && section == entry,
                        onClick = {
                            paused = false
                            section = entry
                        },
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
            if (paused) {
                PauseMenu(
                    onOpenSection = { opened ->
                        section = opened
                        paused = false
                    },
                    onResume = { paused = false },
                )
            } else {
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
}
