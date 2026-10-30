package com.reevan.godfatherreevz.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.godfatherreevz.data.AppSettings
import com.reevan.godfatherreevz.data.AppSettingsDao
import com.reevan.godfatherreevz.data.AppTheme
import com.reevan.godfatherreevz.data.GodfatherDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide preferences.
 *
 * MainActivity and Settings share one instance (`viewModel()` resolves to the activity's store),
 * so a theme change applies immediately.
 */
class PreferencesViewModel(
    private val dao: AppSettingsDao,
) : ViewModel() {

    /**
     * The stored settings, or null until the first read completes. MainActivity holds the first
     * frame until this is non-null, so the app never flashes the default theme before the chosen
     * one. Started eagerly so that read begins before anything is on screen to subscribe.
     */
    val settings: StateFlow<AppSettings?> =
        dao.observe()
            .map { it ?: AppSettings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { dao.setTheme(theme) }
    }

    fun setCallDirectly(callDirectly: Boolean) {
        viewModelScope.launch { dao.setCallDirectly(callDirectly) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                PreferencesViewModel(
                    dao = GodfatherDatabase.getInstance(application).appSettingsDao(),
                )
            }
        }
    }
}
