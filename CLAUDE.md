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
- `GodfatherDatabaseTest` — the rotation transactions against in-memory Room, photo updates, and
  `upgradingFromV1KeepsEverything`: a database built exactly as schema v1, opened by Room (which
  runs the auto-migration and checks the result). Add one of these for every new schema version.
- `FriendPhotosTest` — opening (EXIF rotation, size cap), saving, contact-photo copies, decode
  sizes and the sweep, on a real device's decoders.
- `AppFlowsTest` — end-to-end flows through the real Activity and **the real database and photos
  folder, which it wipes before every test**: empty Home → first friend, call / redial / undo,
  off-schedule call, duplicate number, edit + discard guard, remove + stats, shuffle confirm,
  theme + restart, header buttons, photo choose + frame + remove, add with a photo, edit's photo
  + discard guard. Calls go to `FakeCaller` and photo picks to `FakePhotoPicker` (set on
  `GodfatherApplication.caller` / `.photoPicker` before the Activity starts) — nobody is dialled,
  no gallery is opened. Add a flow here when adding or changing a user-facing flow. Compose's
  test clock drives coroutine delays: use `mainClock.advanceTimeBy`, not `Thread.sleep`.

**Real friends are on the phone: run instrumented tests on the emulator**
(`$env:ANDROID_SERIAL="emulator-5554"`, AVD `habitz_test`, API 34). `connectedAndroidTest`
uninstalls the app when it finishes, and `AppFlowsTest` wipes the database — on the phone that
would delete the owner's friends, notes, call history and photos. Wake and unlock the emulator
first (`adb -s emulator-5554 shell "input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard"`) or
every UI test fails with "No compose hierarchies found". Never run them on the phone.

**No backups of the phone's data** (the owner's choice, 2026-10-30): don't copy its database or
photos off the phone. A schema change is proven before it goes on the phone by its migration test
on the emulator — a database built exactly as the previous schema, upgraded by Room (see
`upgradingFromV1KeepsEverything`).

Target phone: **Nothing Phone (2a), Android 16 (API 36)**, adb serial `00050146M001006`. Update it
with `adb -s 00050146M001006 install -r app/build/outputs/apk/debug/app-debug.apk` (keeps data).
adb lives in `%LOCALAPPDATA%\Android\Sdk\platform-tools` (not on PATH). The phone's font scale is
1.15 — if a test changes it, put it back to exactly that.

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

- Data is stored **locally** in Room (`godfather-reevz.db`), plus friends' photos as files in
  `files/photos`. **No backups**, by my choice: `allowBackup="false"` and the backup/extraction
  rules exclude everything (the `file` domain included).
- Once real data is on the phone, schema changes **must** use proper Room migrations
  (`@AutoMigration` where additive). Never `fallbackToDestructiveMigration`. Room schemas are
  exported to `app/schemas/` and committed.
- **Never write `friends` with REPLACE.** SQLite does it as delete + insert, and the delete sets
  every one of that friend's calls' `friendId` to null. Edits are targeted `UPDATE`s.
- Do not delete or reset user data as part of a normal feature. Call history, notes and photos
  are not recoverable. A photo file is deleted only when nothing uses it any more: replaced,
  removed, its friend removed, or unused for a day (`FriendPhotos.sweep`).

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

All eight phases of `docs/PLAN.md` are done (8 = profile photos, a later request: D8 in
`docs/SPEC.md`). **The owner has used the app for real since 2026-10-30** (32 friends, 20 with
contact photos): the schema (now v2) is frozen — changes only by a new version + migration — and
nothing that wipes or uninstalls the app may touch the phone (see "Build and verify").

Not exercised on a device: the Android 17 picker branch (phone is API 36), a real direct
`ACTION_CALL`, and midnight rollover by changing the phone's date.

