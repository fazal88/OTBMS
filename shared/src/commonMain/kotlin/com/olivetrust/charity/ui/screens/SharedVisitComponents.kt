package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.olivetrust.charity.domain.model.VerificationVisit
import com.olivetrust.charity.domain.model.VisitStatus
import com.olivetrust.charity.domain.util.LocationUtil
import com.olivetrust.charity.openMaps
import kotlinx.datetime.*

@Composable
fun VisitCard(visit: VerificationVisit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        formatVisitDate(visit.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("By: ${visit.employeeId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                VisitStatusBadge(visit.visitStatus)
            }
            
            visit.reapprovalReason?.let {
                Spacer(Modifier.height(8.dp))
                Text("Note: $it", style = MaterialTheme.typography.bodySmall)
            }
            visit.misuseReport?.let {
                Spacer(Modifier.height(4.dp))
                Text("Report: ${it.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            if (visit.latitude != 0.0 || visit.longitude != 0.0) {
                Spacer(Modifier.height(16.dp))
                LocationComparisonRow(visit)
            }
        }
    }
}

private fun formatVisitDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth}/${dateTime.month.number}/${dateTime.year}"
}

@Composable
fun LocationComparisonRow(visit: VerificationVisit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Registered Location (Beneficiary)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("Registered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            IconButton(
                onClick = { openMaps(visit.beneficiaryLatitude, visit.beneficiaryLongitude, "Registered: ${visit.beneficiaryName}") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            }
        }

        // Distance in Middle
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
            val isTooFar = visit.distanceInMeters > 500
            Text("Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                LocationUtil.formatDistance(visit.distanceInMeters),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isTooFar) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            if (isTooFar) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
            }
        }

        // Recorded Location (Visit)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("Recorded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            IconButton(
                onClick = { openMaps(visit.latitude, visit.longitude, "Visit: ${visit.beneficiaryName}") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun VisitStatusBadge(status: VisitStatus) {
    val (color, icon) = when (status) {
        VisitStatus.SUCCESSFUL -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
        VisitStatus.REAPPROVAL_REQUIRED -> Color(0xFFFF9800) to Icons.Default.Refresh
        VisitStatus.MISUSE_REPORTED -> MaterialTheme.colorScheme.error to Icons.Default.Warning
        VisitStatus.EDIT_REQUESTED -> Color(0xFF9C27B0) to Icons.Default.Edit
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
            Text(
                text = status.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
