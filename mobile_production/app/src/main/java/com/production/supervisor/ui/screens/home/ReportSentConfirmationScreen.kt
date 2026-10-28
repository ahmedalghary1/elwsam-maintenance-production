package com.production.supervisor.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReportSentConfirmationScreen(onContinue: () -> Unit) {
    BackHandler(onBack = onContinue)
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF0FDF4)) {
        Box(Modifier.fillMaxSize().navigationBarsPadding().padding(24.dp)) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF15803D),
                    modifier = Modifier.size(104.dp)
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "تم إرسال التقرير بنجاح",
                    color = Color(0xFF14532D),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "وصل تقرير الوردية إلى الإدارة.",
                    color = Color(0xFF166534),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp)
            ) {
                Text("متابعة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
