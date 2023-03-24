package com.ravikantsharma.clockanimation_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravikantsharma.clockanimation_compose.ui.theme.ClockAnimationComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClockAnimationComposeTheme {

            }
        }
    }
}

//@Preview(showBackground = true)
@Composable
fun Step2ShrinkAndExtendArrowPreview() {
    ClockAnimationComposeTheme {
        val size = 300.dp
        Box(
            modifier = Modifier
                .width(size)
                .height(size)
                .background(Color.Black)
                .padding(16.dp)
        ) {
            ClockAnimation(duration = 6000)
        }
    }
}

@Composable
fun ClockAnimation(duration: Int) {
    val infiniteTransition = rememberInfiniteTransition()

    // Creates a child animation of float type as a part of the [InfiniteTransition].
    val clockAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    var strokeWidth by remember {
        mutableStateOf(0f)
    }

    val currentHour by remember(clockAnimation) {
        derivedStateOf { clockAnimation.toInt() / 30 }
    }

    val hours: List<Int> = remember {
        List(12) { it }
    }

    val dotsVisibility = remember(currentHour) {
        hours.map { index ->
            when {
                index > currentHour -> false
                index > currentHour - 12 -> true
                else -> false
            }
        }
    }

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            // Set strokeWidth based on the size of the viewport
            .onGloballyPositioned {
                strokeWidth = (it.size.width / 24).toFloat()
            }
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                val endOffset = Offset(
                    size.width / 2,
                    size.height / 2 - calculateClockHandLength(size.height / 2, currentHour)
                )

                // Rotate the line around the pivot point, which is the
                // center of the screen. Rotation goes from 0 to 720 degrees
                rotate(clockAnimation, pivot = center) {
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = endOffset,
                        strokeWidth = strokeWidth
                    )
                }

                hours.forEach {
                    if (!dotsVisibility[it]) return@forEach
                    val degree = it * 30f

                    rotate(degree, pivot = center) {
                        val start = Offset(size.width / 2, 0f)
                        val end = Offset(size.width / 2, strokeWidth)
                        drawLine(
                            color = Color.White,
                            start = start,
                            end = end,
                            strokeWidth = strokeWidth
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








