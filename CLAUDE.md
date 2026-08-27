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
- Do not delete or reset user data as part of a normal feature implementation.
- Treat user-entered meal and spending data as valuable and non-recoverable.

## Current project state

Single Gradle module `:app`, package `com.reevan.reevzmealz`. Room persists everything locally,
fully offline. Target device is a **Nothing Phone (2a)**; `minSdk 24` / `targetSdk 37` covers it.

**Today** shows the current day's *plan*: all four meal slots (breakfast, lunch, snack, dinner),
what is planned in each, and the day's total pinned at the bottom. It is read-only — planning
happens in Plan Meal.

**Plan Meal** picks a day (week strip or month grid, toggleable) and fills its four slots with
foods from the Foods section. Several foods per slot. Full CRUD over assignments.

The app shell has six top-level sections in a bottom navigation bar: **Today, Plan Meal, Bought
Items, Foods, Money Spent, Settings**. Today, Plan Meal, Bought Items and Foods are built; Money
Spent and Settings are placeholders awaiting their specs.

**Foods** holds atomic food items — name, a Homecooked/Outside toggle, and a price that only
applies when Outside. They are the building blocks meal planning will draw on. Full CRUD.

**Bought Items** records grocery/ingredient purchases — name, price, date — as a month-wise
history in the style of a payments app's transaction list: a sticky month heading carrying that
month's total, then one card per purchase, newest first. Full CRUD.

```
app/src/main/java/com/reevan/reevzmealz/
├── MainActivity.kt              single ComponentActivity, hosts ReevzMealzApp
├── data/
│   ├── Meal.kt                  MealType / MealPlace enums + superseded @Entity (see below)
│   ├── MealDao.kt               unused; kept so the meals table survives
│   ├── PlannedMeal.kt           @Entity (day, slot, foodId) + PlannedFood join POJO
│   ├── PlannedMealDao.kt        observeDay(Flow), observePlannedDays, insert, delete, clearSlot
│   ├── Food.kt                  @Entity, atomic food item, nullable pricePaise
│   ├── FoodDao.kt               observeAll(Flow), insert, update, delete
│   ├── BoughtItem.kt            @Entity, a purchase: name, pricePaise, boughtAt
│   ├── BoughtItemDao.kt         observeAll(Flow), insert, update, delete
│   └── MealDatabase.kt          @Database v3, singleton, exportSchema, autoMigrations
├── ui/
│   ├── AppSection.kt            the six sections: title, tab label, icon
│   ├── ReevzMealzApp.kt         shell: the one Scaffold + bottom NavigationBar
│   ├── common/
│   │   ├── PlanSlots.kt          PlanSlot + buildSlots/totalCostPaise (the cost rules)
│   │   └── SectionPlaceholder.kt  stand-in body for unbuilt sections
│   ├── theme/                   Theme.kt, Color.kt, Type.kt
│   ├── today/                   built: TodayScreen, TodayViewModel
│   ├── foods/                    built: FoodsScreen, FoodsViewModel, FoodEditorSheet
│   ├── bought/                   built: BoughtItemsScreen/ViewModel, editor, MonthGrouping
│   ├── plan/                     built: PlanMealScreen/ViewModel, DayPicker, FoodPickerSheet
│   ├── money/                   placeholder
│   └── settings/                placeholder
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
  removes it from every plan rather than orphaning rows.
- **A homecooked food has `pricePaise = null`**, not zero. `FoodsViewModel.save` nulls it out on
  save so a price typed before flipping the toggle cannot leak through.
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
