package com.rudra.realspeedtest.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.realspeedtest.ui.theme.*

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            onClick = onClick,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpeedGauge(
    speed: Double,
    maxSpeed: Double = 200.0,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val safeSpeed = speed.toFloat().takeIf { it.isFinite() } ?: 0f
    val animatedSpeed by animateFloatAsState(
        targetValue = safeSpeed,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "speed"
    )

    val sweepAngle = ((animatedSpeed / maxSpeed.toFloat()) * 240f).coerceIn(0f, 240f)

    val gaugeColor = when {
        animatedSpeed >= 100 -> ExcellentColor
        animatedSpeed >= 50 -> GoodColor
        animatedSpeed >= 25 -> FairColor
        animatedSpeed >= 10 -> PoorColor
        else -> BadColor
    }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val radius = (size.toPx() - strokeWidth - 20.dp.toPx()) / 2
            val center = Offset(size.toPx() / 2, size.toPx() / 2)

            drawArc(
                color = surfaceVariant,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            val progressBrush = Brush.sweepGradient(
                colors = listOf(gaugeColor.copy(alpha = 0.7f), gaugeColor),
                center = center
            )
            drawArc(
                brush = progressBrush,
                startAngle = 150f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.1f".format(animatedSpeed),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Mbps",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = gaugeColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = getSpeedLabel(animatedSpeed.toDouble()),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun MiniSpeedGauge(
    speed: Double,
    maxSpeed: Double = 200.0,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val sweepAngle = ((speed.toFloat() / maxSpeed.toFloat()) * 270f).coerceIn(0f, 270f)
    val gaugeColor = getSpeedColor(speed)

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2
            val center = Offset(size.toPx() / 2, size.toPx() / 2)

            drawArc(
                color = surfaceVariant,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        Text(
            text = "%.0f".format(speed),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getSpeedLabel(speed: Double): String = when {
    speed >= 100 -> "Excellent"
    speed >= 50 -> "Good"
    speed >= 25 -> "Fair"
    speed >= 10 -> "Poor"
    else -> "Slow"
}

private fun getSpeedColor(speed: Double): Color = when {
    speed >= 100 -> ExcellentColor
    speed >= 50 -> GoodColor
    speed >= 25 -> FairColor
    speed >= 10 -> PoorColor
    else -> BadColor
}

@Composable
fun StatCard(
    title: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    ModernCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedStatCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    color: Color
) {
    ModernCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ProgressIndicator(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
fun AlertBanner(
    title: String,
    message: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NetworkInfoCard(
    publicIP: String,
    ispName: String,
    connectionType: String,
    city: String? = null,
    country: String? = null,
    modifier: Modifier = Modifier
) {
    ModernCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "Public IP", value = publicIP)
                InfoItem(label = "Connection", value = connectionType)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "ISP", value = ispName)
                InfoItem(label = "Location", value = listOfNotNull(city, country).joinToString(", "))
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = containerColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
