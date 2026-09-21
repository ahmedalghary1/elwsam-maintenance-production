package com.maintenance.supervisor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.maintenance.supervisor.ui.LoginViewModel

@Composable fun LoginScreen(vm: LoginViewModel, onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.done) { if (state.done) onDone() }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background), endY = 900f)
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondary,
                shadowElevation = 12.dp
            ) { Icon(Icons.Outlined.Build, null, Modifier.padding(18.dp).size(38.dp), tint = MaterialTheme.colorScheme.onSecondary) }
            Spacer(Modifier.height(18.dp))
            Text("إدارة الصيانة", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
            Text("الفحص الدوري للمصانع", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primaryContainer)
            Spacer(Modifier.height(28.dp))

            Card(
                Modifier.fillMaxWidth().widthIn(max = 480.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("مرحبًا بعودتك", style = MaterialTheme.typography.titleLarge)
                    Text("سجّل الدخول لبدء فحص اليوم.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(22.dp))
                    OutlinedTextField(
                        state.phone, vm::phone, Modifier.fillMaxWidth(),
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        enabled = !state.loading
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        state.password, vm::password, Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور")
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { vm.submit() }),
                        enabled = !state.loading
                    )
                    state.error?.let {
                        Surface(Modifier.fillMaxWidth().padding(top = 14.dp), color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp)); Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(vm::submit, Modifier.fillMaxWidth().height(58.dp), enabled = !state.loading) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else { Text("تسجيل الدخول", fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null) }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Security, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp)); Text("بياناتك محفوظة بأمان على الجهاز", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
