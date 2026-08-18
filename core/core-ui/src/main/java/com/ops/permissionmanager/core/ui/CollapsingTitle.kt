package com.ops.permissionmanager.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 顶部可收缩标题。
 *
 * 展开态（collapsed=false）：标题 24sp 靠左，副标题显示；
 * 收缩态（collapsed=true）：标题 17sp 水平居中，副标题隐藏。
 * 字号与水平位移均由 250ms FastOutSlowIn 缓动驱动。
 */
@Composable
fun CollapsingTitle(
    title: String,
    subtitle: String?,
    collapsed: Boolean,
    modifier: Modifier = Modifier
) {
    val titleSize by animateFloatAsState(
        targetValue = if (collapsed) 17f else 24f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "titleSize"
    )
    val alignFraction by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "titleAlign"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = titleSize.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    translationX = (containerWidthPx - size.width) * alignFraction / 2f
                }
            )

            AnimatedVisibility(visible = !collapsed) {
                subtitle?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}