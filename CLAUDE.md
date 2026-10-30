# Godfather Reevz

## What this project is

Godfather Reevz is a **personal Android app** for my own phone: it tells me which friend to call
today (one a day, in a shuffled rotation) so I keep in touch with everyone.

**The product spec is [`docs/SPEC.md`](docs/SPEC.md) — read it before working on any feature.** It
is the source of truth for screens and behaviour (my brief verbatim, plus decisions D1–D7). Do
not invent features beyond it; where it is silent or ambiguous, ask. The approved plan, data
model and phase status are in [`docs/PLAN.md`](docs/PLAN.md); keep its status table up to date.

It is a personal app — not a multi-user SaaS product. Its siblings, Reevz Habitz (`..\ReevzHabitz`,
the closest reference), Reevz Drip and Reevz Mealz, use the same stack and conventions.

## Stack

- Kotlin + Jetpack Compose, Material 3
- Room for local persistence (KSP for its code generation)
- MVVM: `ViewModel` + `StateFlow`, each ViewModel exposing a `Factory` built with
  `viewModelFactory { initializer { ... } }`. No DI framework — DAOs come from
  `GodfatherDatabase.getInstance(application)`.
- Android-first, local-first / offline-first

## Core principles

- Keep the app simple and fast to use.
- **Do not introduce a backend, authentication, cloud database, analytics, or AI** unless I
  explicitly ask for it.
- Prefer local storage and offline functionality; prefer Android/Jetpack libraries.
- Keep the architecture understandable rather than over-engineered. Solve the problem in front of
  you, not hypothetical future ones.

## Working rules

- Make small, focused changes. Do not modify unrelated files.
- Preserve existing functionality when implementing new features.
- **Before making an architectural change, explain the reason first.**
- **Before adding a dependency, explain why it is needed.**
- Run the appropriate build/tests after significant changes.
- **Never claim a feature works without verifying it.** If verification did not run, or failed, say
  so plainly and show the output.
- Do not silently change project-wide configuration (Gradle files, version catalog,
  `gradle.properties`, manifest, theme). If a config change is genuinely required, call it out.

## Build and verify

Windows / PowerShell:

```
.\gradlew.bat assembleDebug          # compile the app
.\gradlew.bat testDebugUnitTest      # host unit tests
.\gradlew.bat lint                   # Android lint
```

Instrumented tests (`connectedDebugAndroidTest`):
- `GodfatherDatabaseTest` — the rotation transactions against in-memory Room.

**`connectedAndroidTest` uninstalls the app when it finishes, which deletes the database.** Once I
use the app for real, back up the phone's database first
(`adb exec-out run-as com.reevan.godfatherreevz cat databases/godfather-reevz.db`) and restore it
after — or run them on an emulator (`ANDROID_SERIAL=emulator-5554`).

Target phone: **Nothing Phone (2a), Android 16 (API 36)**, adb serial `00050146M001006`. Update it
with `adb -s 00050146M001006 install -r app/build/outputs/apk/debug/app-debug.apk` (keeps data).
adb lives in `%LOCALAPPDATA%\Android\Sdk\platform-tools` (not on PATH).

## Git

- Local repository; one commit per finished phase. **Never push** unless explicitly asked.
- Do not commit generated build artifacts, `local.properties`, IDE-specific files, or secrets.

## UI guidelines

- Design for a **phone**, one-handed: common actions reachable with a thumb.
- Every screen's header has the Settings and Menu buttons on the right (the brief).
- Use colour *roles* from `MaterialTheme.colorScheme`, or `GodfatherTheme.colors` for the few
  app-only colours — never hardcoded colours. Names and headings use `GodfatherTheme.displayFont`.
- Accessible touch targets (48dp minimum); minimum (not fixed) heights so big fonts don't clip.
- Navigation is the hand-rolled back stack in `ui/navigation/` (as in Habitz), not a library.

## Data guidelines

- Data is stored **locally** in Room (`godfather-reevz.db`). **No backups**, by my choice:
  `allowBackup="false"` and the backup/extraction rules exclude everything.
- Once real data is on the phone, schema changes **must** use proper Room migrations
  (`@AutoMigration` where additive). Never `fallbackToDestructiveMigration`. Room schemas are
  exported to `app/schemas/` and committed.
- **Never write `friends` with REPLACE.** SQLite does it as delete + insert, and the delete sets
  every one of that friend's calls' `friendId` to null. Edits are targeted `UPDATE`s.
- Do not delete or reset user data as part of a normal feature. Call history and notes are not
  recoverable.

## The rotation (read before touching scheduling)

