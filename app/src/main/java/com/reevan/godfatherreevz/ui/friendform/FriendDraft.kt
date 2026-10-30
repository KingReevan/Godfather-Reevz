package com.reevan.godfatherreevz.ui.friendform

import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.data.NewFriend
import com.reevan.godfatherreevz.util.PhoneNumbers
import com.reevan.godfatherreevz.util.capitalizeWords

/** Longest name accepted, in characters: room for a full name, short enough for a card. */
const val MAX_NAME_LENGTH = 60

/**
 * A friend being typed into Add Friend or the Edit Friend editor. [photo] is the photo file chosen
 * on the form (already saved), or null for none.
 */
data class FriendDraft(val name: String, val phone: String, val photo: String? = null) {

    /** As it will be saved: words capitalised, whitespace tidied; number trimmed. */
    fun toNewFriend(): NewFriend = NewFriend(
        name = capitalizeWords(name.trim().replace(Regex("\\s+"), " ")),
        phone = PhoneNumbers.clean(phone),
        photo = photo,
    )

    /**
     * Whether saving this over [original] would change anything, compared as saved (so adding a
     * trailing space, or lower-casing a letter the save capitalises anyway, is no change).
     */
    fun changes(original: Friend): Boolean =
        toNewFriend().let { it.name != original.name || it.phone != original.phone || it.photo != original.photo }

    /**
     * Whether this can be saved, and what to tell the user. [existing] are the friends already
     * saved; [exceptId] is the friend being edited, who may of course keep their own number.
     *
     * Errors only appear for things typed in: an empty field just keeps the button disabled.
     */
    fun check(existing: List<Friend>, exceptId: Long? = null): DraftCheck {
        val nameBlank = name.isBlank()
        val phoneBlank = phone.isBlank()
        val phoneValid = !phoneBlank && PhoneNumbers.isValid(phone)
        val duplicate = if (phoneValid) {
            existing.firstOrNull { it.id != exceptId && PhoneNumbers.sameNumber(it.phone, phone) }
        } else {
            null
        }
        val phoneError = when {
            phoneBlank -> null
            !phoneValid -> "Enter a valid phone number"
            duplicate != null -> "Already saved as ${duplicate.name}"
            else -> null
        }
        return DraftCheck(
            canSave = !nameBlank && phoneValid && duplicate == null,
            phoneError = phoneError,
        )
    }
}

data class DraftCheck(val canSave: Boolean, val phoneError: String?)

/** The friend among [existing] already saved with [phone]'s number, if any. */
fun findSameNumber(existing: List<Friend>, phone: String): Friend? =
    existing.firstOrNull { PhoneNumbers.sameNumber(it.phone, phone) }
