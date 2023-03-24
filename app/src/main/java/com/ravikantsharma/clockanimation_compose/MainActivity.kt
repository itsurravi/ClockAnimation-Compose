package com.ravikantsharma.clockanimation_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravikantsharma.clockanimation_compose.ui.theme.ClockAnimationComposeTheme
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClockAnimationComposeTheme {

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShrinkAndExtendArrowPreview() {
    ClockAnimationComposeTheme {
        val size = 300.dp
        Box(
            modifier = Modifier
                .size(size)
                .background(Color.Black)
                .padding(16.dp)
        ) {
            ClockAnimation(duration = 12000)
        }
    }
}

@Composable
fun ClockAnimation(duration: Int) {
    val infiniteTransition = rememberInfiniteTransition()

    // Creates a child animation of float type as a part of the [InfiniteTransition].
    val animationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    var currentHour by remember { mutableStateOf(0) }
    val disassembleDuration = duration / 6

    val hours: List<Int> = remember { List(12) { it } }
    val dotsVisibility = remember(currentHour) {
        hours.map { index ->
            when {
                index > currentHour -> false
                index > currentHour - 12 -> true
                else -> false
            }
        }
    }

    // Start calculation each time the animationAngle changes.
    val assembleValue = remember(animationAngle) {
        // We only need this animation for second rotation
        if (animationAngle >= 360) {
            // Reversed linear interpolation between 0..30 degrees, transformed into 0..1
            (animationAngle % 30) / 30
        } else -1f
    }

    val dotsPositions = remember(animationAngle) {
        List(12) { currentDot ->
            angleToFraction(
                angle = animationAngle,
                startAngle = currentDot * 30f,
                degreeLimit = 60f,
                easing = LinearOutSlowInEasing
            )
        }
    }

    val disassembleAnimations = remember { hours.map { Animatable(1f) } }

    val currentHourChannel = remember { Channel<Int>(12, BufferOverflow.DROP_OLDEST) }
    val currentHourFlow = remember(currentHourChannel) { currentHourChannel.receiveAsFlow() }

    LaunchedEffect(key1 = animationAngle) {
        // Add hour calculation inside of a launchEffect
        val newCurrentHour = animationAngle.toInt() / 30

        if (newCurrentHour != currentHour) {
            currentHour = newCurrentHour
            // Sending currentHour through channel
            currentHourChannel.trySend(currentHour)
        }
    }

    LaunchedEffect(key1 = currentHourFlow) {
        currentHourFlow.collectLatest {
            // launch each animation asynchronously
            launch {
                if (currentHour < 12) {
                    disassembleAnimations[currentHour].snapTo(0f)

                    disassembleAnimations[currentHour].animateTo(
                        1f,
                        tween(disassembleDuration, easing = LinearOutSlowInEasing)
                    )
                }
            }
        }
    }

    var strokeWidth by remember { mutableStateOf(0f) }

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            // Set strokeWidth based on the size of the viewport
            .onGloballyPositioned {
                strokeWidth = (it.size.width / 24).toFloat()
            }
            .drawBehind {
                val halfStroke = strokeWidth / 2
                val stepHeight = size.height / 24

                val center = Offset(size.width / 2, size.height / 2)
                val endOffset = Offset(
                    size.width / 2,
                    (size.height / 2) - calculateClockHandLength(size.height / 2, currentHour)
                )

                // Rotate the line around the pivot point, which is the
                // center of the screen. Rotation goes from 0 to 720 degrees
                rotate(animationAngle, pivot = center) {
                    // Drawing a clock hand itself
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = endOffset,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                        blendMode = BlendMode.DstOut
                    )

                    // Drawing a clock hand
                    if (assembleValue != -1f) {
                        val positionY = halfStroke + calculateAssembleDistance(
                            stepHeight = stepHeight,
                            currentHour = currentHour
                        ) * assembleValue

                        val start = Offset(size.width / 2, positionY)
                        val end = Offset(size.width / 2, positionY + halfStroke)

                        drawLine(
                            color = Color.White,
                            start = start,
                            end = end,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                            blendMode = BlendMode.DstOut
                        )
                    }
                }

                hours.forEach {
                    if (!dotsVisibility[it]) return@forEach
                    val degree = it * 30f

                    rotate(degree, pivot = center) {
                        // Based on the hour value, the travel distance will be longer.
                        val positionY =
                            halfStroke + stepHeight * it * (1 - dotsPositions[it])

                        val start = Offset(size.width / 2, positionY)
                        val end = Offset(size.width / 2, positionY + halfStroke)
                        drawLine(
                            color = Color.White,
                            start = start,
                            end = end,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                            blendMode = BlendMode.DstOut
                        )
                    }
                }
                rotate(animationAngle / 2, pivot = center) {
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta
                            )
                        ),
                        blendMode = BlendMode.DstAtop
                    )
                }
            }
    )
}

