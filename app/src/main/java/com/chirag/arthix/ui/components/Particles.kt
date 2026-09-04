package com.chirag.arthix.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiBurst(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF34A853), Color(0xFFFFC107), Color(0xFFE4463A), Color(0xFF4285F4)),
    particleCount: Int = 40,
    isRunning: Boolean = false,
    onComplete: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }
    val particles = remember { mutableStateListOf<ConfettiParticle>() }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            progress.snapTo(0f)
            particles.clear()
            for (i in 0 until particleCount) {
                val angle = Random.nextDouble(0.0, Math.PI * 2)
                val speed = Random.nextDouble(10.0, 40.0)
                particles.add(
                    ConfettiParticle(
                        x = 0f,
                        y = 0f,
                        velocityX = (cos(angle) * speed).toFloat(),
                        velocityY = (sin(angle) * speed).toFloat() - 20f,
                        color = colors.random(),
                        size = Random.nextFloat() * 12f + 8f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = Random.nextFloat() * 20f - 10f
                    )
                )
            }
            progress.animateTo(1f, animationSpec = tween(2000, easing = LinearOutSlowInEasing))
            onComplete()
        }
    }

    if (isRunning) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val p = progress.value
            val gravity = p * 600f // Fall down over time
            val spread = p * 20f

            particles.forEach { particle ->
                val px = cx + particle.x + (particle.velocityX * spread)
                val py = cy + particle.y + (particle.velocityY * spread) + gravity
                val alpha = (1f - p).coerceIn(0f, 1f)

                rotate(degrees = particle.rotation + (particle.rotationSpeed * spread), pivot = Offset(px, py)) {
                    if (particle.size.toInt() % 2 == 0) {
                        drawRect(
                            color = particle.color.copy(alpha = alpha),
                            topLeft = Offset(px, py),
                            size = Size(particle.size, particle.size / 2)
                        )
                    } else {
                        drawCircle(
                            color = particle.color.copy(alpha = alpha),
                            radius = particle.size / 2f,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}

data class Ember(
    var startX: Float,
    var startY: Float,
    var speed: Float,
    var size: Float,
    var delay: Float
)

@Composable
fun FloatingEmbers(modifier: Modifier = Modifier, color: Color) {
    val transition = rememberInfiniteTransition(label = "embers")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing)),
        label = "time"
    )

    val embers = remember {
        List(15) {
            Ember(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                speed = Random.nextFloat() * 0.5f + 0.5f,
                size = Random.nextFloat() * 6f + 2f,
                delay = Random.nextFloat() * 1000f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        embers.forEach { ember ->
            val emberTime = (time + ember.delay) % 1000f
            val progress = emberTime / 1000f // 0 to 1
            
            val y = h - (progress * h * ember.speed)
            val x = (ember.startX * w) + sin(progress * Math.PI * 4).toFloat() * 20f
            
            // Fade in and out
            val alpha = if (progress < 0.2f) {
                progress / 0.2f
            } else if (progress > 0.8f) {
                (1f - progress) / 0.2f
            } else {
                1f
            }

            drawCircle(
                color = color.copy(alpha = alpha * 0.6f),
                radius = ember.size,
                center = Offset(x, y)
            )
        }
    }
}
