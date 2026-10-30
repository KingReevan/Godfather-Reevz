package com.reevan.godfatherreevz.ui.navigation

/**
 * Every screen in the app. [label] is the screen's breadcrumb, and for the four Menu screens also
 * its Menu button text.
 *
 * [route] is a string form used only to save the back stack across process death; [fromRoute]
 * reverses it.
 */
sealed class Destination(val label: String, val route: String) {
    data object Home : Destination("Home", "home")
    data object Menu : Destination("Menu", "menu")
    data object Settings : Destination("Settings", "settings")
    data object AddFriend : Destination("Add Friend", "add")
    data object ContactImport : Destination("Contacts", "contacts")
    data object EditFriendList : Destination("Edit Friend", "edit")
    data object RemoveFriend : Destination("Remove Friend", "remove")
    data object Statistics : Destination("Statistics", "stats")

    // Per-friend screens have short generic crumbs; the screens themselves show whose they are.
    data class FriendDetails(val friendId: Long) : Destination("Friend", "friend/$friendId")
    data class EditFriend(val friendId: Long) : Destination("Edit", "edit/$friendId")

    // Both lists are lazy on purpose. The companion is initialised as part of Destination itself,
    // before its nested objects exist, so an eager listOf(Home, ...) here captures nulls.
    companion object {
        /** The Menu screen's buttons, top to bottom. */
        val menuEntries: List<Destination> by lazy {
            listOf(AddFriend, EditFriendList, RemoveFriend, Statistics)
        }

        private val fixed: List<Destination> by lazy {
            listOf(
                Home, Menu, Settings, AddFriend, ContactImport, EditFriendList, RemoveFriend,
                Statistics,
            )
        }

        fun fromRoute(route: String): Destination {
            fixed.firstOrNull { it.route == route }?.let { return it }
            val (prefix, arg) = route.split('/', limit = 2).let { it[0] to it.getOrNull(1) }
            val id = requireNotNull(arg?.toLongOrNull()) { "Unknown route: $route" }
            return when (prefix) {
                "friend" -> FriendDetails(id)
                "edit" -> EditFriend(id)
                else -> throw IllegalArgumentException("Unknown route: $route")
            }
        }
    }
}
