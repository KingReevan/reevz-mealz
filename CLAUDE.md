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
- Support **light and dark themes** — the existing `ReevzMealzTheme` already handles dynamic color
  and light/dark, so route new UI through it.
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
freely editable (add foods from Foods, remove them, mark a slot "Ate nothing", or reset the day
back to the plan). Editing never touches the plan.

**Plan Meal** picks a day (week strip or month grid, toggleable) and fills its four slots with
foods from the Foods section. Several foods per slot. Full CRUD over assignments.

The app shell has six top-level sections in a bottom navigation bar: **Today, Plan Meal, Bought
Items, Foods, Money Spent, Settings**. All six are built.

**Foods** holds atomic food items — name, a Homecooked/Outside toggle, and a price plus a **place**
(where it was bought) that only apply when Outside. The place shows as a chip on the right of each
row. They are the building blocks meal planning will draw on. Full CRUD.

**Bought Items** records grocery/ingredient purchases — name, price, date — as a month-wise
history in the style of a payments app's transaction list: a sticky month heading carrying that
month's total, then one card per purchase, newest first. Full CRUD.

**Money Spent** totals a chosen day, week, month or year (toggle, Google-Calendar style, with
‹ › navigation), split into Bought Items and Outside food. Defaults to the current month.
Browsing stops at the 12-month retention window, and the current period counts only up to today.

**Settings** is the single home for configuration, in sections: **Theme** (System/Light/Dark),
**Notifications** (nightly reminder on/off and time), **Sins** (monthly allowance), **Storage**
(the retention purge). New settings belong here as another section.

**Notifications**: one nightly reminder, default 19:00 device-local, which fires only when
tomorrow has *nothing* planned at all.

**Sins** are the discipline mechanic. One sin = one meal that did not go to plan. The allowance
(default 40) is per calendar month and configurable, but **locked for 3 days after being set** so
it cannot be raised the moment it pinches. Remaining sins show in the top-right of every screen.
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
│   ├── PlannedMealDao.kt        observeDay(Flow), observePlannedDays, insert, delete, clearSlot
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
│   ├── AppSection.kt            the six sections: title, tab label, icon
│   ├── ReevzMealzApp.kt         shell: the one Scaffold + bottom NavigationBar
│   ├── common/
│   │   ├── PlanSlots.kt          PlanSlot, buildSlots/totalCostPaise, effectiveSlots
│   │   ├── FoodPickerSheet.kt    shared food picker (Today + Plan Meal)
│   │   └── SectionPlaceholder.kt  stand-in body for unbuilt sections
│   ├── theme/                   Theme.kt, Color.kt, Type.kt, Shape.kt
│   ├── today/                   built: TodayScreen, TodayViewModel
│   ├── foods/                    built: FoodsScreen, FoodsViewModel, FoodEditorSheet
│   ├── bought/                   built: BoughtItemsScreen/ViewModel, editor, MonthGrouping
│   ├── plan/                     built: PlanMealScreen/ViewModel, DayPicker
│   ├── sin/                      SinStatus (rules), SinViewModel, EndDayDialog
│   ├── money/                    built: MoneySpentScreen/ViewModel, SpendPeriod
│   └── settings/                 built: SettingsScreen/ViewModel (retention purge)
└── util/
    ├── Money.kt                 paise <-> "₹420.50", editable-field rendering
    ├── FoodFormat.kt            food source labels and row subtitles
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
- **Meal slots are one shared composable: `ui/common/MealSlotCard.kt`.** Centre-aligned, with the
  slot name at 20sp bold and its cost pill on one line, then the foods with a chip each — red chip
  for a price, plain for homecooked. Today and Plan Meal both render it and differ only via
  `actions`, `emptyText` and `supportingLine`; do not fork it, or the two screens will drift.
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
- **The kid-friendly look comes from shape and type, not colour.** Material You dynamic colour is
  deliberately kept, so the palette follows the wallpaper; `ReevzShapes` (rounder than Material's
  defaults) and the enlarged `Typography` are what carry the friendly feel, and they reach every
  screen through `ReevzMealzTheme`. A genuinely rounded font would need a file in `res/font`.
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
  in `ui/theme/Color.kt` (`goodColor()`), chosen by `isSystemInDarkTheme()`. Red is
  `colorScheme.error`.
- **`SinViewModel` is shared, not per-screen.** `viewModel()` resolves to the activity's store, so
  the shell badge, Today and Settings all read one instance and cannot disagree.
- **Money Spent has two streams**: `bought_items` totals, and outside-food cost derived with the
  same plan-versus-actual rule Today uses (a day's own eaten record if it has one, else its
  plan). That rule is duplicated once, in `SpendDao.observeOutsideFoodTotal`, because it has to
  run as SQL — keep it in step with `effectiveSlots` if either changes.
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
- **`Food.place` is free text and optional even for outside food**, stored as null when blank so
  "no place recorded" has one representation. It is deliberately not a table of shops to pick from:
  it is a memory jog, and nothing reconciles across foods. It is shown only in the Foods list —
  meal slots, the food picker and Money Spent do not carry it.
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
