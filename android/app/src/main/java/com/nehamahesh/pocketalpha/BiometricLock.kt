package com.nehamahesh.pocketalpha

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

class BiometricLock(private val activity: FragmentActivity) {
    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(): Boolean =
        BiometricManager.from(activity).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        onSuccess: () -> Unit,
        onUnavailable: () -> Unit = {}
    ) {
        if (!canAuthenticate()) {
            onUnavailable()
            return
        }

        val executor = java.util.concurrent.Executor { command ->
            activity.runOnUiThread(command)
        }

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock PocketAlpha")
            .setSubtitle("Confirm your identity to access your portfolio")
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(info)
    }
}
