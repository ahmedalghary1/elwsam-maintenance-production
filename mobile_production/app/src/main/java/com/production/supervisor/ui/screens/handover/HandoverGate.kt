package com.production.supervisor.ui.screens.handover

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.production.supervisor.ui.screens.home.ProductionHomeUiState
import com.production.supervisor.ui.screens.home.ProductionHomeViewModel

@Composable
fun HandoverGate(state: ProductionHomeUiState, viewModel: ProductionHomeViewModel) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("مراجعة التقرير السابق", style = MaterialTheme.typography.titleLarge)
        Text("يلزم التحقق من التقارير المسلّمة إليك قبل كتابة تقرير الإنتاج والأعطال.")
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it) }
        Button(onClick = { viewModel.loadData() }, enabled = !state.isLoading) { Text("إعادة المحاولة") }
    }
    state.pendingHandover?.let { report ->
        HandoverDialog(report, {}, { viewModel.confirmHandover(it) },
            onReject = { viewModel.confirmHandover(it, "REJECT") }, busy = state.isLoading, error = state.message)
    }
}
