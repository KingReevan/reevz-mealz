# Reevz Mealz

## What this project is

Reevz Mealz is a **personal Android app** for managing meals, meal planning, food spending, and
eating-out frequency. The primary goal is to make it easier for me to decide what to eat, and to
reduce unnecessary eating out and unnecessary spending.

It is a personal app — not a multi-user SaaS product. Treat that as a design constraint, not a
temporary phase.

## Stack

- Kotlin + Jetpack Compose
- Material 3
- Room for local persistence
- MVVM
- Android-first
- Local-first / offline-first

## Core principles

- Keep the app simple and fast to use.
- **Do not introduce a backend, authentication, cloud database, analytics, or AI** unless I
  explicitly ask for it.
- Prefer local storage and offline functionality.
- Prefer Android/Jetpack libraries when they are appropriate.
- Keep the architecture understandable rather than over-engineered.
- Do not build abstractions for hypothetical future requirements. Solve the problem in front of you.

## Working rules

- Make small, focused changes. Do not modify unrelated files.
- Preserve existing functionality when implementing new features.
- **Before making an architectural change, explain the reason first.**
- **Before adding a dependency, explain why it is needed** and why an existing library or the
  standard library is not enough.
- Run the appropriate build/tests after significant changes.
- **Never claim a feature works without verifying it.** If verification did not run, or failed, say
  so plainly and show the output.
- Do not silently change project-wide configuration (Gradle files, version catalog,
  `gradle.properties`, manifest, theme). If a config change is genuinely required, call it out and
  explain it.

### Feature workflow

1. Inspect the existing implementation.
2. Explain the proposed approach briefly.
3. Make the smallest reasonable change.
4. Build/test the project.
5. Report what changed and whether verification succeeded.

## Build and verify

Windows / PowerShell (primary shell here):

```
.\gradlew.bat assembleDebug          # compile the app
.\gradlew.bat testDebugUnitTest      # host unit tests
.\gradlew.bat lint                   # Android lint
```

Instrumented tests (`connectedDebugAndroidTest`) need a running emulator or device; don't assume one
is attached.

## Git

- Keep commits small and logically focused.
- Do not commit generated build artifacts, `local.properties`, IDE-specific files, or secrets.
- Do not rewrite Git history unless explicitly requested.
- **Do not push to GitHub unless explicitly requested.**
- Note: `.gitignore` currently ignores only a few `.idea/*` paths, so `.idea/` shows up as
  untracked. Do not commit it.

## UI guidelines

- Design primarily for a **phone**.
- Optimize for quick **one-handed** interaction — common actions reachable with a thumb.
- Prioritize clarity and ease of use over visual complexity.
- Avoid unnecessary screens and navigation. Fewer taps to log a meal is the goal.
- Use Jetpack Compose and Material 3.
- Support **light and dark themes** — `ReevzMealzTheme` owns the retro palette for both, so route
  new UI through it and use colour *roles*, never hardcoded colours.
- Use accessible touch targets (48dp minimum) and readable typography.

## Data guidelines

- Meal data is stored **locally**.
- Once persistent data exists, database schema changes **must** use proper Room migrations. Do not
  rely on `fallbackToDestructiveMigration`.
- Room schemas are exported to `app/schemas/` and **are committed** — they are the baseline every
  future migration is written against. Schema changes mean bumping `@Database(version = ...)` and
  adding a real `Migration`.
- Additive schema changes (new table, new nullable column) should use `@AutoMigration`, which Room
  generates from the exported schema diff — far safer than hand-writing `CREATE TABLE` SQL that
  must match Room's expected hash exactly. Non-additive changes still need a manual `Migration`.
- Room migrations can only be tested with instrumented tests (`MigrationTestHelper`), so they need
  a device. Say plainly when a migration has not been exercised on one.
- Do not delete or reset user data as part of a normal feature implementation. The **one**
  sanctioned bulk delete is `MaintenanceDao.purgeOlderThan`, the 12-month retention window the
  user asked for. It is manual (a button in Settings), shows exact row counts, and requires
  confirmation before deleting. It never touches `foods` or the superseded `meals` table. Do not
  make it automatic and do not widen its scope without asking.
- Treat user-entered meal and spending data as valuable and non-recoverable.

## Current project state

Single Gradle module `:app`, package `com.reevan.reevzmealz`. Room persists everything locally,
fully offline. Target device is a **Nothing Phone (2a)**; `minSdk 24` / `targetSdk 37` covers it.

**Today** shows the current day: all four meal slots (breakfast, lunch, snack, dinner) and the
day's total pinned at the bottom. **All four slots must be visible without scrolling** — that is a
hard requirement, and it is why the slots are compact and why Edit Mode and End day share the
header rather than taking a row each. Read-only by default, showing the plan. An **Edit Mode**
switch turns it into a record of what was *actually* eaten — seeded from the plan on first use, then
freely editable (add foods from Foods, remove them, or reset the day back to the plan). Editing
never touches the plan.

**Plan Meal** picks a day (week strip or month grid, toggleable) and fills its four slots with
foods from the Foods section. Several foods per slot. Full CRUD over assignments — but **only
while the day is still in the future**: at that day's own midnight the plan locks, and what
actually happens is recorded through Today's Edit Mode instead. Past and present days stay
viewable, just not editable. Its slots use
the **side-by-side** layout — name on the left, foods stacked as blocks on the right — so all four
fit under the day picker.

