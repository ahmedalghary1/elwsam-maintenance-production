package com.production.supervisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.production.supervisor.data.repository.AuthRepository
import com.production.supervisor.ui.navigation.ProductionNavGraph
import com.production.supervisor.ui.theme.ProductionSupervisorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isLoggedIn by authRepository.isLoggedInFlow.collectAsState(initial = false)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ProductionSupervisorTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ProductionNavGraph(isLoggedIn = isLoggedIn)
                    }
                }
            }
        }
    }
}