- `data/` — Room schema v2: `friends` (position/turnUsed rotation columns, notes, `photo` — added
  in v2 by `AutoMigration(1, 2)`), `calls` (one row per friend per day pressed; `scheduled`,
  `consumedTurn`; FK `SET NULL` on removal), `app_settings` (theme, callDirectly). `FriendDao`
  holds every rotation transaction plus the photo updates (`replacePhoto`, `setPhotoIfNone`,
  `photosInUse`; `removeFriends` hands back the removed friends' photos); `CallDao` is read-only
  views. Dates are `LocalDate` stored as epoch days.
- `photos/` — `FriendPhotos`: a friend's photo is a copy — ≤512 px square JPEG, random name — in
  `files/photos`; `Friend.photo` is the file name. `open` (picker/contact URI → upright bitmap,
  EXIF applied, ≤2048 px), `save` (a framed `Square` → new file), `copyContactPhoto`, `load`
  (sampled for an avatar's size), `delete`, `sweep` (unused files over a day old; run at app
  start — a form may hold an unsaved one). A changed photo is a new file, so caches never go
  stale. `Framing` (pure, pinned by `FramingTest`): the framing step's centre/zoom in picture
  pixels, drag, pinch-around-the-fingers, clamping, and the saved square.
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
  the friend's photo and its date bottom-right: last-called (`-` when never) or, for past cards,
  that call's date.
- `ui/common/FriendAvatar` — `FriendAvatar` (photo, else `Initials`) and `ContactAvatar` (a
  contact's thumbnail, for the importer); decoded off the main thread into a small LRU cache.
- `ui/photo/PhotoEditor` — `EditablePhoto`, used by the friend page, Add and Edit: tap → Choose
  photo (`LocalPhotoPicker` = the system photo picker; the framing dialog: drag, pinch, Use
  photo) / Use contact photo (with contacts access) / Remove photo. One activity-wide
  `PhotoEditorViewModel` holds the picture being framed and the outcome, tagged with the asking
  screen's `owner`, so both survive rotation.
- `ui/settings/` — Settings: theme picker (radio rows with a swatch, each name in its own
  display font); Calling ("Call directly" switch + phone-permission row); Contacts (permission
  row; "Photos from contacts" → Copy: friends without a photo get their contact's, matched by
  `contactPhotoFor`); Order (Shuffle again → confirm dialog → `FriendDao.reshuffle` → "Shuffled"); About.
  Permission rows re-check on resume: "Allow" asks, and once Android stops asking, "Open
  settings".
- `GodfatherApplication.appScope` — for writes that must outlive the screen (recording a call,
  undo, notes, photo changes, reshuffle). `GodfatherApplication.caller` / `.photoPicker` — what
  `MainActivity` provides as `LocalCaller` / `LocalPhotoPicker` (tests swap in fakes). Its
  `onCreate` sweeps unused photo files.
- `ui/home/` — Home's focus stack. **Layout rules are the pure `buildHome`** (pinned by
  `HomeLayoutTest`): today's card = today's scheduled call's friend once made (stays until
  midnight; a removed friend shows by saved name), else the rotation head; upcoming = everyone else
  once, in call order; past = the last `PAST_CARDS` calls, one per friend, oldest first — call
  history, so nothing visibly resets at the end of a pass. `HomeViewModel` lives in the shell;
  `HomeScroll` (shell-held) keeps the list position across visits and centres today's card when
  the friend in focus changes.
- `ui/details/` — friend page: photo (`EditablePhoto`; a change saves at once and deletes the old
  file), facts, notes (debounced 600ms + saved on leaving, via appScope),
  "Called today ✓ · Undo" (confirm dialog), Call button. Call = `recordCall()` first (idempotent
  per friend per day), then `LocalCaller` places it: `ACTION_CALL` if "Call directly" and
  CALL_PHONE granted, else asks for the permission once and falls back to `ACTION_DIAL`. A 1.5s
  cool-down stops double taps. `TodayRole` (TO_CALL / CALLED / OTHER) drives the chip and the
  "not today's call" hint.
- `util/Search.matchesSearch` — the one search rule (name ignoring case/accents, or 3+ digits of a
  number) for the importer, Edit and Remove. `ui/common/FriendList` — `FriendSearchField`,
  `FriendRow`, `filterFriends`, `ListMessage`.