The app shell has six top-level sections. Four sit in the bottom navigation bar — **Today, Plan
Meal, Bought Items, Foods** — and the two occasional ones, **Money Spent** and **Settings**, live
behind a **pause menu** opened from the ☰ button at the top-left. All six are built.

**Foods** is searchable by name and filterable by source (All / Home / Outside), and every row
ends in a tag: the place for outside food, a green **Home** for homecooked. It holds atomic
food items — name, a Homecooked/Outside toggle, and a price plus a **place**
(where it was bought) that only apply when Outside. The place shows as a chip on the right of each
row. They are the building blocks meal planning will draw on. Full CRUD.

**Bought Items** records grocery/ingredient purchases — name, price, date — as a month-wise
history in the style of a payments app's transaction list: a sticky month heading carrying that
month's total, then one card per purchase, newest first. Full CRUD.

**Money Spent** totals a chosen day, week, month or year (toggle, Google-Calendar style, with
‹ › navigation), split into Outside food and Bought Items, and then lists **every item behind
that total** — grouped under a sticky date heading carrying that day's total, with each item's
place and price. Opens on **today**; picking Week/Month/Year jumps to
the current one. Browsing stops at the 12-month retention window, and the current period counts
only up to today.

**Settings** is the single home for configuration, as bordered panels: **Theme**
(System/Light/Dark), **Notifications** (nightly reminder on/off and time), **Sins** (monthly
allowance), **Storage** (the retention purge). New settings belong here as another panel.

**Notifications**: one nightly reminder, default 19:00 device-local, which fires only when
tomorrow has *nothing* planned at all.

**Sins** are the discipline mechanic. One sin = one meal that did not go to plan. The allowance
(default 40) is per calendar month and configurable, but **locked for 3 days after being set** so
it cannot be raised the moment it pinches. Remaining sins show as a **health bar** in the header of
every screen, draining pink to yellow to red and reading GAME OVER at zero.
An **End day** button in Today's header opens a dialog with a toggle per meal slot — positive means
"Good Job!" in green, negative means "This is bad!" in red and costs a sin. Confirming settles the
day permanently. At zero the dialog is headed "You have failed for the month".

```
app/src/main/java/com/reevan/reevzmealz/
├── MainActivity.kt              single ComponentActivity, hosts ReevzMealzApp
├── data/
│   ├── Meal.kt                  MealType / MealPlace enums + superseded @Entity (see below)
│   ├── MealDao.kt               unused; kept so the meals table survives
│   ├── PlannedMeal.kt           @Entity (day, slot, foodId) + SlotFood join POJO
│   ├── PlannedMealDao.kt        observeDay(Flow), observePlannedDays, insert, delete
│   ├── EatenMeal.kt             @Entity actuals + EatenDay marker entity
│   ├── EatenMealDao.kt          observeDay, observeIsLogged, startLoggingDay, resetDayToPlan
│   ├── Food.kt                  @Entity, atomic food item, nullable pricePaise + place
│   ├── FoodDao.kt               observeAll(Flow), insert, update, delete
│   ├── BoughtItem.kt            @Entity, a purchase: name, pricePaise, boughtAt
│   ├── BoughtItemDao.kt         observeAll(Flow), insert, update, delete
│   ├── AppSettings.kt           @Entity, theme + reminder prefs, ThemeMode enum
│   ├── AppSettingsDao.kt        observe/get/upsert
│   ├── Sin.kt                   SinEvent, EndedDay, SinSettings entities
│   ├── SinDao.kt                sin counts, endDay transaction, allowance settings
│   ├── SpendDao.kt              read-only period totals for Money Spent
│   ├── MaintenanceDao.kt        12-month retention purge (the only bulk delete)
│   └── MealDatabase.kt          @Database v8, singleton, exportSchema, autoMigrations
├── notify/
│   ├── PlanReminderScheduler.kt  AlarmManager arming + pure nextTriggerAt
│   ├── PlanReminderReceiver.kt   checks tomorrow, notifies, re-arms
│   └── BootReceiver.kt           re-arms after reboot
├── ui/
│   ├── AppSection.kt            the six sections: title, tab label, icon, inBottomBar
│   ├── PauseMenu.kt             Money Spent + Settings, reached from the header
│   ├── ReevzMealzApp.kt         shell: the one Scaffold + bottom NavigationBar
│   ├── common/
│   │   ├── PlanSlots.kt          PlanSlot, buildSlots/totalCostPaise, effectiveSlots
│   │   ├── FoodPickerSheet.kt    shared food picker: source -> place -> food, + new food
│   │   ├── FilterField.kt        the one filter text box, used by the picker and Foods
│   │   └── SectionPlaceholder.kt  stand-in body for unbuilt sections
│   ├── theme/                   Theme.kt, Color.kt (retro palette), Type.kt, Shape.kt
│   ├── today/                   built: TodayScreen, TodayViewModel
│   ├── foods/                    built: FoodsScreen, FoodsViewModel, FoodEditorSheet
│   ├── bought/                   built: BoughtItemsScreen/ViewModel, editor, MonthGrouping
│   ├── plan/                     built: PlanMealScreen/ViewModel, DayPicker, PlanLock
│   ├── sin/                      SinStatus (rules), SinViewModel, EndDayDialog, SinHealthBar
│   ├── money/                    built: MoneySpentScreen/ViewModel, SpendPeriod, SpendGrouping
│   └── settings/                 built: SettingsScreen/ViewModel (retention purge)
└── util/
    ├── Money.kt                 paise <-> "₹420.50", editable-field rendering
    ├── FoodFormat.kt            food source labels and row subtitles
    ├── TextCase.kt              capitalizeWords, for food and place names
    ├── PlaceSuggestions.kt      knownPlaces / suggestPlaces, the Place autocomplete
    └── Dates.kt                 day/week/month boundaries and grids, via Calendar
```

