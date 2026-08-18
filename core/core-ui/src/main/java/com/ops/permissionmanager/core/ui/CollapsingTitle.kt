package com.ops.permissionmanager.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 顶部可收缩标题。
 *
 * 展开态（collapsed=false）：标题 24sp 居中，副标题显示；
 * 收缩态（collapsed=true）：标题 17sp 靠左，副标题隐藏。
 * 字号与水平对齐均由 250ms FastOutSlowIn 缓动驱动。
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
        val titleWidth = 120.dp
        val pxWidth = maxWidth
        val translationPx = ((pxWidth - titleWidth) * alignFraction) / 2f

        Text(
            text = title,
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .graphicsLayer {
                    translationX = translationPx.toPx()
                }
        )

        AnimatedVisibility(
            visible = subtitle != null && !collapsed,
            modifier = Modifier.padding(top = 34.dp)
        ) {
            subtitle?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 56.dp)
                )
            }
        }
    }
}