//@Preview(showBackground = true)
@Composable
fun ShrinkAndExtendArrowWithSliderPreview() {
    ClockAnimationComposeTheme {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val size = 300.dp
            var progress by remember { mutableStateOf(0f) }
            var animationAngle by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .size(size)
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                ClockAnimationWithSlide(animationAngle)
            }
            Text("Control animation with a slider!")
            Slider(
                modifier = Modifier.padding(16.dp),
                value = progress,
                onValueChange = {
                    progress = it
                    animationAngle = it * 720f
                }
            )
        }
    }
}

@Composable
fun ClockAnimationWithSlide(animationAngle: Float) {
    val infiniteTransition = rememberInfiniteTransition()

    /*// Creates a child animation of float type as a part of the [InfiniteTransition].
    val animationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )*/

    var currentHour by remember { mutableStateOf(0) }
//    val disassembleDuration = duration / 6

    val hours: List<Int> = remember { List(12) { it } }
    val dotsVisibility = remember(currentHour) {
        hours.map { index ->
            when {
                index > currentHour -> false
                index > currentHour - 12 -> true
                else -> false
            }
        }
    }

    // Start calculation each time the animationAngle changes.
    val assembleValue = remember(animationAngle) {
        // We only need this animation for second rotation
        if (animationAngle >= 360) {
            // Reversed linear interpolation between 0..30 degrees, transformed into 0..1
            (animationAngle % 30) / 30
        } else -1f
    }

    val dotsPositions = remember(animationAngle) {
        List(12) { currentDot ->
            angleToFraction(
                angle = animationAngle,
                startAngle = currentDot * 30f,
                degreeLimit = 60f,
                easing = LinearOutSlowInEasing
            )
        }
    }

    /*val disassembleAnimations = remember { hours.map { Animatable(1f) } }

    val currentHourChannel = remember { Channel<Int>(12, BufferOverflow.DROP_OLDEST) }
    val currentHourFlow = remember(currentHourChannel) { currentHourChannel.receiveAsFlow() }*/

    LaunchedEffect(key1 = animationAngle) {
        // Add hour calculation inside of a launchEffect
        val newCurrentHour = animationAngle.toInt() / 30

        if (newCurrentHour != currentHour) {
            currentHour = newCurrentHour
            // Sending currentHour through channel
//            currentHourChannel.trySend(currentHour)
        }
    }

    /*LaunchedEffect(key1 = currentHourFlow) {
        currentHourFlow.collectLatest {
            // launch each animation asynchronously
            launch {
                if (currentHour < 12) {
                    disassembleAnimations[currentHour].snapTo(0f)

                    disassembleAnimations[currentHour].animateTo(
                        1f,
                        tween(disassembleDuration, easing = LinearOutSlowInEasing)
                    )
                }
            }
        }
    }*/

    var strokeWidth by remember { mutableStateOf(0f) }

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            // Set strokeWidth based on the size of the viewport
            .onGloballyPositioned {
                strokeWidth = (it.size.width / 24).toFloat()
            }
            .drawBehind {
                val halfStroke = strokeWidth / 2
                val stepHeight = size.height / 24

                val center = Offset(size.width / 2, size.height / 2)
                val endOffset = Offset(
                    size.width / 2,
                    (size.height / 2) - calculateClockHandLength(size.height / 2, currentHour)
                )

                // Rotate the line around the pivot point, which is the
                // center of the screen. Rotation goes from 0 to 720 degrees
                rotate(animationAngle, pivot = center) {
                    // Drawing a clock hand itself
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = endOffset,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Drawing a clock hand
                    if (assembleValue != -1f) {
                        val positionY = halfStroke + calculateAssembleDistance(
                            stepHeight = stepHeight,
                            currentHour = currentHour
                        ) * assembleValue

                        val start = Offset(size.width / 2, positionY)
                        val end = Offset(size.width / 2, positionY + halfStroke)

                        drawLine(
                            color = Color.White,
                            start = start,
                            end = end,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }

                hours.forEach {
                    if (!dotsVisibility[it]) return@forEach
                    val degree = it * 30f

                    rotate(degree, pivot = center) {
                        // Based on the hour value, the travel distance will be longer.
                        val positionY =
                            halfStroke + stepHeight * it * (1 - dotsPositions[it])

                        val start = Offset(size.width / 2, positionY)
                        val end = Offset(size.width / 2, positionY + halfStroke)
                        drawLine(
                            color = Color.White,
                            start = start,
                            end = end,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
    )
}

fun calculateClockHandLength(maxHeight: Float, currentHour: Int): Float {
    val stepHeight = maxHeight / 12

    // Height decreases first 360 deg, then increases again
    return stepHeight * if (currentHour < 12) {
        12 - 1 - currentHour
    } else {
        currentHour - 12
    }
}


private fun calculateAssembleDistance(stepHeight: Float, currentHour: Int): Float {
    return stepHeight * (23 - currentHour)
}

@Suppress("SameParameterValue")
private fun angleToFraction(
    angle: Float,
    startAngle: Float,
    degreeLimit: Float,
    easing: Easing
): Float {
    val currentDeg: Float = (angle - startAngle).coerceIn(0f, degreeLimit)
    // Progress from 0 to 1
    val progressFraction: Float = currentDeg / degreeLimit
    return easing.transform(progressFraction)
}