Conventions set in milestone 1 — keep following them:

- **Money is an integer count of paise** (`Meal.costPaise`, `Food.pricePaise`,
  `BoughtItem.pricePaise`). Never a `Float` or `Double`.
- **`MealPlace` (HOME / OUT) is shared** by `Meal.place` and `Food.source` — it is the same
  distinction, so there is no parallel enum. The Foods UI labels it "Homecooked" / "Outside".
- **Notifications use `AlarmManager`, not WorkManager.** This is time-of-day delivery, which
  WorkManager is explicitly not for. A one-shot alarm is re-armed by the receiver after each fire
  and by `BootReceiver` after a reboot — alarms do not survive reboots, so without that receiver
  the reminder silently stops. `setAndAllowWhileIdle` avoids needing the Android 12+ exact-alarm
  permission; a nightly nudge does not need to land on the second.
- The reminder fires **only when tomorrow has nothing planned at all**. A partly planned day
  stays quiet, deliberately.
- Reminder time is **device-local, not hardcoded Asia/Kolkata** — the phone is in India so local
  is IST, and this still behaves sensibly if the phone travels.
- The permission check before `notify` is **inline, not extracted into a helper**: lint's
  `MissingPermission` check cannot follow the dataflow through a helper and fails the build. The
  `SecurityException` catch handles revocation between check and post.
