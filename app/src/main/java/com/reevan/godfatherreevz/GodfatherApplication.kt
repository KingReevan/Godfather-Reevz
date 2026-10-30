package com.reevan.godfatherreevz

import android.app.Application
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.photos.FriendPhotos
import com.reevan.godfatherreevz.util.AndroidCaller
import com.reevan.godfatherreevz.util.Caller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    /**
     * What picks a friend's photo (provided to the UI as `LocalPhotoPicker`): the system photo
     * picker. UI tests replace it with one that returns a test image.
     */
    var photoPicker: ActivityResultContract<PickVisualMediaRequest, Uri?> = ActivityResultContracts.PickVisualMedia()

    override fun onCreate() {
        super.onCreate()
        // Photo files no friend uses (from a form that was abandoned, say) are tidied away.
        appScope.launch(Dispatchers.IO) {
            runCatching {
                val inUse = GodfatherDatabase.getInstance(this@GodfatherApplication).friendDao().photosInUse()
                FriendPhotos.sweep(this@GodfatherApplication, inUse.toSet())
            }
        }
    }
}