- `ui/editfriend/` — Edit Friend: A–Z searchable list → editor (`key`ed by friend id) for photo,
  name and phone; Save enabled only for a real, valid change (a new photo counts) (`FriendDraft.changes`, compared as
  saved); leaving with changes goes through `LeaveGuard` → "Discard changes?" → lands exactly
  where the owner was heading. Saving is an in-place UPDATE: rotation place, calls, notes untouched.
- `ui/removefriend/` — Remove Friend: A–Z searchable, whole row toggles the checkbox, red
  "Remove (n)" → confirm ("Their notes and photos go with them. Past calls stay in Statistics.")
  → back to Menu; their photo files are deleted. Calls keep the friend's name snapshot (FK `SET NULL`).
- `ui/statistics/` — **all numbers come from the pure `callStats`** (pinned by `CallStatsTest`):
  friends, calls made (removed friends' calls included), this month = days with a call / days so
  far (from the start, if that was this month), current streak (an uncalled today keeps
  yesterday's), longest streak, never called, a month calendar (days before the owner started
  are neutral, not missed), "Longest since a call" (top 3), calls per month for six months.
  **No per-friend call counts** — they would reveal pass boundaries.
- `util/Caller` — interface + `AndroidCaller`, provided through `LocalCaller` so UI tests can use a
  fake that never dials. **Never place a real call while testing**: deny the phone permission or
  turn "Call directly" off, and back out of the dialer.
- `ui/common/AppMessages` (`LocalAppMessages`) — the app-wide snackbar, owned by the shell so a
  message survives the screen closing; lifted 76dp so it never covers a bottom action button.
- `ui/common/Submission` — as in Habitz, plus `repeatable = true` for screens that stay open after
  their action (Add Friend).
- `ui/friendform/` — `FriendDraft` (pure: validation, duplicate check by `PhoneNumbers.sameNumber`,
  tidy save), `FriendFormScaffold`, `FriendNameField` (display-only capitalisation, as in Habitz),
  `FriendPhoneField`. Shared by Add and (phase 5) Edit.
- `ui/addfriend/` — Add Friend: "Choose from contacts" card + manual form (with an optional photo);
  adding clears the form and stays. `AddFriendViewModel.add` / `.addPicks` re-check duplicates
  against the database (`planAdd`, pure), copy each `ContactPick`'s contact photo for those
  added, and return an `AddResult` message ("5 friends added · 1 already saved").
- `contacts/PhoneContacts` — reads the phonebook (READ_CONTACTS), `groupContacts` (pure: one per
  contact, same-number duplicates merged, default number first, locale A–Z), the system picker
  intent (API 37+: `ContactsPickerSessionContract.ACTION_PICK_CONTACTS` multi-select; older:
  `ACTION_PICK` on phone numbers) and reading its result URI. Contacts carry their `photo`
  (PHOTO_URI) and `thumbnail`; `contactPhotoFor` (pure) finds a number's contact photo. The
  classic system picker asks for the photo too, but without contacts access it usually can't be
  read, so those friends come in without one.
- `ui/contactpicker/` — the importer: permission prompt (explanation → request; refused twice →
  Open settings; always offers the system picker), searchable A–Z list with sticky letters,
  checkboxes and contact photos, number choice for multi-number contacts, already-saved contacts
  disabled. "Add N friends" (photos copied) returns Home. System-picker picks are added at once. The API 37 picker branch has not
  been run on a device (the phone is API 36).

Generated assets — edit the script, not the output (licences in `docs/THIRD_PARTY.md`):
- `tools/fetch_icons.py` → `res/drawable/ic_*.xml` (Material Symbols Rounded, Apache 2.0).
- `tools/fetch_fonts.py` → `res/font/*.ttf` (SIL OFL).
- `tools/make_icon.py [preview.png]` → the adaptive launcher icon layers (`mipmap-anydpi`, no
  bitmaps — minSdk 26): the Don in a tuxedo with a red rose, on a coral→sunflower sunset. The
  monochrome layer (Nothing OS themed icons) is a silhouette with real holes for the details,
  scaled 1.2× because themed launchers draw it small.
