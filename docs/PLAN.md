# Godfather Reevz — Implementation Plan

> The plan the owner approved on 2026-10-29, kept here so it travels with the code. The product
> brief and decisions are in [`SPEC.md`](SPEC.md). Keep the phase status table up to date.

## Context

Reevan wants to call one friend every day so friendships don't fade. **Godfather Reevz** is a
personal Android app for their Nothing Phone (2a). The phone is `A142` "Pacman", adb serial
`00050146M001006`, on **Android 16 / API 36**, with a 1084×2412 screen at 420 dpi. The app:

- picks today's friend from a shuffled rotation,
- shows everyone as a stack of cards with their last-called date (`dd-mm-yyyy` or `-`),
- counts a call only when the in-app **Call** button is pressed,
- supports adding, editing and removing friends, with Statistics and Settings (themes, reshuffle).

`C:\Users\reeva\AndroidStudioProjects\GodfatherReevz` currently holds an untouched Android Studio
Compose template:
- package `com.reevan.godfatherreevz`
- AGP 9.3.3, Kotlin 2.2.10, Compose BOM 2026.02.01
- `compileSdk { release(37) }`, `minSdk 24`, Java 11
- not a git repository

The app copies the stack and conventions of their previous apps, mainly **ReevzHabitz**, plus
ReevzDrip and ReevzMealz.