- The manifest carries `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and two receivers. The boot
  receiver is `exported="false"`, which is correct — only the system sends `BOOT_COMPLETED`.
- **Meal slots are one shared composable: `ui/common/MealSlotCard.kt`.** Today and Plan Meal both
  render it and differ only via `layout`, `actions`, `emptyText` and `supportingLine`; do not fork
  it, or the two screens will drift.
- **`SlotLayout` has two modes, and the screens genuinely need different ones.** `CENTRED` (Today)
  puts the name and cost pill on one centred line above the foods — the user asked for Today's
  slots to be centred. `SIDE_BY_SIDE` (Plan Meal) puts the name and pill in a fixed 136dp left
  column with the foods stacked as bordered blocks down the right; a slot is then only as tall as
  its taller column, which is what got dinner above the fold on a screen that also carries the day
  picker. Do not "unify" these into one layout — the two screens have different vertical budgets.
- The side-by-side row needs `Modifier.height(IntrinsicSize.Min)` for the `VerticalDivider`
  between the two columns to have a height to fill. Remove it and the row goes back to
  wrap-content, and the divider silently collapses to nothing rather than erroring.
- **The slots are full-bleed rectangles with no gap, closed by a divider — not `Card`s.** A Card's
  insets, corners and inter-card gaps cost roughly a fifth of the list height, which is what was
  pushing dinner off the screen. The list container carries the same `surfaceVariant` background as
  the slots so the space below dinner reads as the end of one panel rather than a gap. There are
  deliberately **no emoji** on the slots.
- Today's header carries the day, the Edit Mode switch and **End day** on one line. End day was a
  full-width footer button; between it and the taller cards, dinner did not fit. Do not move it
  back to the footer, and keep the header labels short ("Editing", not "Editing what you ate") —
  the row has about 200dp for them before the switch and button.
- Today's `supportingLine` appears **only when something was actually planned**. "Planned: nothing"
  was a wasted line in all four slots on an unplanned day.
- `MealSlotCard(canRemove = ...)` must be false whenever the rows shown did not come from the
  table the caller's remove writes to. Plan Meal passes `true` (always plan rows); Today passes
  `editMode && dayLogged`.
- **The look is a retro arcade HUD: fixed pink / yellow / blue, square corners, monospace.** All
  three levers live in `ui/theme/` and reach every screen through `ReevzMealzTheme` — palette in
  `Color.kt`, 0dp corners in `ReevzShapes`, `FontFamily.Monospace` in `Typography`.
- **Material You dynamic colour is off, and the `dynamicColor` flag is gone.** It used to be on.
  A wallpaper-derived scheme and a named three-colour palette cannot both be in charge, and the
  palette is now the point, so it wins. Do not reintroduce the flag.
- Pink is `primary` (slot names, buttons, a healthy sin bar), blue is `secondary`, yellow is
  `tertiary`. Use the roles, not the raw `Retro*` values, so light and dark both stay correct.
- **Palette contrast is checked, not eyeballed.** Every text-on-background pair in both schemes
  clears WCAG AA (the weakest is 4.26:1 on 20sp bold, which is a large-text pass), and `outline`
  clears 3:1 against the slot panel because it draws the 2dp chip borders. If you change a colour,
  re-check the pair rather than trusting it to look fine in dark mode.
- Type is monospace at *smaller* sizes than the old system-font scale, because monospace glyphs are
  wider. That is what keeps "BOUGHT ITEMS" fitting beside the sin bar in the header.
- A true pixel font (Press Start 2P and the like) would need a `.ttf` in `res/font`; monospace is
  the no-asset stand-in and carries the feel at every size without risking an unreadable label.
- `ThemeMode` is resolved by `ThemeMode.isDark()`; `MainActivity` reads it from
  `PreferencesViewModel` so Settings changes apply immediately. Both share one instance via the
  activity's ViewModel store.
- **Day-scoped ViewModels hold the day in a `MutableStateFlow`, never captured once in a `val`.**
  Today, Plan Meal, Money Spent and Sin all expose a `refreshDay()`/`refreshNow()`/`refreshToday()`
  called from `LifecycleResumeEffect`. Capturing the day at construction meant leaving the app
  open past midnight showed yesterday and let "End day" settle the wrong date.
- **Never return a fresh `stateIn(...)` from a function called during composition.** Each call
  starts a new sharing coroutine in `viewModelScope` that lingers for the `WhileSubscribed`
  timeout, so a recomposing screen accumulates them. Selection state (`pickerSlot`) lives in the
  ViewModel and drives one flow built once.
- **Today's ✕ only appears when `dayLogged` is true** (`canRemove = editMode && state.dayLogged`).
  Before the day is logged the rows on screen come from `planned_meals`, so their ids would be
  used against `eaten_meals` — and both tables AUTOINCREMENT from 1, so the ids overlap and the
  wrong food gets deleted. Adding and clearing are safe (they key on foodId / day+slot); only
  removal is id-based.
- **Money is grouped Indian-style** by `formatPaise` (₹1,00,000). Hand-rolled, not
  `NumberFormat` — Java's `DecimalFormat` cannot express the pattern, and CLDR data varies by
  platform. `paiseToEditableRupees` stays ungrouped because its output must parse back.
- **Sins are stored as events, never as a counter.** `sin_events` holds one row per off-plan
  meal; the month's remaining count is derived as `allowance - sins this calendar month`. That is
  why requirement 15 needs no code: a new month starts fresh on its own and leftover sins are
  discarded automatically. Do not "optimise" this into a stored counter — it would need a reset
  job that can fail to run.
- **`ended_days` makes a day's judgement final.** `SinDao.endDay` no-ops if the day is already
  ended, so a second press cannot double-deduct. Pressing End day again shows a read-only summary
  instead.
- **The 3-day allowance lock keys off `SinSettings.setAt`, which is null until the user first
  chooses a number.** The built-in 40 is a starting value, not a decision, so the first change is
  free. `SinViewModel.setAllowance` re-checks the lock before writing, so a stale screen cannot
  bypass it.
- `SinStatus.remaining` is clamped at zero; `failed` is `allowance - used <= 0`, so overshooting
  the allowance still reads as simply failed rather than negative.
- Material 3 has no success colour role, so the green for "Good Job!" is the one hand-picked pair
  in `ui/theme/Color.kt` (`goodColor()`). It picks by the **scheme's own background luminance**,
  not `isSystemInDarkTheme()` — the latter followed the OS and so went wrong whenever Settings
  forced Light or Dark against it. Red is `colorScheme.error`.
- **Sins are a health bar in the header** (`ui/sin/SinHealthBar.kt`): a bordered strip of 10 cells
  that empties as the month is spent — pink, then yellow under half, then red under a fifth, and
  GAME OVER at zero. The cell count is fixed at 10, not one per sin: the allowance is configurable,
  so a cell-per-sin bar would change width whenever it changed and be unreadably fine at 40. Any
  part-used cell still counts as lit, so an empty bar means exactly zero.
- **Each Settings group is a bordered `SettingsSection` panel, not flat text under a divider.**
  Monospace wraps every explanation onto two or three lines, and those lines then carry as much
  visual weight as the controls, so the screen read as one wall. Add a new setting as another
  panel, and keep its hint to one line where the control can speak for itself.
- **Buttons whose label can wrap use `heightIn(min = 48.dp)`, never `height(48.dp)`.** A fixed
  height clipped the second line of "Delete records older than 12 months" clean off on a real
  phone. The label is now short enough to fit on one line either way.
- The Theme hint no longer claims colours come from the wallpaper. It said so while Material You
  was on; that stopped being true with the retro palette, and the text outlived the behaviour.
- **`SinViewModel` is shared, not per-screen.** `viewModel()` resolves to the activity's store, so
  the header health bar, Today and Settings all read one instance and cannot disagree.
- **Money Spent has two streams**: `bought_items`, and outside-food cost derived with the same
  plan-versus-actual rule Today uses (a day's own eaten record if it has one, else its plan). That
  rule is duplicated once, in `SpendDao.observeOutsideFoodsIn`, because it has to run as SQL — keep
  it in step with `effectiveSlots` if either changes.
- **Money Spent's two streams are red and blue, never red and pink.** Outside food is `error`,
  bought items are `secondary`. Pink (`primary`) sat **25 degrees of hue** from the red with
  identical lightness, so as a 3dp stripe the two were indistinguishable — the user could not tell
  which stream a row belonged to. Blue is 156 degrees away, and it already means "home" on the meal
  slots, which is what bought items are: groceries cooked at home. The same pair colours the
  `SplitBar`, the two `StreamTotal` rows and each item's stripe, so all three agree. Note that
  WCAG luminance ratio is the *wrong* metric for telling two accents apart — red against blue
  scores 1.31:1 while being obvious; check hue separation instead, and keep it past 60 degrees.
- **The breakdown is grouped by date, not dated per row** (`ui/money/SpendGrouping.kt`). A date
  column repeated the same "3 Sept" down eight rows; a heading says it once and frees that width
  for the food's name, which had been wrapping. The heading is sticky and shaped like Bought
  Items' month heading — both are "a period and its total", so they should not look like two
  different ideas — and it carries the day's total, summed from the very lines beneath it for the
  same reason there is no `SUM()` query.
- **The two streams are merged into one dated list** (`spendLines` -> `groupByDay`, both pure and
  unit-tested in `SpendGroupingTest`). `SpendEntry.dayStart` is already a local midnight but
  `BoughtItem.boughtAt` is a real timestamp, so it **must** be normalised or a purchase forms its
  own section per instant instead of joining that day's food. Within a day, outside food (red)
  is listed before bought items (blue), and each stream keeps its query's order — `groupBy`
  preserves encounter order, which is what makes that hold.
- **The Day window shows no date headings.** With one day in the window the heading would repeat
  what the navigator and the headline total already say, three times over. `showDayHeadings` is
  simply `period != DAY`; the rest of the rendering is identical, so there is no second code path.
- **The breakdown is ruled with hairlines between rows** (`SpendDivider`), the same
  1dp `outlineVariant` as the Foods list and the picker — but only *within* a day now that the
  list is grouped: the heading separates one day from the next, and a rule under a group's last
  row would box it in. Grouping removed the old cross-stream seam special case entirely.
- **The DAO returns items, not sums, and the totals are added up in Kotlin.** There is deliberately
  no `SUM()` query: a separate sum could drift from the itemised list shown underneath it, and then
  the headline and the breakdown would disagree with no way to tell which was right.
- **Money Spent opens on Day and every period switch jumps to the current one.** It used to open on
  Month and keep whatever anchor you had browsed to, so on the 1st of a month it read ₹0 while the
  money spent yesterday sat one tap away, and browsing back to March then tapping Day showed
  1 March. `selectPeriod` re-anchors to `now`, and `refreshNow` carries a view that was on the
  current period into the new day at midnight.
- Money queries filter on `f.source = :outside` with `MealPlace.OUT` passed as a **parameter**,
  not a hardcoded `'OUT'` string, so Room's own enum converter does the mapping and renaming the
  constant cannot silently break the totals.
- **A period never counts days that have not happened** (`countedRange`), so the current month
  reads as money already spent rather than a projection.
- **The cost rule lives in one place: `ui/common/PlanSlots.kt`.** Only food bought outside counts
  towards a slot or day total. Homecooked food contributes nothing — its real cost varies and is
  tracked through Bought Items instead. `PlanSlot.costPaise` filters on `MealPlace.OUT` rather
  than trusting `pricePaise` to be null, so a stray price cannot leak into a total. Do not
  re-derive this logic in a screen.
- **Food blocks are coloured by where the food came from**: `secondaryContainer` (blue) for
  homecooked, `errorContainer` (red) for bought outside, border to match. Source is the thing worth
  seeing at a glance, and it is what decides whether the meal costs anything. The block is
  left-aligned with the price on the right — centring the name with a floating price chip wasted the
  width and made edit mode scroll.
- **Adding a food is a green `+` block, not a text button** (`AddFoodAction`). It is the same
  width as the food blocks and carries only a `+`, so it reads as the next empty place in the
  stack. It is a `RowScope` extension taking `weight(1f)`, which is why the `actions` slot is
  typed `RowScope.() -> Unit` — that is what lets it fill the row on both screens. Its greens
  live in `addBlockColors()`, picked by scheme luminance the same way `goodColor()` is.
- **Neither screen has a slot-clearing action any more.** Today's edit mode lost "Ate nothing"
  and Plan Meal lost `Clear`: every row already carries its own ✕, so both were a second route
  to the same result taking a third of the action row. `+` is now the only slot action, and it
  fills the row. `ClearSlotAction` is gone with them — do not reintroduce a slot-wide clear
  without asking, and note that `PlannedMealDao.clearSlot` and `EatenMealDao.clearSlot` are now
  uncalled (kept as queries, but nothing in the UI reaches them).
- **The Foods list is ruled with hairlines between rows** — 1dp `outlineVariant`, drawn only
  between rows, never above the first or below the last, so the list reads as ruled rather than
  boxed in. Same rule and colour as the food picker, because they are the same list of foods
  seen from two places. This is why the list uses the index-based `items(count = ...)` overload
  rather than `items(items = ...)`: the divider needs to know it is not the first row.
- **The bottom bar is hand-built (`ui/RetroNavBar.kt`), not Material's `NavigationBar`.**
  Material sources its stadium-shaped active indicator from component tokens rather than
  `MaterialTheme.shapes`, so the app's 0dp corners could never reach it and a rounded pill sat
  in the middle of a square HUD. The selected tab is a filled square cell instead, with
  hairline dividers between cells and the same 2dp seam the meal slots use.
- `RetroNavBar` applies `navigationBarsPadding()` itself, which is the job `NavigationBar` used
  to do — drop it and the bar sits under the system gesture bar.
- **A plan locks at its own midnight** (`ui/plan/PlanLock.kt`). A plan is an *intention*, so it
  can only be set while the day is ahead; once the day begins, what happens belongs to Today's
  Edit Mode. Without this the same day could be rewritten from two screens with two different
  meanings, and — worse — yesterday's plan could be edited after the fact to match what was
  actually eaten, which would quietly destroy the plan-versus-actual comparison Today and Money
  Spent both rest on.
- `planLock(dayStart, today)` is a pure function over two local midnights, so the cutoff is 12 am
  and not "24 hours from now"; it is unit-tested (`PlanLockTest`), including the midnight flip.
  Its three states exist because the UI needs different copy: OPEN, DAY_UNDER_WAY ("The day has
  begun. Use Edit mode in Today.") and DAY_PASSED ("This day has passed."). The notice is
  `tertiary` yellow — 12.3:1 dark, 4.8:1 light — and names the way out rather than only refusing.
- **The lock is enforced at the write, not just in the UI.** `assignFood`, `removeSlotFood` and
  `openPicker` all pass through `selectedDayIsOpen()`, which calls `refreshToday()` first so the
  check reads the clock rather than cached state — a screen left open across midnight would
  otherwise still believe tomorrow is tomorrow. Same pattern as `SinViewModel.setAllowance`
  re-checking the 3-day lock. `refreshToday` also closes a picker left open over midnight, so no
  sheet is left where taps would silently do nothing.
- A locked day shows **no `+` blocks and no ✕** (`actions = null`, `canRemove = lock.isOpen`),
  which makes the locked screen considerably shorter than the editable one — that is why the
  two-line locked heading costs nothing.
- **`MealType` declaration order is the UI order**: breakfast, lunch, snack, dinner. Room stores
  enums by name, so reordering is safe for existing rows — but never rely on `ordinal`.
- **The ad-hoc meal log is superseded.** `Meal` / `MealDao` are still declared on the database so
  the `meals` table and its rows are preserved, but nothing in the UI reads them. Do not "tidy
  up" by removing the entity: that would drop the table and destroy data.
- A plan row is `(dayStart, type, foodId)` with `dayStart` at local midnight, a unique index so
  the same food cannot land twice in one slot, and `onDelete = CASCADE` so deleting a food
  removes it from every plan rather than orphaning rows. `eaten_meals` has the same shape.
- **Plan and actuals are separate tables.** `planned_meals` is the intention and is never
  rewritten by Today; `eaten_meals` is what happened. `SlotFood` is the shared join POJO for
  both, which is why it is not called `PlannedFood`.
- **`eaten_days` is load-bearing, not redundant.** Without it, a day with no eaten rows would be
  ambiguous between "not edited yet, so assume the plan" and "edited, and I skipped everything".
  Its presence means the eaten rows are authoritative *even when empty*, which is how a skipped
  meal avoids inflating the day's cost. `effectiveSlots` in `PlanSlots.kt` encodes that choice —
  do not re-derive it in a screen.
- Switching Edit Mode on calls `startLoggingDay`, which copies that day's plan into the eaten
  table once. It is idempotent, so every edit action can safely call it first.
- **A homecooked food has `pricePaise = null`**, not zero, and `place = null`. `FoodsViewModel.save`
  nulls both out on save so a price or shop typed before flipping the toggle cannot leak through.
- **Food name and place are capitalised in two places, on purpose.** The keyboard gets
  `KeyboardCapitalization.Words` so it happens as you type, and `FoodsViewModel.save` runs
  `capitalizeWords` again on the way to the database. The keyboard setting is only a *hint* to
  the IME — pasted text, swipe input and `adb shell input text` all slip through it — so the
  save-time pass is what actually guarantees the stored value.
- `util/TextCase.kt` only uppercases the **first letter** of each word and leaves the rest as
  typed, so "KFC" and "McDonald's" survive; real title-casing would flatten them. Leading
  punctuation does not consume the word start ("(tea break)" -> "(Tea Break)", which matters
  because shops get written in brackets), but a leading digit does ("2nd" stays "2nd").
- **The Place field autocompletes from places already used** (`util/PlaceSuggestions.kt`, shown
  as a scrolling row of chips under the field). Suggestions are derived from the foods already in
  memory, not a shops table — so a place cannot go stale, and deleting the last food from a shop
  stops it being suggested. Ordered by how many foods use the place, then alphabetically, because
  the whole point is that the usual suspects come first.
- **The matching lives in pure functions so it can be unit-tested** (`PlaceSuggestionsTest`,
  14 cases). An empty box offers everything; prefix matches beat mid-word ones; a place identical
  to what is typed is dropped so no chip does nothing when tapped. Places differing only in case
  fold together, which matters for rows typed before `capitalizeWords` existed.
- **The Place field is a `TextFieldValue`, not a `String`, purely so a tapped suggestion can put
  the caret at the end.** The `String` overload of `OutlinedTextField` keeps its previous selection
  and merely clamps it to the new length, so after typing "t" and tapping "Thilak" the caret stayed
  at offset 1 and the next keystrokes landed *inside* the word ("cube" + "hilak"). Do not simplify
  it back to a `String`; that reintroduces the bug. It needs
  `rememberSaveable(key, stateSaver = TextFieldValue.Saver)`, since a `TextFieldValue` is not
  Bundle-storable on its own.
- The suggestion row is a one-line `LazyRow`, not a wrapping `FlowRow`: wrapping would grow taller
  as places accumulate and shove the Create button down the sheet, and its height would change as
  you type. Its items deliberately carry **no `key`** — the list reorders on every keystroke, has
  no state worth preserving across those reorders, and a duplicate key is a crash.
- Editing a food whose place is already a known place shows **no suggestions**, since the full
  place name matches nothing else; clearing the box brings them all back. Say so if it should
  offer the *other* places instead when the field already holds a complete one.
- **`Food.place` is free text and optional even for outside food**, stored as null when blank so
  "no place recorded" has one representation. It is deliberately not a table of shops to pick
  from: it is a memory jog, and nothing reconciles across foods.
- **The picker asks where before what** (`FoodPickerSheet`): **Ate out / Home food**, then a grid
  of restaurants, then that restaurant's foods. A meal eaten out almost always comes from one
  place, so choosing the shop first turns a flat list of every food into two taps and a short
  list. Home food skips the grid, since homecooked food has no place by definition. Both Today's
  Edit Mode and Plan Meal use it — the reasoning applies to planning ahead too, and it leaves one
  code path.
- The two source blocks are **red for outside and blue for home**, the pair the meal slots already
  use for a food's source, so the choice made in the picker is the colour the row will end up.
  Place tiles are the same blue as the place chips; the create actions are the `+` block's green.
- **Nothing in the picker is a dead end.** A restaurant not in the grid, or a dish not in that
  restaurant's list, can be created from the picker — place, name, price — and that writes a real
  row to `foods`, so it appears in the Foods section and every later picker. This is why the
  "No foods yet, create them in the Foods section first" message and `anyFoodsExist` are both
  gone: there is no longer a state the picker cannot get you out of.
- **`data/Food.kt`'s `foodOf(...)` is the one way to build a food from form input**, and all three
  entry points go through it — the Foods editor, and the picker on Today and on Plan Meal. It
  capitalises name and place and nulls out price/place for homecooked food. Three copies of those
  rules is three chances to drift, which is exactly what happened to the capitalisation rule once
  already.
- The grid is built from `Rows` inside a scrolling `Column`, like the month picker — a handful of
  shops does not need a lazy grid, and the last row is padded with `Spacer(weight)` so one tile
  keeps its column width instead of stretching across.
- The picker's step is tracked as an **enum plus a nullable place**, both `rememberSaveable`, so a
  trip to another app does not drop you back at step one. Note `newFoodIsOutside` is separate
  state: "+ New place" and "+ New home food" both arrive at the new-food step with no place
  chosen, so which kind of food is being created cannot be inferred from the place.
- **Back inside the picker is a visible `‹`, not the system gesture.** System back on a
  `ModalBottomSheet` dismisses the whole sheet, which would throw away two taps of progress.
- **There is one filter text box in the app: `ui/common/FilterField.kt`.** It is used by the Foods
  search; the picker's own two filter boxes went away when the flow replaced them — choosing the
  restaurant *is* the place filter now. It holds no state; the caller owns the text, because the
  caller is what does the filtering.
- **Foods filters in the composable, not the ViewModel**, the same as the picker: the list is
  already in memory, so a filtered flow would add a second source of truth for no gain. The query
  and the source filter are `rememberSaveable`, so a trip to another app does not silently widen
  the list back out while the user is still reading a filtered view.
- **Foods' source filter is `All / Home / Outside` as a `SingleChoiceSegmentedButtonRow`** — the
  same control as Plan Meal's Week/Month and Money Spent's period switch, so a filter looks like a
  filter everywhere. `SourceFilter.place` is null for `All`, which makes the filter one null check
  rather than a three-way branch. The search box sits *above* the row rather than beside it: 394dp
  of phone will not give three legible segment labels and room to type on one line, and Foods
  scrolls anyway.
- **Foods has two distinct empty states.** "No foods yet" (nothing exists) and "Nothing matches"
  (the filters hid everything) are different problems with different fixes, and one message for
  both would send the user to Add food when the real answer is to clear the filter.
- **Homecooked food gets a green "Home" tag; outside food gets its blue place chip.** The user
  asked for green, and it reuses `addBlockColors()` rather than inventing a fourth green. It is
  the one chip in the app with a border, and it needs one: the pale green container is only
  1.12:1 against the light-theme background, so without an edge the tag would have no shape
  there. The border is 4.75:1, past the 3:1 a UI boundary needs. Note the meal *slots* still
  colour homecooked blue — there the block is the food itself, and blue-versus-red is what shows
  at a glance whether a meal costs anything.
- An outside food with no place recorded still shows no tag, since `Food.place` is optional even
  for outside food. Rare enough to leave alone; say so if it should read "Outside" instead.
- **The picker filters on name and place, in two boxes side by side** (`FilterField`, used
  twice). They combine with AND, so "dosa" plus "tea" reaches dosas from Tea Break. A non-empty
  place drops homecooked food, which has no place by definition — that is the intent, the filter
  exists to choose between shops. The labels are one word ("Food", "Place") because monospace is
  wide and two boxes have only about 165dp each; "Search foods" did not fit. The clear button
  shows only when a box has text, so an empty box keeps its full width.
- **The place is shown wherever two foods could be confused for each other**: the Foods list,
  the food picker and the Money Spent breakdown. Same blue chip in all three, capped at 170dp
  and ellipsised so a long shop name cannot squeeze out the food's own name. Two foods can
  share a name and differ only by shop, which is exactly when the picker needs it. The meal
  slots still leave it out — there the food is already chosen.
- Destructive actions confirm first: deleting a food goes through an `AlertDialog`.
- **`java.util.Calendar`, not `java.time`.** `java.time` needs API 26 and `minSdk` is 24, so using
  it would require core library desugaring or raising `minSdk`. Change that deliberately, not
  incidentally.
- **No Repository layer.** With a single local data source it would be a pure pass-through, so the
  ViewModel uses the DAO directly.
- **No DI library.** `MealDatabase.getInstance()` plus a `viewModelFactory` covers it.
- User-visible strings live inline in the composables, not `strings.xml` — single-language personal
  app. Revisit only if localization is actually wanted.
- Meal and purchase timestamps are always "now". There is deliberately no date/time picker yet.
  Editing an existing record preserves its original timestamp, so a typo fix cannot move an item
  into a different month.
- **Month grouping keys on `year * 100 + month`** (`monthKeyOf`), never on month alone — the same
  month in different years must not merge. Grouping lives in `ui/bought/MonthGrouping.kt` as a
  pure function so it is unit-testable without a device.
- `BoughtItem` is deliberately **not** linked to `Food`. A purchase is an event; a Food is a
  reusable planning block. Requiring every purchase to exist as a Food first would add friction
  for no gain. Revisit only if the two genuinely need to reconcile.
- **The launcher icon is generated, not hand-drawn: `tools/make_icon.py`.** The art is a 16x16
  ASCII grid at the top of that script — a pixel chicken in the app's own pink/yellow/indigo.
  One run rewrites both adaptive-icon vector layers, the monochrome layer and all ten legacy
  PNGs, so edit the grid and re-run rather than touching any generated file.
- The adaptive foreground is sized to the 72dp safe **circle**, not the safe square. A circular
  mask inscribes a circle in that square, so art sized to the square loses whatever sits at top
  and bottom centre — here, the comb and the feet. `safe_scale()` fits the art's furthest
  corner to the radius, which is why the bird looks smaller than the safe square would allow.
- The `<monochrome>` layer is its own drawable, not the foreground reused. Themed icons are
  tinted one flat colour, so reusing the foreground would flatten the bird into a blob; the
  monochrome layer leaves the eyes as holes instead.
- Legacy mipmaps are **PNG, and the template `.webp` files were deleted**. Two files with the
  same resource name in one folder is a build error, so they cannot coexist.
- No icon-pack dependency. `material-icons-core` is frozen at 1.7.8 while Compose is on 1.10.4
  and the artifact is deprecated, so navigation icons are hand-authored vector drawables in
  `res/drawable/ic_*.xml`. Do not put `android:tint="?attr/colorControlNormal"` in them — that is
  an AppCompat attribute and this project has no AppCompat. Compose's `Icon` tints the painter.
- **One Scaffold only**, owned by `ReevzMealzApp`. Section screens are plain content composables
  that take a `Modifier`; Today draws its FAB as a `Box` overlay rather than nesting a Scaffold.
  This keeps window insets applied exactly once.
- **No navigation library.** Section switching is `rememberSaveable` state plus a `when`, with a
  `BackHandler` returning to Today. Add `androidx.navigation.compose` when sections actually gain
  sub-screens, not before.
- **The bottom bar carries four tabs, not six.** Six left about 68dp each on a phone, which was
  cramped; four get about 103dp. `AppSection.inBottomBar` is the split, and `AppSection.tabs` /
  `pauseMenuSections` are the two lists — adding a section means deciding which side it belongs on.
- **Money Spent and Settings live in the pause menu** (`ui/PauseMenu.kt`), opened by the ☰ in the
  header. They are still ordinary full sections; only the way in changed. A pause menu rather than
  an overflow dropdown because the app is themed as a game and the top-right corner already
  belongs to the sin bar — which is also why the ☰ is *leading*, not trailing.
- `paused` is separate state from `section`, so pausing does not lose where you were. Back closes
  the menu first and only then falls back to returning to Today; while paused no tab reads as
  selected, and the header title says PAUSED rather than naming a screen that is not on show.

## Toolchain notes (non-obvious — read before touching Gradle)

This project is on a very new toolchain, and several things differ from older Android setups:

- **AGP 9.3.2**, Gradle 9.5.0, Kotlin 2.2.10, Compose BOM 2026.02.01.
- **There is no `org.jetbrains.kotlin.android` plugin.** AGP 9 compiles Kotlin itself. Only
  `com.android.application` and `org.jetbrains.kotlin.plugin.compose` are applied. Don't "fix" this
  by adding the Kotlin Android plugin.
- `compileSdk` uses the AGP 9 block form: `compileSdk { version = release(37) }`. `targetSdk 37`,
  `minSdk 24`.
- Release build type uses `optimization { enable = false }` — the AGP 9 replacement for
  `isMinifyEnabled` / `proguardFiles`. R8 is currently off; that needs flipping before any real
  release.
- R8 keep rules live in `app/src/main/keepRules/rules.keep`, not `proguard-rules.pro`.
- Gradle **configuration cache is enabled**. Build logic that isn't configuration-cache-safe will
  fail the build.
- All dependencies and versions go through the version catalog at `gradle/libs.versions.toml` —
  never hardcode a version in `app/build.gradle.kts`.
- **`android.disallowKotlinSourceSets=false` in `gradle.properties` is load-bearing. Do not remove
  it.** KSP (Room's code generator) registers its generated source directories through
  `kotlin.sourceSets`, which built-in Kotlin rejects by default (AGP issue #386221070). Without the
  flag the build fails at configuration time. It only relaxes that ownership check — it does not
  disable built-in Kotlin. Drop it once AGP and KSP stop conflicting.
- KSP must track the Kotlin version: Kotlin 2.2.10 pairs with KSP `2.2.10-2.0.2`.
- `compileOptions` targets **Java 17** while the Gradle daemon toolchain is Java 25. That is
  intentional and fine — the daemon JVM is not the bytecode target. Kotlin follows `compileOptions`
  on its own, so there is no `jvmTarget` / `jvmToolchain` block and none is needed.
- Remaining rough edge: the catalog declares `core-ktx 1.10.1` and `lifecycle-runtime-ktx 2.6.1`,
  but they actually resolve to `1.18.0` and `2.9.4` because transitive constraints pull them up.
  The build is correct; the catalog just is not telling the truth. Worth reconciling so the
  effective versions stop depending on a transitive chain.
