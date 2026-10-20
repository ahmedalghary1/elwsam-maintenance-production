package com.elwsam.operations

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElwsamTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    if (!state.sessionLoaded) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (state.session == null) {
        UnifiedLoginScreen(state, viewModel)
    } else {
        ModulePicker(state.session!!, state.loading, viewModel::logout)
    }
}

@Composable
private fun UnifiedLoginScreen(state: AppUiState, viewModel: AppViewModel) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0D2A3B), Color(0xFF174D68), Color(0xFFF4F7F9)), endY = 1150f)
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(58.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = .13f)) {
                Icon(Icons.Outlined.Factory, null, Modifier.padding(18.dp).size(48.dp), tint = Color.White)
            }
            Spacer(Modifier.height(18.dp))
            Text("الوسام", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("للتشغيل والصيانة", color = Color(0xFFD8EAF3), fontSize = 17.sp)
            Spacer(Modifier.height(32.dp))
            ElevatedCard(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Text("تسجيل الدخول", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("استخدم حسابك المسجل في لوحة التحكم.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = viewModel::setPhone,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        enabled = !state.loading,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::setPassword,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePassword) {
                                Icon(if (state.passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "إظهار أو إخفاء كلمة المرور")
                            }
                        },
                        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !state.loading,
                        shape = RoundedCornerShape(14.dp)
                    )
                    state.error?.let {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Button(
                        onClick = viewModel::login,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.loading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(23.dp), strokeWidth = 3.dp, color = Color.White)
                        else {
                            Text("دخول", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.Login, null)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(17.dp), tint = Color(0xFF526570))
                Spacer(Modifier.width(6.dp))
                Text("تظهر لك الأنظمة المسموح بها فقط", color = Color(0xFF526570), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ModulePicker(session: UnifiedSession, loading: Boolean, onLogout: () -> Unit) {
    val context = LocalContext.current
    val maintenanceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == com.maintenance.supervisor.MainActivity.RESULT_LOGGED_OUT) onLogout()
    }
    val productionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == com.production.supervisor.MainActivity.RESULT_LOGGED_OUT) onLogout()
    }
    val canMaintain = session.role == "MAINTENANCE_SUPERVISOR" || session.role == "GENERAL_SUPERVISOR"
    val canProduce = session.role == "PRODUCTION_SUPERVISOR" || session.role == "GENERAL_SUPERVISOR" || session.role == "ADMIN"

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF0D2A3B), Color(0xFF1D607D)))
                ).padding(horizontal = 22.dp, vertical = 26.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f)) {
                            Icon(Icons.Outlined.Factory, null, Modifier.padding(12.dp).size(28.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الوسام للتشغيل والصيانة", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("منصة العمل الموحّدة", color = Color(0xFFD0E4ED), fontSize = 13.sp)
                        }
                        IconButton(onClick = onLogout, enabled = !loading) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, "تسجيل الخروج", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                    Text("مرحبًا، ${session.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("اختر النظام الذي تريد فتحه", color = Color(0xFFD0E4ED), fontSize = 15.sp)
                }
            }

            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (canMaintain) {
                    ModuleCard(
                        title = "الصيانة الدورية",
                        description = "فحص الماكينات، قوائم الصيانة والمزامنة التلقائية",
                        icon = Icons.Outlined.Build,
                        color = Color(0xFF16846F),
                        onClick = {
                            maintenanceLauncher.launch(
                                Intent(context, com.maintenance.supervisor.MainActivity::class.java)
                                    .putExtra(com.maintenance.supervisor.MainActivity.EXTRA_EMBEDDED, true)
                            )
                        }
                    )
                }
                if (canProduce) {
                    ModuleCard(
                        title = "الإنتاج والأعطال",
                        description = "تسجيل الوردية، إنتاج الماكينات، التوقفات والتسليم",
                        icon = Icons.Outlined.PrecisionManufacturing,
                        color = Color(0xFFF28C28),
                        onClick = {
                            productionLauncher.launch(
                                Intent(context, com.production.supervisor.MainActivity::class.java)
                                    .putExtra(com.production.supervisor.MainActivity.EXTRA_EMBEDDED, true)
                            )
                        }
                    )
                }
                if (!canMaintain && !canProduce) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp)) {
                        Text("لا توجد أنظمة متاحة لهذه الصلاحية.", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("تم تحديد الأنظمة المتاحة طبقًا لصلاحيتك في لوحة التحكم.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(34.dp), tint = color)
            }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                Spacer(Modifier.height(5.dp))
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            }
            Spacer(Modifier.width(8.dp))
            Surface(shape = CircleShape, color = color) {
                Icon(Icons.Outlined.ArrowBack, "فتح", Modifier.padding(9.dp).size(20.dp), tint = Color.White)
            }
        }
    }
}