### Decisions made with the owner
| # | Question | Answer |
|---|---|---|
| D1 | Missed day | **Today's friend stays until called.** Nobody is skipped; the schedule just slides. |
| D2 | New friend | **Before any repeat.** A new friend is shuffled in among friends never called. If everyone still waiting has been called before, the new friend goes to the front (after today's locked friend). |
| D3 | Tapping a non-today card | **Opens their details, with Call.** Calling them records the date and uses their turn this round. Today's friend stays today's. |
| D4 | Extras | Only **per-friend notes**: one free-text box on each friend's details page for conversation notes. No reminder, widget, backup or skip. |
| D5 | Home layout | **Focus stack (timeline).** Past calls above (faded), today's card enlarged and centred, upcoming friends below. |
| D6 | Menu | **Full-screen Menu page** with four big stacked buttons, as in Habitz. |
| D7 | Themes | **Fixed themes.** Each is one complete look, light or dark, picked in Settings. There is no follow-system mode. |

### My calls (research and defaults; flag any you disagree with)
- **Adding friends: in-app contact importer plus manual entry.** Manual entry is always on the Add Friend page.
  - **Why not the system picker alone:** the classic `ACTION_PICK` on `Phone.CONTENT_TYPE` needs no permission but picks one contact at a time. Each pick then needs an extra confirm step, which is painful with a lot of friends.
  - **The Android 17 picker:** it supports multi-select without a permission (`ContactsPickerSessionContract.ACTION_PICK_CONTACTS`), but needs API 37. This phone is on API 36.
  - **So the primary path is an in-app importer.** It asks for `READ_CONTACTS` once, then offers a searchable A–Z list with multi-select. You choose a number when a contact has several, and friends already added are marked "Added".
  - **Fallback when the permission is denied:** use the system picker. On API 37+ that is `ACTION_PICK_CONTACTS` with `EXTRA_ALLOW_MULTIPLE`. On API 36 or lower it is a single `ACTION_PICK` that fills in the manual form.
  - **Testing limit:** the API 37 branch can't be checked on this phone. It is small and isolated, and gets checked on an API 37 emulator if one is available.
  - **Sources:** [Android 17 contact picker](https://developer.android.com/about/versions/17/features/contact-picker) and the [ContactsPickerSessionContract reference](https://developer.android.com/reference/android/provider/ContactsPickerSessionContract).
- **Calling: places the call directly.** Pressing Call uses `ACTION_CALL` with the `CALL_PHONE` permission. A Settings switch, **"Call directly"** (on by default), can change this to open the dialer with the number filled in (`ACTION_DIAL`, no permission). If the permission is denied, the app opens the dialer instead. Dual-SIM choice is left to the phone's own dialer.
- **Undo a call (edge case: an accidental press).** On the details page, "Called today ✓ · Undo", with a confirm step, reverses today's recorded call for that friend.
- **Removing a friend keeps their past calls in Statistics totals.** Calls store a name snapshot, and the friend link is set to null.
- **No cloud backup:** `allowBackup="false"`, the same as Habitz, so names, numbers and notes never leave the phone. If you want Google auto-backup instead, say so.
- **Month logic needs nothing special.** Rotation is a continuous queue. With 40 friends, the 9 not reached in October simply continue in November, already in random order.
- **Git:** run `git init`, with a `.gitignore` like Habitz's, and make one local commit at the end of each phase. Nothing is ever pushed.

---

## Stack and project setup (copied from ReevzHabitz)

- **Version catalog:** take `ReevzHabitz\gradle\libs.versions.toml` as is.
  - Adds `ksp = "2.2.10-2.0.2"`, `room = "2.8.4"`, `lifecycle-viewmodel-compose` and `lifecycle-runtime-compose`.
  - **No other libraries.** No Hilt, Navigation, DataStore or serialization.
- **`app/build.gradle.kts`:**
  - Plugins: `android.application`, `kotlin.compose`, `ksp`. AGP 9 compiles Kotlin itself, so there is **no** kotlin-android plugin.
  - Raise `minSdk` from 24 to **26**, so `java.time` works without desugaring (Habitz T1).
  - Change Java from 11 to **17**.
  - Add `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
- **`gradle.properties`:** add `android.disallowKotlinSourceSets=false`. KSP needs it under AGP 9.
- **Manifest:**
  - Permissions: `READ_CONTACTS`, `CALL_PHONE`.
  - `<uses-feature android:name="android.hardware.telephony" android:required="false"/>`, because `CALL_PHONE` would otherwise imply the feature is required.
  - A `<queries>` entry for `ACTION_DIAL`/`ACTION_CALL` with `tel:`, so the app can check that a phone app exists.
  - `allowBackup="false"`, and the backup and extraction rules exclude everything.
  - Theme window background matches the default theme, so there's no white flash.
- **Architecture:** MVVM with `StateFlow`. Each ViewModel has a companion `Factory`, and DAOs come from `GodfatherDatabase.getInstance()` with no DI framework. Database file: `godfather-reevz.db`.
- **Settings storage:** a single-row Room `app_settings` table, updated column by column.
- **Docs, Habitz-style:**
  - `docs/SPEC.md`: your brief, verbatim, plus the decisions above.
  - `docs/PLAN.md`: phases and status.
  - `docs/THIRD_PARTY.md`: font and icon licences.
  - `CLAUDE.md`: stack, rules, toolchain notes and current state, adapted from Habitz's.

### Reused from ReevzHabitz
All files are under `C:\Users\reeva\AndroidStudioProjects\ReevzHabitz\app\src\main\java\com\reevan\reevzhabitz\`. Each is copied and adapted, not depended on.
- `util/TodayClock.kt` and `util/ClockChanges.kt`: the single source of "today". It ticks at midnight and refreshes on resume and on clock or time-zone changes.
- `util/Dates.kt`: date helpers, plus a new `dd-mm-yyyy` formatter.
- `util/TextCase.kt`: `capitalizeWords` for names.
- `ui/navigation/{Destination,BackStack,LeaveGuard}.kt`: the hand-rolled back stack (`pushFrom`, `popIfCurrent`, saveable routes) and the "Discard changes?" guard.
- `ui/common/Submission.kt`: one-shot writes that survive rotation and can't double-insert.
- `ui/common/AppHeader.kt`: the thin header pattern.
- `ui/menu/MenuScreen.kt`: the game-menu buttons.
- `ui/settings/{SettingsScreen,PreferencesViewModel}.kt`: radio rows with theme swatches.
- `MainActivity`: holds the first frame until settings load, and makes system bars follow the app theme.
- `tools/make_icon.py`: the script approach for the adaptive icon (background, foreground and monochrome layers).
- `tools/fetch_habit_icons.py`: Material Symbols drawables for UI icons (Apache 2.0).
- From ReevzMealz `ui/settings/SettingsScreen.kt`: the permission status, re-checked with `LifecycleResumeEffect`, and its "Allow" button.

### Package layout (`com.reevan.godfatherreevz`)
- `data/`: entities, DAOs, database, converters.
- `rotation/`: the **pure** scheduling engine. This is where the core rules live, and it is fully unit-tested.
- `util/`: today clock, dates, text, phone-number normalising, `Caller` (placing the call).
- `contacts/`: queries the phone's contacts and parses picker results.
- `ui/`: `GodfatherApp.kt` (shell), plus `home/`, `details/`, `menu/`, `addfriend/`, `contactpicker/`, `editfriend/`, `removefriend/`, `statistics/`, `settings/`, `common/`, `navigation/`, `theme/`.

---

## Data model: schema v1 (designed once, frozen once real friends are on the phone)
```
friends
  id          INTEGER PK autoincrement
  name        TEXT              -- capitalised words
  phone       TEXT              -- as entered/imported (display + dialling)
  notes       TEXT  default ''  -- D4 conversation notes
  addedOn     INTEGER           -- epoch day ("friend since")
  createdAt   INTEGER           -- epoch millis (tiebreaks)
  position    INTEGER           -- 0..n-1, the global call order (≈ days until their next call)
  turnUsed    INTEGER (bool)    -- already called in the current pass

calls                            -- one row = "I pressed Call for this friend on this date"
  id           INTEGER PK
  friendId     INTEGER? FK → friends.id ON DELETE SET NULL   (index)
  friendName   TEXT              -- snapshot, so history survives removal
  date         INTEGER           -- epoch day (index)
  scheduled    INTEGER (bool)    -- this was the day's focus call
  consumedTurn INTEGER (bool)    -- this call used the friend's turn (needed for a correct Undo)
  UNIQUE(friendId, date)         -- redial the same day never double counts

app_settings (single row, id = 1)
  themeKey     TEXT    -- SICILIAN_SUNSET | SUNSET_AFTER_DARK | CORLEONE_NOIR | LIMONCELLO | AMALFI | NOTHING
  callDirectly INTEGER -- default 1
```
- "Last called" is `MAX(calls.date)` per friend, and "ever called" means a calls row exists. Nothing is stored twice, so nothing can drift.
- Writes to `friends` are always targeted `UPDATE`s, **never `REPLACE`**. A REPLACE deletes the row, and `SET NULL` would then detach all of that friend's history.
- Lists are always ordered by `(position, id)`.

## Rotation engine (`rotation/Rotation.kt`, pure Kotlin, injected `Random`)
It works on `List<Slot(id, turnUsed, everCalled, lastCallDate)>` plus `today` and `todayLocked`, and returns the new order. Each DAO `@Transaction` reads the slots, calls one function and writes back only the rows that changed. The head, `todayDone` and whether a call is "scheduled" are **always worked out inside the transaction**, never taken from what the screen last showed.

**Invariant:** every waiting friend (`turnUsed = 0`) comes before every friend whose turn is used. Position 0 is the head.

- **Today's state:**
  - `todayDone` means a `scheduled` call exists dated between today and today+2. The +2 window stops a clock moved backwards from unlocking a second scheduled call, and caps the damage from a wrongly set future clock.
  - **Focus:** if today is done, the friend from that call. If that friend has since been removed, the focus card shows the saved name, marked done. Otherwise the focus is the head, which stays put until called (D1).
- **Recording a call** to friend F:
  - `isFocus = !todayDone && F == head`.
  - The call row is written with `INSERT OR IGNORE`.
  - **If the row was inserted:**
    - If F was waiting, F's turn is used and F is placed using the **spacing rule**.
    - If F's turn was already used, F is re-placed with the spacing rule, so they aren't called again soon. This doesn't use a turn.
  - **If the row already existed** (F was called earlier today) and F is now the focus: the row is marked `scheduled`, and F's turn is used if it wasn't. This stops today getting stuck after removals.
  - **Rollover:** if nobody is still waiting, set `turnUsed = 0` for everyone. **No reordering**, so there is never a visible reshuffle.
- **Spacing rule:** a just-called friend goes to a uniformly random position in `[max(w, k), n−1]`, where `w` is the number still waiting and `k = min(3, n/2)`.
  - A friend's position is roughly the days until their next call, so nobody is called again within `k` days, even across the end of a round.
  - With n = 1, that one friend is today's friend every day.
- **Adding a friend (D2):**
  - `lo = 1` if today's head is locked (today not done), otherwise `0`.
  - `hi` = the first waiting friend at or after `lo` who has been called before. If there is none, `hi` = the end of the waiting block.
  - The new friend is inserted at a random index in `[lo, hi]`.
  - So new friends are shuffled among never-called friends, or go to the front once everyone waiting has been called before. A bulk import inserts them one at a time, so they are interleaved at random.
- **Shuffle again:**
  - The waiting block is shuffled as two groups, never-called then called-before. Today's head is included if today isn't done.
  - The used block is shuffled too, with the spacing rule applied to friends called in the last `k` days.
- **Removing friends:** delete them, renumber, and run the rollover check. If today's undone head is removed, the next friend becomes today's.
- **Undo** (allowed only for a call dated today; the order is touched only if the DELETE removed exactly 1 row):
  - **Scheduled call:** F is waiting again at position 0, so F is today's focus again and not done.
  - **Off-schedule call that used a turn, with F still marked as used:** F is waiting again, at a random spot by the add rule.
  - **Otherwise** (a rollover has happened since, or no turn was used): the order is left alone.
- **Months:** none of this involves months (see "My calls").
- **Round boundaries:** there are no round numbers, counters or reset animations anywhere. Statistics and details show no per-friend call counts, because equal counts would give a boundary away. One honest limit: the cards show last-called dates, as you asked, so a sharp eye can still infer a repeat from an upcoming card's date.
- **Known consequence of D2:** adding friends often keeps pushing back friends who have been called before. That is what the rule says.

### Call flow robustness
- **Quick taps:** the Call button is disabled while a write is in flight, and the unique index plus `INSERT OR IGNORE` make the write idempotent.
- **Order of operations:** the write runs on an **app-wide coroutine scope**, not `viewModelScope`. `startActivity` (call or dialer) fires straight away in the same tap. That way the call can't be lost if Android kills the app while you're on the phone, and the dialer can't be blocked by the app going to the background.
- **"Today":** always comes from `TodayClock` and is passed into the transaction. Nothing else calls `LocalDate.now()`.

---

## Screens

The header appears on **every** screen and is thin. The ⚙ Settings and ☰ Menu icons are always on the right. Home shows "Godfather Reevz" and today's date on the left. Every other screen shows a back arrow and a breadcrumb such as `Home › Menu › Edit Friend`. ⚙ and ☰ jump to `[Home, Settings]` or `[Home, Menu]` through the LeaveGuard.

### Home: the focus stack
- **Layout:** one `LazyColumn`.
  - **Past (faded):** the last 7 calls up to today, sorted by `(date, id)`. One card per friend, current friends only, leaving out today's focus. Oldest at the top, each showing that call's date.
  - **Today's card:** larger, with a primary gradient and a "TODAY" chip. It shows the name, the phone number and the last-called date bottom-right. A soft pulsing glow means the call isn't done yet.
  - **Upcoming:** every other friend in next-call order. Each card shows the name, with the last-called date (`dd-mm-yyyy` or `-`) bottom-right. The first upcoming card has an "Up next" chip.
- **Today's card when the call is done:** it stays in focus until midnight, marked "✓ Called today".
- **Scrolling:** the list opens scrolled so today's card is centred. It re-centres when the focus changes, for example at midnight.
- **Rounds stay invisible:** the past section is call history, not "everyone called this round". The list never visibly resets at the end of a round, so you can't tell when repetition starts.
- **Tapping:** any card opens that friend's details (D3).
- **Empty state:** "No friends yet", with an **Add your first friend** button.
- **Polish:** `Modifier.animateItem()` and haptics.

### Friend details (from any card)
- **Content:**
  - Big name in the theme's display font, and the phone number.
  - Facts: last called (`dd-mm-yyyy` or "Never") and friend since. There is no per-friend call count; see Rotation.
  - A **Notes** box (multi-line, saved automatically as you type with a debounce, and on leaving).
- **Fixed bottom button,** whose label depends on the friend:
  - **Today's friend, not yet called:** "Call ⟨name⟩". Records today's scheduled call.
  - **Another friend:** "Call". Records the call and uses their turn, with a small hint: "Counts as their call; they go to the back of the line."
  - **Already called today:** "Call again". Dials without recording again, and shows "Called today ✓ · Undo".
- **Call flow:**
  - Check that a phone app exists; if none, show an error and record nothing.
  - Write the call, in a transaction that is idempotent per friend and day, then launch the call or dialer.
  - With "Call directly" on and the permission not yet granted, ask for it. If refused, open the dialer instead.
  - The Submission token and the unique `(friendId, date)` index stop double taps from counting twice.

### Menu
Four big buttons, styled as a game menu, as in Habitz: **Add Friend · Edit Friend · Remove Friend · Statistics**.

### Add Friend
- **"Choose from contacts" button,** which opens the contact importer.
- **Importer:**
  - A short explanation, then the `READ_CONTACTS` request.
  - A searchable A–Z list with checkboxes. Contacts with several numbers show a choice of number.
  - Numbers already saved appear disabled as "Added".
  - A bottom button, "Add N friends". After adding, a snackbar says "N friends added".
- **If the permission is refused:** an explanation, an **Open settings** button, and **"Pick one at a time instead"** (the system picker fallback above).
- **Manual form:**
  - **Name:** required. Words are capitalised on display and saved capitalised (Habitz pattern).
  - **Phone:** required, phone keyboard. Valid input is 3–15 digits, with `+ - ( ) .` and spaces allowed.
  - **Duplicate check:** the number is normalised to digits and the last 10 digits compared. A match shows "Already saved as ⟨name⟩".
  - **"Add Friend" button** (bottom right). It clears the form and shows a snackbar, so you can add several in a row.
- **Placement:** each new friend enters the rotation by rule D2.

### Edit Friend
- A friend list, A–Z with search, opens an editor for **name and phone**.
- **Save** is enabled only for a real, valid change, and the duplicate check skips the friend being edited.
- Going back with changes asks "Discard changes?".
- Editing never touches rotation or history: it is an in-place `UPDATE`, never `REPLACE`.

### Remove Friend
- A–Z list. Tapping a row toggles its checkbox; the button reads **Remove (n)**.
- A confirm dialog says "Their past calls stay in Statistics".
- Then delete. Removing today's friend makes the next one today's.
- Returns to Menu.

### Statistics (simple; never mentions rounds)
- **Tiles:**
  - Friends.
  - Total calls.
  - **This month:** calls so far against days elapsed.
  - **Current streak:** consecutive days with a call. Yesterday's streak holds until today ends.
  - **Longest streak.**
  - **Never called.**
- **Current month strip:** day dots, filled where a call was made, with a ring on today.
- **"Longest since last call" (top 3).**
- **Calls per month** for the last 6 months, as a small bar row.
- No per-friend call counts, so Statistics can't reveal a round boundary.

### Settings
- **Theme:** radio rows with a mini swatch preview for each theme.
- **Calling:** the "Call directly" switch, plus the phone-permission status and an Allow button when it is denied.
- **Contacts permission:** status, with an Open settings button.
- **Order:** a **Shuffle again** button, with a confirm dialog: "Everyone still waiting gets a new random spot, including today's friend if you haven't called yet. Friends you've already called won't jump the queue." Then a snackbar: "Shuffled".
- **About:** app version.

---

## Themes (fixed palettes, D7). Default: Sicilian Sunset
Every theme fills **all** Material roles, including the `surfaceContainer*` ones, as Habitz does. Each also has a small `GodfatherColors` set for the focus-card gradient, the faded past cards and the chips. Dynamic colour is off. `ThemeColorsTest` checks WCAG contrast for every theme.

| Theme | Mode | Feel |
|---|---|---|
| **Sicilian Sunset** (default) | light | Coral `#FF6F5E` and sunflower `#FFC23C` on warm cream `#FFF6EA`, espresso text `#2B1D17`. Focus card is a coral→yellow gradient. |
| **Sunset After Dark** | dark | The default's dark sibling: coral and yellow on deep espresso `#1C1412`. |
| **Corleone Noir** | dark | A tuxedo look: black `#0E0C0B` and charcoal, rose-red `#E5484D` with gold `#E8B44A` accents. |
| **Limoncello** | light | Lemon `#FFD93D`, leaf green `#4E7A3A`, white and pale-lemon surfaces. |
| **Amalfi Coast** | light | Coral with Mediterranean blue `#1F6FA3` on sand `#FBF3E4`. |
| **Nothing** | dark | Made for the Phone (2a): pure black, white, Nothing red `#D71921`. Names and headers use a dot-matrix font. |

Fonts are bundled OFL TTFs in `res/font`, fetched by `tools/fetch_fonts.py` and recorded in THIRD_PARTY.md:
- **Playfair Display:** names and headers, for the Godfather feel, in every theme except Nothing.
- **Doto:** the dot-matrix font, for the Nothing theme.
- Body text stays the system font.

## App icon
`tools/make_icon.py` (Habitz approach) generates the adaptive vector layers:
- **Background:** a coral→yellow sunset gradient.
- **Foreground:** a Don Corleone-style bust. Slicked-back dark hair, a stern brow, a black tuxedo with white shirt and bow tie, and a red rose on the lapel, kept inside the 66 dp safe zone.
- **Monochrome:** the silhouette, for Nothing OS and Android themed icons.

The script also writes a PNG preview. I'll check the preview and a launcher screenshot from the phone.

---

## Phases
Each phase ends with a building app, a test run, a report and a local commit. "Current state" in `CLAUDE.md` and `docs/PLAN.md` is updated each time.

| # | Phase | Delivers | Status |
|---|---|---|---|
| 1 | **Foundations** | Gradle and manifest setup. Docs (SPEC, PLAN, CLAUDE.md). Schema v1, entities and DAOs. TodayClock. The **rotation engine with its full unit tests**. Back stack, header, Menu screen, placeholder screens. | ✅ |
| 2 | **Look & feel** | The 6 themes, fonts, `ThemeColorsTest`, the shared `FriendCard`, UI icons, the app icon, and Settings → Theme. |  |
| 3 | **Add Friend** | Manual form with validation and duplicate checks. The contact importer, with its permission flow and system-picker fallbacks. |  |
| 4 | **Home + Details + Call** | The focus stack, details with notes, the Call flow (permission, dialer fallback, idempotent recording), and Undo. 🚩 **Ready for real use.** Test builds go on the phone from Phase 1, but once you start adding real friends, schema v1 is frozen: every change needs a migration, and I back up the phone's database before any test run. |  |
| 5 | **Edit & Remove** | The editor with discard guard; multi-select remove. |  |
| 6 | **Statistics** | The pure `CallStats` calculator (streaks, month, tops) with tests, and the Statistics screen. |  |
| 7 | **Settings & polish** | Call-directly switch, permission statuses, **Shuffle again**, empty states, a check of every theme, lint, a long-name and font-scale pass, and a full pass on the device. |  |

---

## Verification
- **Unit tests** (`.\gradlew.bat testDebugUnitTest`):
  - `RotationTest`, with a seeded Random, covers every rule above:
    - Today stays locked until called; missed days.
    - Adding a friend in the first pass and in later passes, including the case where the head is called-before and today is done.
    - Off-schedule calls of waiting friends and of friends whose turn is used; a same-day redial.
    - Undo: scheduled, off-schedule, after a rollover, and a double undo.
    - Removing the head, the last waiting friend, or everyone. Today's called friend removed.
    - Reshuffle; n = 0, 1, 2 and 3; clock moved backwards.
    - **Property-style runs** simulate thousands of days of random adds, removes, calls, missed days and reshuffles. They assert that nobody is called twice before everyone is called once, a new friend is called before any repeat, today never changes mid-day except through reshuffle or removal, and spacing holds when n ≥ 2.
  - `CallStatsTest`, `PhoneNumbersTest` (normalising, validating, duplicate matching), `DatesTest`, `BackStackTest`, `ThemeColorsTest`.
- **Instrumented tests** (`connectedDebugAndroidTest`):
  - `GodfatherDatabaseTest`: in-memory Room. Rotation transactions, FK `SET NULL` when a friend is removed, and the unique call per day.
  - `AppFlowsTest`: add → home focus → details → call (with a fake `Caller`, so no real call is placed) → undo → edit → remove → stats → reshuffle → theme.
  - These may run on the phone **only before you start using it for real**. After that they run on an emulator, or the phone's database is backed up first (`adb exec-out run-as com.reevan.godfatherreevz cat databases/godfather-reevz.db`), because the test run uninstalls the app.
- **On the phone** (`adb -s 00050146M001006 install -r`, launch with `am start`):
  - Drive the app with `adb shell input` and `uiautomator dump`, and check `adb exec-out screencap` screenshots for every screen in every theme.
  - Check the contact importer against your real contacts. It reads only; **nothing is saved unless you confirm**.
  - Test calls in **dialer mode**, which places nothing. **I'll ask you before placing any real direct call.**
  - Test midnight rollover and missed days by turning automatic time off and changing the date in the phone's Settings, then turning automatic time back on. Unit tests already cover the logic for every date edge.
- `.\gradlew.bat lint` stays clean, with no new warnings.
</content>
</invoke>
