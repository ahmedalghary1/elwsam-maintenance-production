package com.elwsam.operations.updates

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.elwsam.operations.ElwsamTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.Lifecycle

@AndroidEntryPoint
class UpdateActivity : ComponentActivity() {
    @Inject lateinit var updates: UpdateCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updates.check(force = true)
        setContent {
            val state by updates.state.collectAsState()
            LaunchedEffect(state.stage) {
                if (state.stage == "ready") updates.continueInstall()
                if (state.stage == "confirmation") openConfirmation()
            }
            val busy = state.stage in setOf("downloading", "ready", "installing")
            BackHandler(busy) { }
            BackHandler(!busy) { updates.deferCurrent(); finish() }
            ElwsamTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            Text(if (busy) "جارٍ تحديث التطبيق حاليًا" else when (state.stage) {
                                "permission", "confirmation" -> "التحديث جاهز للتثبيت"
                                "failed", "incompatible" -> "لم يكتمل التحديث"
                                else -> "تحديث التطبيق"
                            }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            if (state.version.isNotBlank()) Text("الإصدار ${state.version}", Modifier.padding(top = 12.dp))
                            Spacer(Modifier.height(28.dp))
                            if (state.stage == "downloading") {
                                LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
                                Text("تم تنزيل ${state.progress}٪", Modifier.padding(top = 12.dp))
                            } else if (busy) CircularProgressIndicator()
                            Spacer(Modifier.height(20.dp))
                            Text(when (state.stage) {
                                "downloading" -> "يرجى عدم إغلاق الهاتف أو قطع الإنترنت حتى يكتمل تنزيل التحديث."
                                "ready", "installing" -> "جارٍ التحقق والتثبيت. تقاريرك وبياناتك المحفوظة ستبقى موجودة. قد يُغلق Android التطبيق أثناء التثبيت."
                                "permission" -> "يحتاج Android السماح للتطبيق بتثبيت تحديثاته. فعّل «السماح من هذا المصدر» ثم ارجع إلى التطبيق."
                                "confirmation" -> "أكمل موافقة Android على التثبيت. إذا أغلقت نافذة النظام يمكنك المحاولة مجددًا."
                                else -> state.message.ifBlank { "جارٍ التحقق من وجود نسخة أحدث…" }
                            }, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(28.dp))
                            if (state.stage == "permission") {
                                Button(onClick = {
                                    if (Build.VERSION.SDK_INT >= 26) {
                                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                                    }
                                }) { Text("السماح بتثبيت التحديثات") }
                            }
                            if (state.stage in setOf("failed", "confirmation")) {
                                Button(onClick = { updates.check(force = true) }) { Text("إعادة المحاولة") }
                            }
                            if (!busy) TextButton(onClick = { updates.deferCurrent(); finish() }) { Text("العودة إلى التطبيق") }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (updates.state.value.stage == "permission") updates.continueInstall()
        if (updates.state.value.stage == "confirmation") openConfirmation()
    }

    private fun openConfirmation() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            updates.takeConfirmation()?.let { runCatching { startActivity(it) } }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (updates.state.value.stage == "confirmation") openConfirmation()
    }
}
