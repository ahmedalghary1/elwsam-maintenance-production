package com.production.supervisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
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
        val embedded = intent.getBooleanExtra(EXTRA_EMBEDDED, false)

        setContent {
            val isLoggedIn by produceState<Boolean?>(null, authRepository) {
                authRepository.isLoggedInFlow.collect { value = it }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ProductionSupervisorTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        when (val active = isLoggedIn) {
                            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            false -> if (embedded) {
                                LaunchedEffect(Unit) {
                                    setResult(RESULT_LOGGED_OUT)
                                    finish()
                                }
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            } else key(active) { ProductionNavGraph(isLoggedIn = false) }
                            true -> key(active) {
                            ProductionNavGraph(
                                isLoggedIn = true,
                                onEmbeddedLogout = {
                                    setResult(RESULT_LOGGED_OUT)
                                    finish()
                                }
                            )
                            }
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
