package com.tyto.codepilot.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Tyto palette: obsidian ground, electric crimson accent, stark white type. */
object Tyto {
    val Obsidian = Color(0xFF07070A)
    val Panel = Color(0xFF0E0E14)
    val Line = Color(0xFF22222C)
    val Crimson = Color(0xFFFF1D4E)
    val CrimsonDim = Color(0xFF7A0F2A)
    val White = Color(0xFFF4F4F6)
    val Mute = Color(0xFF8B8B98)
}

val Sharp = RoundedCornerShape(2.dp)

private val TytoColors = darkColorScheme(
    primary = Tyto.Crimson,
    onPrimary = Tyto.Obsidian,
    secondary = Tyto.Crimson,
    onSecondary = Tyto.Obsidian,
    background = Tyto.Obsidian,
    onBackground = Tyto.White,
    surface = Tyto.Panel,
    onSurface = Tyto.White,
    surfaceVariant = Tyto.Panel,
    onSurfaceVariant = Tyto.Mute,
    outline = Tyto.Line,
    outlineVariant = Tyto.Line,
)

private val TytoTypography = Typography().let {
    it.copy(
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
        titleSmall = it.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp),
        labelMedium = it.labelMedium.copy(letterSpacing = 1.2.sp),
    )
}

@Composable
fun TytoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TytoColors,
        typography = TytoTypography,
        shapes = Shapes(
            extraSmall = Sharp,
            small = Sharp,
            medium = Sharp,
            large = Sharp,
            extraLarge = Sharp,
        ),
        content = content,
    )
}

/** Pitch-black base, faint tech grid and a crimson glow near the top. */
fun Modifier.tytoBackground(): Modifier = drawBehind {
    drawRect(Tyto.Obsidian)
    drawRect(
        Brush.radialGradient(
            colors = listOf(Tyto.Crimson.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.10f),
            radius = size.width * 0.95f,
        )
    )
    val step = 28.dp.toPx()
    val gridColor = Color.White.copy(alpha = 0.035f)
    var x = 0f
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}

/** The geometric owl emblem, drawn on a 64x64 grid. */
@Composable
fun OwlMark(
    modifier: Modifier = Modifier,
    color: Color = Tyto.White,
    pupil: Color = Tyto.Crimson,
) {
    Canvas(modifier) {
        val s = size.minDimension / 64f

        fun poly(vararg pts: Float): Path = Path().apply {
            moveTo(pts[0] * s, pts[1] * s)
            for (i in 2 until pts.size step 2) lineTo(pts[i] * s, pts[i + 1] * s)
            close()
        }

        drawPath(
            poly(32f, 6f, 48f, 14f, 54f, 30f, 50f, 52f, 32f, 60f, 14f, 52f, 10f, 30f, 16f, 14f),
            color,
            style = Stroke(width = 2.5f * s, join = StrokeJoin.Miter),
        )
        drawPath(poly(16f, 14f, 20f, 3f, 27f, 12f), color)
        drawPath(poly(48f, 14f, 44f, 3f, 37f, 12f), color)
        for (cx in listOf(23f, 41f)) {
            drawCircle(color, 7f * s, Offset(cx * s, 28f * s), style = Stroke(width = 2f * s))
            drawCircle(pupil, 3f * s, Offset(cx * s, 28f * s))
        }
        drawPath(poly(29f, 36f, 35f, 36f, 32f, 42f), color)
        drawLine(color, Offset(10f * s, 30f * s), Offset(16f * s, 30f * s), 1.5f * s)
        drawLine(color, Offset(54f * s, 30f * s), Offset(48f * s, 30f * s), 1.5f * s)
    }
}

/** Crimson status dot with an expanding neon ring. */
@Composable
fun PulseDot(dot: Dp = 7.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)),
        label = "ring",
    )
    Canvas(Modifier.size(dot * 2.4f)) {
        val r = dot.toPx() / 2f
        drawCircle(Tyto.Crimson.copy(alpha = (1f - ring) * 0.6f), r + ring * dot.toPx(), center)
        drawCircle(Tyto.Crimson, r, center)
    }
}

/** Three crimson ticks that blink in sequence while the agent works. */
@Composable
fun ThinkingTicks() {
    val transition = rememberInfiniteTransition(label = "think")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse, StartOffset(i * 180)),
                label = "tick$i",
            )
            Box(Modifier.size(6.dp).background(Tyto.Crimson.copy(alpha = alpha)))
        }
    }
}

/** Launch screen: owl with a laser line sweeping down it. */
@Composable
fun TytoSplash(onDone: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "splash")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing)),
        label = "sweep",
    )
    LaunchedEffect(Unit) {
        delay(1500)
        onDone()
    }
    Box(Modifier.fillMaxSize().tytoBackground(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(Modifier.size(120.dp)) {
                OwlMark(Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height * sweep
                    drawLine(Tyto.Crimson, Offset(-16f, y), Offset(size.width + 16f, y), 3f)
                }
            }
            Text("TYTO", color = Tyto.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 12.sp)
            Text("CODE FROM YOUR POCKET", color = Tyto.Mute, fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}
