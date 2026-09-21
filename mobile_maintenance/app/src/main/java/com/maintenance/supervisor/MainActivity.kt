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
        setContent { MaintenanceTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { SessionRoot(tokens) } } }
    }
}

@Composable private fun SessionRoot(tokens: TokenStore) {
    val loggedIn by produceState<Boolean?>(null, tokens) { tokens.sessionActive.collect { value = it } }
    loggedIn?.let { key(it) { AppNav(it) } } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable private fun AppNav(loggedIn: Boolean) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = if (loggedIn) "home" else "login") {
        composable("login") { LoginScreen(hiltViewModel()) { nav.navigate("home") { popUpTo("login") { inclusive = true } } } }
        composable("home") { HomeScreen(hiltViewModel(), onInspect = { nav.navigate("inspection") }, onLogout = { nav.navigate("login") { popUpTo("home") { inclusive = true } } }) }
        composable("inspection") { InspectionScreen(hiltViewModel(), onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() }) }
    }
}
