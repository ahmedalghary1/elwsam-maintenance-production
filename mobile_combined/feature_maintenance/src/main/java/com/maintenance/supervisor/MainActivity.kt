package com.maintenance.supervisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.*
import com.maintenance.supervisor.data.security.TokenStore
import com.maintenance.supervisor.ui.*
import com.maintenance.supervisor.ui.screens.*
import com.maintenance.supervisor.ui.theme.MaintenanceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var tokens: TokenStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val embedded = intent.getBooleanExtra(EXTRA_EMBEDDED, false)
        setContent {
            MaintenanceTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SessionRoot(tokens, embedded) {
                        if (embedded) {
                            setResult(RESULT_LOGGED_OUT)
                            finish()
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_EMBEDDED = "elwsam_embedded"
        const val RESULT_LOGGED_OUT = 701
    }
}

@Composable private fun SessionRoot(tokens: TokenStore, embedded: Boolean, onEmbeddedLogout: () -> Unit) {
    val loggedIn by produceState<Boolean?>(null, tokens) { tokens.sessionActive.collect { value = it } }
    when (val active = loggedIn) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        false -> if (embedded) {
            LaunchedEffect(Unit) { onEmbeddedLogout() }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else key(active) { AppNav(false, onEmbeddedLogout) }
        true -> key(active) { AppNav(true, onEmbeddedLogout) }
    }
}

@Composable private fun AppNav(loggedIn: Boolean, onEmbeddedLogout: () -> Unit) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = if (loggedIn) "home" else "login") {
        composable("login") { LoginScreen(hiltViewModel()) { nav.navigate("home") { popUpTo("login") { inclusive = true } } } }
        composable("home") { HomeScreen(hiltViewModel(), onInspect = { nav.navigate("inspection") }, onLogout = onEmbeddedLogout) }
        composable("inspection") { InspectionScreen(hiltViewModel(), onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() }) }
    }
}
