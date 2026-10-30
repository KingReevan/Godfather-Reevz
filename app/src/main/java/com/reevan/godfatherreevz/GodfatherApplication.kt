package com.reevan.godfatherreevz

import android.app.Application
import com.reevan.godfatherreevz.util.AndroidCaller
import com.reevan.godfatherreevz.util.Caller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GodfatherApplication : Application() {

    /**
     * For writes that must finish even if the screen that started them goes away — above all
     * recording a call: the dialer opens in the same tap, the app goes to the background, and
     * Android may destroy the screen (and its ViewModel scope) before the write lands.
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * What places calls (provided to the UI as `LocalCaller`). UI tests replace it, before the
     * Activity starts, with a fake that never dials anyone.
     */
    var caller: Caller = AndroidCaller
}
