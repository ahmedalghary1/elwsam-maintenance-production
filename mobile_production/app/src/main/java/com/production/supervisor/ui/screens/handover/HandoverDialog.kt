package com.production.supervisor.ui.screens.handover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.production.supervisor.data.remote.dto.ShiftReportDto
import com.production.supervisor.ui.theme.*

@Composable
fun HandoverDialog(
    pendingReport: ShiftReportDto,
    onDismiss: () -> Unit,
    onConfirm: (notes: String) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تأكيد استلام الوردية السابقة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FactoryDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "وردية: ${pendingReport.shiftDisplay} (${pendingReport.reportDate})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FactoryDark
                )
                Text(
                    text = "المشرف المسلّم: ${pendingReport.supervisorName}",
                    fontSize = 13.sp,
                    color = FactoryTextSecondary
                )

                if (pendingReport.generalNotes.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FactorySurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ملاحظات المشرف المسلّم:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(pendingReport.generalNotes, fontSize = 12.sp, color = FactoryTextPrimary)
                        }
                    }
                }

                Text(
                    text = "بصفتك المشرف المستلم، يرجى التأكد من تسليم كافة الماكينات بحالة تشغيلية سليمة.",
                    fontSize = 13.sp,
                    color = FactoryTextPrimary
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الاستلام (اختياري)") },
                    leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null, tint = FactoryNavy) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes) },
                colors = ButtonDefaults.buttonColors(containerColor = FactoryGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأكيد استلام الوردية", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
