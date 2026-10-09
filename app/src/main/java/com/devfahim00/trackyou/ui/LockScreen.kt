package com.devfahim00.trackyou.ui

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.devfahim00.trackyou.MainViewModel

/** Fingerprint / face unlock is possible on this device. */
fun canBiometric(context: android.content.Context): Boolean =
    BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS

/** Shows the system biometric dialog. Calls [onSuccess] when unlocked. */
fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock TrackYou")
        .setSubtitle("Use your fingerprint or face to continue")
        .setNegativeButtonText("Use PIN")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    runCatching { prompt.authenticate(info) }
}

/**
 * Full-screen lock shown on cold start and whenever the app comes back
 * from the background while app lock is enabled.
 */
@Composable
fun LockScreen(vm: MainViewModel, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val haptics = LocalHapticFeedback.current
    val pinLen = remember { vm.pinLength().coerceAtLeast(4) }
    val bioOk = remember { canBiometric(context) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun tryUnlock(candidate: String) {
        if (vm.checkPin(candidate)) {
            onUnlock()
        } else {
            error = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            pin = ""
        }
    }

    fun addDigit(d: Char) {
        if (error) error = false
        if (pin.length < pinLen) {
            pin += d
            if (pin.length == pinLen) tryUnlock(pin)
        }
    }

    BackHandler { /* locked: back is ignored */ }

    LaunchedEffect(Unit) {
        if (bioOk && vm.biometricUnlock && activity != null) {
            showBiometricPrompt(activity, onUnlock)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))
            Box(
                Modifier
                    .size(76.dp)
                    .background(HeroBrush, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Welcome back", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Enter your PIN to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            PinDots(count = pin.length, total = pinLen, error = error)
            Spacer(Modifier.height(10.dp))
            Text(
                if (error) "Wrong PIN, try again" else " ",
                style = MaterialTheme.typography.labelMedium,
                color = if (error) expenseColor() else Color.Transparent
            )
            Spacer(Modifier.height(18.dp))

            PinKeypad(
                onDigit = { addDigit(it) },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
            )

            Spacer(Modifier.height(10.dp))
            if (bioOk && activity != null) {
                TextButton(onClick = { showBiometricPrompt(activity, onUnlock) }) {
                    Icon(Icons.Rounded.Fingerprint, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Unlock with fingerprint")
                }
            }
        }
    }
}

/** Row of dots showing PIN entry progress. */
@Composable
fun PinDots(count: Int, total: Int, error: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            val filled = i < count
            Box(
                Modifier
                    .size(14.dp)
                    .background(
                        when {
                            error -> expenseColor().copy(alpha = 0.85f)
                            filled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape
                    )
            )
        }
    }
}

/** 0-9 numeric keypad with backspace, shared by lock and PIN setup. */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', '0', '<')
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.chunked(3).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                rowKeys.forEach { k ->
                    if (k == ' ') {
                        Spacer(Modifier.size(72.dp))
                    } else if (k == '<') {
                        IconButton(
                            onClick = onBackspace,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Backspace, "Backspace",
                                modifier = Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Box(
                            Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), CircleShape)
                                .clickable { onDigit(k) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                k.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
