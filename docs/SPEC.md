# Godfather Reevz — Product Spec

> Source of truth for what the app does. The brief below is the owner's, kept verbatim. The
> decisions after it were made with the owner during planning (2026-10-29). If an implementation
> question isn't answered here, ask rather than guess.

## The brief

You must build an android app. For Nothing phone 2a. This app is simple. For every day of the month, I want to make one call to a friend so I can keep in touch with them. I'm doing this so that I don't lose contact with my friends. I have a lot of friends and it gets bad when I don't talk to any of them for a long time. So I decided to make this app. I have dedicated myself to calling one person every day. This app should help me do that. The app will help tell me who I should call on the current day and also show details of whom I called last (last date of call). Obviously, the app must let me add people to the application. I should be able to enter their names and phone number. CRUD operation must be supported for this though. The home page of the app will show all the names of the people. The name of the person whom I have to contact for 'today' will be highlighted and in focus. These names will be shown as horizontal cards stacked on top of each other. The bottom right of the card should have the date where I last called the person. Keep in mind that by 'last called' it doesn't mean that the receiver (the friend) picked up, it just means that I called them last on that date. You don't have to store timestamps or anything, just date is fine. When I click on the highlighted persons card then it should show me details about that person. There will be a 'Call' button at the bottom of that details page (suitable back button must be there so that I can go back to home page). Only when I click on that call button will the app count it as a call I made to that friend. That will be the only indicator that I made the call. When that button is pressed the call should be made to the friend at that number or redirect to the phone app if it is easier to implement. The entire app will have a header at the top with a settings button and a menu button (both present towards the right of the header). The settings will just have basic things like theme and if I want there to be any sort of configuration or modes or toggles then I will put it in the settings section. The menu button will open the menu for me. In the menu there will be the following options:

- Add Friend
- Edit Friend
- Remove Friend
- Statistics

You must obviously make the pages for each of the above options so that I can perform CRUD on my friends easily. There is one more important thing you must know. I want the app to have a shuffle feature. I don't want the friend names to show up in the order that I added them. I want the order to shuffle up. Shuffling is good as my friends will not be able to predict my calls. If I have more friends than the days of a month (for example, 40 friends as compared to 31 days in October) then the friends who I didn't call in October should appear in the next month, shuffled of course. You must handle all edge cases.

Give good and unique frontend themes for the app. Make use of friendly colors like yellow or coral (make them primary colors). Of course, that will be the default theme, I should be able to choose themes in settings (you will add a few themes I can choose from).

In the statistics menu page, you can just put simple stats.

The  settings must allow me to shuffle the names once more. There should be a shuffle button that will have a confirmation dialog box (to prevent accidental shuffling). When I click on this shuffle button and confirm then the contacts would be shuffled once more.

Whenever I add a new friend, then they may show up in the next month or whenever possible (but I must not make calls to people I have already made without making a call to the newly added friend). Everyone deserves a call. If I haven't finished making calls to some people then I shouldn't get repeated people at all. Only when I'm done calling all the friends then the repetition can start (of course I won't be able to tell when the repetition starts since there is no way for me to tell and it should stay this way).

When the app is initially made, I will have zero friends. When I add some friends then the `last called` in the card should just show a dash '-'. But after I call a friend, the date I called them must be recorded in the format dd-mm-yyyy

When adding a friend to the app, you could make it easy for me by letting me choose the contacts from the list of contacts I already have in my phonebook. Or I can enter everything manually. Research and see which is the better method (you must think through this in terms of user experience and whether it is possible).

You can freely ignore development costs.

App Name: `Godfather Reevz`
App Icon: Something resembling Don Coreleone. The Godfather. A man in a tuxed/suit.

---

## Decisions (made with the owner)

| # | Question | Answer |
|---|---|---|
| D1 | A day ends without pressing Call | **Today's friend stays until called.** Nobody is skipped; the schedule slides. |
| D2 | Where a new friend goes | **Before any repeat.** Shuffled in among never-called friends; if everyone still waiting has been called before, the new friend goes to the front (after today's friend, if today's call isn't made yet). |
| D3 | Tapping a card that isn't today's | **Details + Call.** Calling them records the date and uses their turn this pass; today's friend stays today's. |
| D4 | Extras | Only **per-friend notes**: one free-text box per friend on their details page, for conversation details. No reminder, widget, backup/restore or skip. |
| D5 | Home layout | **Focus stack (timeline):** recent past calls above (faded), today's card enlarged and centred, everyone else below in upcoming order. |
| D6 | Menu | **Full-screen Menu page** with four big buttons. |
| D7 | Themes | **Fixed themes**, each one complete look (light or dark), chosen in Settings. No "follow system". |
| D8 | Profile photos (asked for 2026-10-30, after real use began) | **Each friend can have a photo**, chosen by the owner; friends added from contacts bring their contact photo. See "Later requests". |

### Defaults chosen during planning (the owner approved the plan containing them)
- **Adding friends:** an in-app contact importer (asks for contacts permission once; searchable,
  multi-select, pick which number, already-added marked) plus manual entry. Refused permission →
  the system contact picker (no permission needed; multi-select on Android 17+, one at a time
  before).
- **Calling:** the Call button places the call directly (phone permission). Settings switch
  "Call directly" off, or the permission refused → the dialer opens with the number filled in.
- **Undo:** a call recorded today can be undone from the friend's details page (accidental press).
- **Removing a friend** keeps their past calls in Statistics totals.
- **No backups:** nothing leaves the phone (`allowBackup="false"`), as in Reevz Habitz.
- **No per-friend call counts** anywhere (equal counts would reveal when a pass ends).

## Later requests

### 2026-10-30 — profile photos (D8)
The owner, verbatim: "ok I added some friends. Is it not possible to put their profile pics as
well? I want to be able to choose their profile pics. If I added them from contacts then that
profile pic should also carry over."

How it works (defaults chosen while building it):
- **Choosing:** tap the photo on a friend's page, on Add Friend's form or in the Edit Friend
  editor → *Choose photo* (the system photo picker, no permission needed) → frame the face in a
  circle (drag, pinch to zoom) → *Use photo*. The same menu offers *Use contact photo* (the
  phonebook's photo for that number, with contacts access) and *Remove photo*. On the friend's
  page a change saves at once; in Add and Edit it goes with the form (Edit's discard guard covers
  it).
- **From contacts:** the importer copies each picked contact's photo. Friends added before this
  existed get theirs from Settings › Contacts › *Photos from contacts* → Copy, which fills in only
  friends without a photo, matched by number (the duplicate-number rule).
- **Stored as a copy** in the app's private storage (small square JPEGs), so a photo stays if the
  original or the contact changes, and — like everything else — never leaves the phone.
- Photos show wherever a friend does: Home's cards, their page, the Edit/Remove lists; the
  importer shows contacts' photos. A friend without one shows their initials.
