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
import com.reevan.godfatherreevz.util.folded
import com.reevan.godfatherreevz.util.matchesSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

/** Contacts matching the search [query] (see [matchesSearch]). */
fun filterContacts(contacts: List<PhoneContact>, query: String): List<PhoneContact> =
    contacts.filter { contact -> matchesSearch(contact.name, contact.numbers.map { it.number }, query) }

/** The letter a contact is filed under in the list: "Émile" → 'E'; digits and symbols → '#'. */
fun sectionOf(name: String): Char {
    val first = name.trim().folded().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (first in 'A'..'Z') first else '#'
}
