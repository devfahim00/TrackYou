package com.devfahim00.trackyou

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devfahim00.trackyou.data.Prefs
import com.devfahim00.trackyou.data.ThemeMode
import com.devfahim00.trackyou.ui.LockScreen
import com.devfahim00.trackyou.ui.MainScreen
import com.devfahim00.trackyou.ui.OnboardingScreen
import com.devfahim00.trackyou.ui.TrackYouTheme
import com.devfahim00.trackyou.util.Reminders

/**
 * FragmentActivity (instead of ComponentActivity) so BiometricPrompt can be
 * shown from the lock screen.
 */
class MainActivity : FragmentActivity() {

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result reflected by system settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Reminder engine: channels + (re)schedule workers according to prefs.
        runCatching {
            Reminders.ensureChannels(this)
            Reminders.sync(this)
        }

        setContent {
            val vm: MainViewModel = viewModel()
            val dark = when (vm.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            var locked by remember { mutableStateOf(vm.appLockEnabled) }

            // Re-lock whenever the app goes to the background.
            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP && vm.appLockEnabled) locked = true
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            TrackYouTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (vm.userName.isBlank() || vm.currency == null) {
                        OnboardingScreen(vm)
                    } else if (locked && vm.appLockEnabled) {
                        LockScreen(vm) { locked = false }
                    } else {
                        MainScreen(vm)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        askNotificationPermissionOnce()
    }

    /** Android 13+ requires POST_NOTIFICATIONS to be requested at runtime. */
    private fun askNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT >= 33) {
            val prefs = Prefs(this)
            if (!prefs.notifPermAsked) {
                prefs.notifPermAsked = true
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
