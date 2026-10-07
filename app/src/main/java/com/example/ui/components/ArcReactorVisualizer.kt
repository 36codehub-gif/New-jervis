package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceState
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    voiceState: VoiceState,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for outer glow
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactor_rotation"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    val activeAudioScale = if (voiceState == VoiceState.LISTENING) {
        1f + (rmsLevel.coerceIn(0f, 15f) / 15f) * 0.25f
    } else {
        1f
    }

    val primaryColor = when (voiceState) {
        VoiceState.LISTENING -> JarvisCyanPrimary
        VoiceState.PROCESSING, VoiceState.EXECUTING -> JarvisAmberAccent
        VoiceState.SPEAKING -> JarvisSuccessGreen
        VoiceState.ERROR -> JarvisErrorRed
        VoiceState.IDLE -> JarvisCyanSecondary
    }

    Box(
        modifier = modifier
            .size(220.dp)
            .scale(if (voiceState == VoiceState.LISTENING) activeAudioScale else pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Holographic Concentric Rings Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // Outer boundary glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // Segmented telemetry ring
            val numSegments = 12
            val segmentAngle = 360f / numSegments
            for (i in 0 until numSegments) {
                val startAngle = i * segmentAngle + (rotationAngle % 360)
                drawArc(
                    color = primaryColor.copy(alpha = 0.6f),
                    startAngle = startAngle,
                    sweepAngle = segmentAngle * 0.5f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner counter rotating ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.4f),
                radius = maxRadius * 0.72f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Core tech tick marks
            for (deg in 0 until 360 step 30) {
                val rad = Math.toRadians((deg + counterRotation).toDouble())
                val p1 = Offset(
                    center.x + (maxRadius * 0.65f * cos(rad)).toFloat(),
                    center.y + (maxRadius * 0.65f * sin(rad)).toFloat()
                )
                val p2 = Offset(
                    center.x + (maxRadius * 0.72f * cos(rad)).toFloat(),
                    center.y + (maxRadius * 0.72f * sin(rad)).toFloat()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.8f),
                    start = p1,
                    end = p2,
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Center Tactical Mic Orb
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.85f),
                            JarvisBackgroundDark.copy(alpha = 0.95f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .testTag("mic_orb_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (voiceState) {
                    VoiceState.LISTENING -> Icons.Default.Mic
                    VoiceState.SPEAKING -> Icons.Default.SmartToy
                    else -> Icons.Default.Mic
                },
                contentDescription = "Activate Jarvis Voice Assistant",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}
