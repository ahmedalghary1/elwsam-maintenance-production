package com.production.supervisor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.production.supervisor.data.local.entity.ProductionOptionEntity

@Composable
fun ProductionOptionSelector(
    label: String,
    options: List<ProductionOptionEntity>,
    selected: ProductionOptionEntity?,
    onSelected: (ProductionOptionEntity?) -> Unit,
    modifier: Modifier = Modifier,
    allowNone: Boolean = true,
    noneLabel: String = "بدون تحديد"
) {
    var expanded by remember(label) { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(5.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selected?.name ?: noneLabel, maxLines = 1)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (allowNone) {
                    DropdownMenuItem(text = { Text(noneLabel) }, onClick = { onSelected(null); expanded = false })
                }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = { onSelected(option); expanded = false }
                    )
                }
            }
        }
    }
}
