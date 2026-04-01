package com.example.hanaparal.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// ── Capability check ──────────────────────────────────────────────────────────

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNSUPPORTED
}

fun checkBiometricAvailability(context: Context): BiometricAvailability {
    val manager = BiometricManager.from(context)
    return when (manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
        else -> BiometricAvailability.UNSUPPORTED
    }
}

// ── Prompt builder ────────────────────────────────────────────────────────────

fun buildBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (Int, String) -> Unit,
    onFailed: () -> Unit
): BiometricPrompt {
    val executor = ContextCompat.getMainExecutor(activity)
    return BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }
    )
}

fun buildPromptInfo(
    title: String = "Verify Identity",
    subtitle: String = "Use biometrics to continue",
    negativeButtonText: String = "Cancel"
): BiometricPrompt.PromptInfo {
    return BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(negativeButtonText)
        .build()
}

// ── Composable helper ─────────────────────────────────────────────────────────

fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun rememberBiometricLauncher(
    title: String = "Verify Identity",
    subtitle: String = "Use biometrics to continue",
    onSuccess: () -> Unit,
    onError: (Int, String) -> Unit = { _, _ -> },
    onFailed: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    return remember(context, title, subtitle, onSuccess, onError, onFailed) {
        {
            val activity = context.findActivity()
                ?: error("BiometricPrompt requires a FragmentActivity context")

            val availability = checkBiometricAvailability(context)
            if (availability != BiometricAvailability.AVAILABLE) {
                onError(-1, "Biometric authentication unavailable: $availability")
            } else {
                val prompt = buildBiometricPrompt(
                    activity = activity,
                    onSuccess = onSuccess,
                    onError = onError,
                    onFailed = onFailed
                )
                val promptInfo = buildPromptInfo(title, subtitle)
                prompt.authenticate(promptInfo)
            }
        }
    }
}