- **All scheduling rules live in the pure `rotation/Rotation.kt`**, pinned by `RotationTest`
  (unit tests + long random simulations of the owner's rules). Change rules there, not in DAOs or
  UI. `FriendDao`'s `@Transaction` methods read the order, apply one rule, save changed rows.
- Model: `friends.position` (0 = head = today's friend while today's call isn't made) and
  `friends.turnUsed`. Every waiting friend precedes every used one; when nobody is left waiting
  everyone becomes waiting again **without reordering** — the pass boundary must stay invisible
  (spec: "I won't be able to tell when the repetition starts … it should stay this way"). So: no
  round numbers, no per-friend call counts, no "reset" moments anywhere in the UI.
- "Today's call is done" = a `scheduled` call dated within `TodayWindow` (today..today+2). Work it
  out inside the transaction from `today`, never from what a screen showed.
- `util/TodayClock` is the single source of "today". Never call `LocalDate.now()` elsewhere.

## Toolchain notes (non-obvious — read before touching Gradle)

- **AGP 9.3.3**, Gradle 9.5.0, Kotlin 2.2.10, Compose BOM 2026.02.01, Room 2.8.4, KSP
  2.2.10-2.0.2. Gradle daemon toolchain is Java 25 (`gradle/gradle-daemon-jvm.properties`).
- **There is no `org.jetbrains.kotlin.android` plugin.** AGP 9 compiles Kotlin itself.
- `minSdk` is 26 so `java.time` is available without desugaring.
- `compileSdk { version = release(37) }`; release `optimization { enable = false }` (R8 off).
- Gradle **configuration cache is enabled**.
- All dependencies and versions go through `gradle/libs.versions.toml`.
- **`android.disallowKotlinSourceSets=false` in `gradle.properties` is load-bearing** (KSP).
- `compileOptions` targets Java 17; no `jvmTarget` / `jvmToolchain` block is needed.

## Current project state

Phases 1–3 of `docs/PLAN.md` are done (foundations, look & feel, Add Friend); later phases fill
in the placeholder screens (`ui/common/ComingSoon`).

- `data/` — Room schema v1: `friends` (position/turnUsed rotation columns, notes), `calls` (one row
  per friend per day pressed; `scheduled`, `consumedTurn`; FK `SET NULL` on removal),
  `app_settings` (theme, callDirectly). `FriendDao` holds every rotation transaction; `CallDao` is
  read-only views. Dates are `LocalDate` stored as epoch days.
- `rotation/Rotation.kt` + `RotationTest` — see "The rotation" above.
- `util/` — `TodayClock` + `clockChanges` (from Habitz), `Dates` (`formatCallDate` = dd-mm-yyyy,
  `formatLastCalled` = "-" when never), `TextCase.capitalizeWords`, `PhoneNumbers` (validate,
  same-number match on the last 10 digits, dial string).
- `ui/navigation/` — `Destination`, `BackStack` (+ `jumpTo` for the header's Settings/Menu
  buttons), `LeaveGuard`. `ui/common/Submission` for one-shot writes. All from Habitz.
- `ui/common/AppHeader` — `HomeHeader` (app name + date), `BreadcrumbHeader`, `HeaderActions`
  (Settings + Menu, on every screen).
- `ui/menu/MenuScreen` — the four Menu buttons, game-menu style.
- `ui/theme/` — six fixed themes (`AppTheme`: Sicilian Sunset default, Sunset After Dark, Corleone
  Noir, Limoncello, Amalfi Coast, Nothing); `GodfatherTheme.colors` (today's-card gradient, past
  cards, called mark) / `.displayFont`. Contrast for every theme is pinned by `ThemeColorsTest` —
  run it after touching any colour. Fonts are bundled variable TTFs read through
  `FontVariation` (experimental API, opted in): Playfair Display, and Doto for Nothing — Doto's
  weight axis is its dot size, so each weight reads +200 further along it.
- `ui/common/FriendCards` — `TodayCard` (gradient, pulsing ring until called), `FriendCard`
  (upcoming; optional "Up next" chip), `PastCard` (faded recent call), `Initials`. Every card has
  its date bottom-right: last-called (`-` when never) or, for past cards, that call's date.
- `ui/settings/SettingsScreen` — theme picker (radio rows with a swatch, each name in its own
  display font).
- `GodfatherApplication.appScope` — for writes that must outlive the screen (recording a call).
- `ui/common/AppMessages` (`LocalAppMessages`) — the app-wide snackbar, owned by the shell so a
  message survives the screen closing; lifted 76dp so it never covers a bottom action button.
- `ui/common/Submission` — as in Habitz, plus `repeatable = true` for screens that stay open after
  their action (Add Friend).
- `ui/friendform/` — `FriendDraft` (pure: validation, duplicate check by `PhoneNumbers.sameNumber`,
  tidy save), `FriendFormScaffold`, `FriendNameField` (display-only capitalisation, as in Habitz),
  `FriendPhoneField`. Shared by Add and (phase 5) Edit.
- `ui/addfriend/` — Add Friend: "Choose from contacts" card + manual form; adding clears the form
  and stays. `AddFriendViewModel.add` re-checks duplicates against the database (`planAdd`, pure)
  and returns an `AddResult` message ("5 friends added · 1 already saved").
- `contacts/PhoneContacts` — reads the phonebook (READ_CONTACTS), `groupContacts` (pure: one per
  contact, same-number duplicates merged, default number first, locale A–Z), the system picker
  intent (API 37+: `ContactsPickerSessionContract.ACTION_PICK_CONTACTS` multi-select; older:
  `ACTION_PICK` on phone numbers) and reading its result URI.
- `ui/contactpicker/` — the importer: permission prompt (explanation → request; refused twice →
  Open settings; always offers the system picker), searchable A–Z list with sticky letters,
  checkboxes, number choice for multi-number contacts, already-saved contacts disabled. "Add N
  friends" returns Home. System-picker picks are added at once. The API 37 picker branch has not
  been run on a device (the phone is API 36).

Generated assets — edit the script, not the output (licences in `docs/THIRD_PARTY.md`):
- `tools/fetch_icons.py` → `res/drawable/ic_*.xml` (Material Symbols Rounded, Apache 2.0).
- `tools/fetch_fonts.py` → `res/font/*.ttf` (SIL OFL).
- `tools/make_icon.py [preview.png]` → the adaptive launcher icon layers (`mipmap-anydpi`, no
  bitmaps — minSdk 26): the Don in a tuxedo with a red rose, on a coral→sunflower sunset. The
  monochrome layer (Nothing OS themed icons) is a silhouette with real holes for the details,
  scaled 1.2× because themed launchers draw it small.
