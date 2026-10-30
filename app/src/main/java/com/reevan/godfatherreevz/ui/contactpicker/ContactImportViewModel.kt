package com.reevan.godfatherreevz.ui.contactpicker

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.godfatherreevz.contacts.PhoneContact
import com.reevan.godfatherreevz.contacts.PhoneContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

/** The phonebook as the contact importer shows it. */
sealed interface ContactsState {
    data object Loading : ContactsState
    data class Loaded(val contacts: List<PhoneContact>) : ContactsState
    data object Failed : ContactsState
}

/** Reads the phone's contacts for the importer. Adding them goes through AddFriendViewModel. */
class ContactImportViewModel(private val application: Application) : ViewModel() {

    private val _contacts = MutableStateFlow<ContactsState>(ContactsState.Loading)
    val contacts: StateFlow<ContactsState> = _contacts.asStateFlow()

    /** (Re)reads the phonebook — every time the importer opens, so new contacts show up. */
    fun load() {
        viewModelScope.launch {
            _contacts.value = withContext(Dispatchers.IO) {
                runCatching { ContactsState.Loaded(PhoneContacts.loadAll(application)) }
                    .getOrDefault(ContactsState.Failed)
            }
        }
    }

    /** What the system picker returned (read at once: its access is temporary). */
    suspend fun readPicked(uri: Uri): List<PhoneContact> = withContext(Dispatchers.IO) {
        runCatching { PhoneContacts.readPicked(application, uri) }.getOrDefault(emptyList())
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ContactImportViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
            }
        }
    }
}

/** Lower-cased with accents removed: "Émile" matches "emile". */
private fun String.folded(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

/**
 * Contacts matching the search [query]: by any part of the name (ignoring case and accents), or —
 * when the query has three or more digits — by number.
 */
fun filterContacts(contacts: List<PhoneContact>, query: String): List<PhoneContact> {
    val text = query.trim()
    if (text.isEmpty()) return contacts
    val needle = text.folded()
    val digits = text.filter(Char::isDigit)
    return contacts.filter { contact ->
        contact.name.folded().contains(needle) ||
            (digits.length >= 3 && contact.numbers.any { it.number.filter(Char::isDigit).contains(digits) })
    }
}

/** The letter a contact is filed under in the list: "Émile" → 'E'; digits and symbols → '#'. */
fun sectionOf(name: String): Char {
    val first = name.trim().folded().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (first in 'A'..'Z') first else '#'
}
