package com.nehamahesh.pocketalpha

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : FragmentActivity() {
    private var biometricUnlocked by mutableStateOf(false)
    private var biometricPromptShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        biometricUnlocked = SessionStore(this).token == null
        setContent {
            PocketAlphaTheme {
                val viewModel: AppViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }

                LaunchedEffect(biometricUnlocked) {
                    if (!biometricUnlocked && !biometricPromptShown) {
                        biometricPromptShown = true
                        BiometricLock(this@MainActivity).authenticate(
                            onSuccess = { biometricUnlocked = true },
                            onUnavailable = { biometricUnlocked = true }
                        )
                    }
                }

                LaunchedEffect(state.error, state.notice) {
                    val message = state.error ?: state.notice
                    if (message != null && state.user != null) {
                        snackbar.showSnackbar(message)
                        viewModel.clearMessage()
                    }
                }

                Box(Modifier.fillMaxSize().background(AlphaColors.Background)) {
                    AnimatedContent(
                        targetState = when {
                            state.booting -> "launch"
                            state.user != null && !biometricUnlocked -> "locked"
                            state.user == null -> "auth"
                            state.selected != null -> "detail"
                            else -> "main"
                        },
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "root"
                    ) { destination ->
                        when (destination) {
                            "launch" -> LaunchScreen()
                            "locked" -> BiometricUnlockScreen {
                                biometricPromptShown = false
                                BiometricLock(this@MainActivity).authenticate(
                                    onSuccess = { biometricUnlocked = true },
                                    onUnavailable = { biometricUnlocked = true }
                                )
                            }
                            "auth" -> AuthScreen(state, viewModel::login, viewModel::register, viewModel::clearMessage)
                            "detail" -> StockDetailScreen(state, viewModel)
                            else -> MainShell(state, viewModel)
                        }
                    }
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
                    if (state.loading && state.user != null && state.selected == null) {
                        Box(Modifier.fillMaxSize().background(AlphaColors.Background.copy(alpha = .32f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AlphaColors.Green)
                        }
                    }
                }
            }
        }
    }
}
