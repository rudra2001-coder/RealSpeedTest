package com.rudra.realspeedtest.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.realspeedtest.data.model.QualityLabel
import com.rudra.realspeedtest.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.White),
    elevation: Dp = 3.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val finalModifier = modifier.shadow(elevation + 2.dp, shape, clip = false)

    if (onClick != null) {
        Card(
            modifier = finalModifier,
            colors = colors,
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            onClick = onClick
        ) { content() }
    } else {
        Card(
            modifier = finalModifier,
            colors = colors,
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) { content() }
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
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "speed"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gauge_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val sweepAngle = (animatedSpeed / maxSpeed.toFloat() * 270f).coerceIn(0f, 270f)

    val gaugeColor = when {
        animatedSpeed >= 100 -> ExcellentColor
        animatedSpeed >= 50 -> GoodColor
        animatedSpeed >= 25 -> FairColor
        animatedSpeed >= 10 -> PoorColor
        else -> BadColor
    }

    val gradientColors = listOf(
        LightBackground,
        Color.White,
        gaugeColor.copy(alpha = 0.05f),
        Color.White
    )

    val animatedGradient by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    Box(
        modifier = modifier
            .size(size)
            .padding(8.dp)
            .background(
                Brush.sweepGradient(
                    colors = listOf(
                        gaugeColor.copy(alpha = 0.08f),
                        Color.Transparent,
                        gaugeColor.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(28.dp)
            )
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .background(Color.White, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 22.dp.toPx()
            val radius = (size.toPx() - strokeWidth - 40.dp.toPx()) / 2
            val center = Offset(size.toPx() / 2 - 14.dp.toPx(), size.toPx() / 2 - 14.dp.toPx())

            // Outer glow ring
            drawArc(
                color = gaugeColor.copy(alpha = glowAlpha * 0.3f),
                startAngle = 135f,
                sweepAngle = sweepAngle.coerceAtLeast(2f),
                useCenter = false,
                style = Stroke(width = strokeWidth + 12.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Track background
            drawArc(
                color = Gray100,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Active arc with gradient
            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Glow highlight on the arc
            if (sweepAngle > 2f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.4f),
                    startAngle = 135f,
                    sweepAngle = (sweepAngle * 0.3f).coerceAtMost(30f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth * 0.4f, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }

            // Elegant tick marks
            val tickCount = 9
            for (i in 0..tickCount) {
                val tickAngle = 135.0 + (270.0 / tickCount) * i
                val tickAngleRad = Math.toRadians(tickAngle)
                val tickInner = radius - strokeWidth / 2 + 4.dp.toPx()
                val tickOuter = radius + strokeWidth / 2 - 4.dp.toPx()
                val isReached = sweepAngle >= (270f / tickCount) * i
                drawLine(
                    color = if (isReached && sweepAngle > 0) gaugeColor.copy(alpha = 0.5f) else Gray200,
                    start = Offset(
                        center.x + tickInner * cos(tickAngleRad).toFloat(),
                        center.y + tickInner * sin(tickAngleRad).toFloat()
                    ),
                    end = Offset(
                        center.x + tickOuter * cos(tickAngleRad).toFloat(),
                        center.y + tickOuter * sin(tickAngleRad).toFloat()
                    ),
                    strokeWidth = if (isReached) 3.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", animatedSpeed),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Text(
                text = "Mbps",
                fontSize = 14.sp,
                color = Gray500,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = gaugeColor,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = getSpeedLabel(animatedSpeed.toDouble()),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2
            val center = Offset(size.toPx() / 2, size.toPx() / 2)

            // Background track with shadow effect
            drawArc(
                color = Gray100,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Active arc
            drawArc(
                color = gaugeColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Highlight
            if (sweepAngle > 2f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.5f),
                    startAngle = 135f,
                    sweepAngle = (sweepAngle * 0.3f).coerceAtMost(20f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth * 0.5f, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.0f", speed),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = gaugeColor
            )
        }
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
fun ISPScoreCard(
    score: Int,
    label: QualityLabel,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (label) {
        QualityLabel.EXCELLENT -> ExcellentColor
        QualityLabel.GOOD -> GoodColor
        QualityLabel.FAIR -> FairColor
        QualityLabel.POOR -> PoorColor
        QualityLabel.BAD -> BadColor
        QualityLabel.UNKNOWN -> Gray400
    }

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Internet Quality",
                style = MaterialTheme.typography.titleMedium,
                color = Gray600,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(backgroundColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = backgroundColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = backgroundColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ISP Score out of 100",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
    }
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 14.sp,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 4.dp)
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
    subtitle: String = "",
    modifier: Modifier = Modifier,
    icon: ImageVector,
    color: Color,
    trend: String? = null
) {
    ModernCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 14.sp,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
            }
            if (trend != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = trend,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (trend.startsWith("+")) GoodColor else if (trend.startsWith("-")) BadColor else Gray500,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(GaugeBackground, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
fun CDNResultItem(
    name: String,
    speed: Double,
    status: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        "DONE" -> ExcellentColor
        "TESTING" -> FairColor
        "FAILED" -> BadColor
        else -> Gray400
    }

    ModernCard(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status == "DONE") ExcellentColor.copy(alpha = 0.08f) else Gray100
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (status) {
                            "DONE" -> "Completed"
                            "TESTING" -> "Testing..."
                            "FAILED" -> "Failed"
                            else -> "Pending"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }
            if (status == "DONE") {
                Text(
                    text = String.format("%.1f", speed),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = getSpeedColor(speed)
                )
                Text(
                    text = " Mbps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            } else if (status == "TESTING") {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun CDNResultCard(
    name: String,
    speed: Double,
    latency: Double,
    status: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        "DONE" -> ExcellentColor
        "TESTING" -> FairColor
        "FAILED" -> BadColor
        else -> Gray400
    }

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Column {
                        Text(
                            text = "Speed",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                        Text(
                            text = String.format("%.1f Mbps", speed),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (status == "DONE") getSpeedColor(speed) else Gray500
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column {
                        Text(
                            text = "Latency",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                        Text(
                            text = String.format("%.0f ms", latency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (status == "DONE") {
                MiniSpeedGauge(speed = speed, size = 70.dp)
            }
        }
    }
}

@Composable
fun AnimatedProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 8.dp.toPx()
        val radius = (size.toPx() - strokeWidth) / 2

        drawArc(
            color = Color.White.copy(alpha = alpha),
            startAngle = 0f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray700
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
    modifier: Modifier = Modifier,
    city: String = "",
    country: String = ""
) {
    ModernCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Teal500,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Network Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItemModern(label = "Public IP", value = publicIP, modifier = Modifier.weight(1f))
                InfoItemModern(label = "Connection", value = connectionType, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val location = buildString {
                    if (city.isNotEmpty()) append(city)
                    if (city.isNotEmpty() && country.isNotEmpty()) append(", ")
                    if (country.isNotEmpty()) append(country)
                }
                InfoItemModern(
                    label = "Location",
                    value = location.ifEmpty { "Unknown" },
                    modifier = Modifier.weight(1f)
                )
                InfoItemModern(label = "Connection", value = connectionType, modifier = Modifier.weight(1f))
            }
            if (ispName.isNotEmpty() && ispName != "Unknown") {
                Spacer(modifier = Modifier.height(12.dp))
                InfoItemModern(label = "ISP", value = ispName, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun InfoItemModern(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Gray500,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HistoryLineChart(
    data: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = Blue700,
    title: String = "Download Speed",
    unit: String = "Mbps"
) {
    if (data.isEmpty()) {
        ModernCard(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
            }
        }
        return
    }

    val maxValue = data.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val minValue = 0.0

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${data.size} tests",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val padding = 8.dp.toPx()

                    if (data.size < 2) return@Canvas

                    val stepX = (width - padding * 2) / (data.size - 1).coerceAtLeast(1)

                    val path = Path()
                    data.forEachIndexed { index, (_, value) ->
                        val x = padding + index * stepX
                        val normalizedValue = (value - minValue) / (maxValue - minValue)
                        val y = height - padding - (normalizedValue * (height - padding * 2)).toFloat()

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    data.forEachIndexed { index, (_, value) ->
                        val x = padding + index * stepX
                        val normalizedValue = (value - minValue) / (maxValue - minValue)
                        val y = height - padding - (normalizedValue * (height - padding * 2)).toFloat()

                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0 $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray500
                )
                Text(
                    text = "${String.format("%.0f", maxValue)} $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray500
                )
            }
        }
    }
}

@Composable
fun SpeedComparisonCard(
    currentSpeed: Double,
    averageSpeed: Double,
    modifier: Modifier = Modifier
) {
    val difference = if (averageSpeed > 0) ((currentSpeed - averageSpeed) / averageSpeed * 100) else 0.0
    val isFaster = difference > 0

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "vs. Your Average",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray600
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Last 30 tests",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isFaster) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isFaster) Green700 else Red500,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${if (isFaster) "+" else ""}${String.format("%.1f", difference)}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isFaster) Green700 else Red500
                )
            }
        }
    }
}

@Composable
fun BandwidthDistributionChart(
    cdnResults: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (cdnResults.isEmpty()) return

    val total = cdnResults.sumOf { it.second }
    if (total <= 0) return

    val colors = listOf(Blue700, Green700, Orange700, Purple700, Teal700, Red700, Pink700, Indigo700)

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bandwidth Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                var offset = 0f
                cdnResults.forEachIndexed { index, (_, speed) ->
                    val fraction = (speed / total).toFloat()
                    if (fraction > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .offset(x = (offset * 100).dp)
                                .background(colors[index % colors.size])
                        )
                        offset += fraction
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            cdnResults.forEachIndexed { index, (name, speed) ->
                val percentage = (speed / total * 100)
                if (percentage > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(colors[index % colors.size], RoundedCornerShape(3.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "${String.format("%.1f", percentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors[index % colors.size]
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ISPScoreBreakdown(
    downloadScore: Int,
    latencyScore: Int,
    jitterScore: Int,
    packetLossScore: Int,
    modifier: Modifier = Modifier
) {
    val total = downloadScore + latencyScore + jitterScore + packetLossScore
    val maxScore = 40 + 25 + 20 + 15

    ModernCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ISP Score Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            ScoreBreakdownItem(
                label = "Download Speed",
                score = downloadScore,
                maxScore = 40,
                color = Blue700
            )
            ScoreBreakdownItem(
                label = "Latency",
                score = latencyScore,
                maxScore = 25,
                color = Purple700
            )
            ScoreBreakdownItem(
                label = "Connection Stability",
                score = jitterScore,
                maxScore = 20,
                color = Orange700
            )
            ScoreBreakdownItem(
                label = "Connection Reliability",
                score = packetLossScore,
                maxScore = 15,
                color = Red700
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Gray200)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Score",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$total/$maxScore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        total >= 80 -> Green700
                        total >= 60 -> Orange700
                        else -> Red500
                    }
                )
            }
        }
    }
}

@Composable
private fun ScoreBreakdownItem(
    label: String,
    score: Int,
    maxScore: Int,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Gray600
            )
            Text(
                text = "$score/$maxScore",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Gray200, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(score.toFloat() / maxScore)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}