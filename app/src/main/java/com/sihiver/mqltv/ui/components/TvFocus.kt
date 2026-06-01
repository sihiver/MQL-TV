package com.sihiver.mqltv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sihiver.mqltv.ui.theme.AccentOrange

@Composable
fun TvFocusableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AccentOrange,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = Color(0x0AFFFFFF),
    focusedBackgroundColor: Color = accentColor.copy(alpha = 0.22f),
    unfocusedBorderWidth: Dp = 1.dp,
    focusedBorderWidth: Dp = 4.dp,
    focusedScale: Float = 1.06f,
    onFocused: (() -> Unit)? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused?.invoke()
            bringIntoViewRequester.bringIntoView()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = tween(180),
        label = "tvFocusScale",
    )

    // Padding luar agar ring fokus tidak ter-clip parent
    Box(
        modifier = modifier.padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .bringIntoViewRequester(bringIntoViewRequester)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                // Ring fokus di LUAR clip — selalu terlihat saat pindah item
                .border(
                    width = if (isFocused) focusedBorderWidth else unfocusedBorderWidth,
                    color = when {
                        isFocused -> Color.White
                        else -> Color(0x28FFFFFF)
                    },
                    shape = shape,
                )
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 2.dp,
                            color = accentColor,
                            shape = shape,
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .background(if (isFocused) focusedBackgroundColor else backgroundColor)
                .onKeyEvent {
                    when {
                        it.key == Key.Back -> true
                        (it.key == Key.DirectionCenter || it.key == Key.Enter) -> {
                            if (it.type == KeyEventType.KeyUp) {
                                onClick()
                            }
                            true
                        }
                        else -> false
                    }
                }
                .focusable(interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
            content = { content(isFocused) },
        )
    }
}
