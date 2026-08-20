package com.ops.permissionmanager.core.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 记住并异步加载指定包名的应用图标。
 *
 * 实现：IO 线程取 Drawable → 交给 Coil 解码并缓存（Coil 内部维护内存缓存，替代自实现 LruCache）。
 * 缓存未命中时切换到 IO 线程加载，避免包管理 IO 阻塞主线程。
 *
 * @return 加载成功返回 [ImageBitmap]，失败或尚未加载返回 null。
 */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val loader = context.imageLoader
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val request = ImageRequest.Builder(context).data(drawable).build()
                loader.execute(request).drawable?.toBitmap()?.asImageBitmap()
            }.getOrNull()
        }
    }
    return icon
}

/** 将 Drawable 绘制到 ARGB_8888 Bitmap（任意 Drawable 包括 AdaptiveIconDrawable 都正确渲染）。 */
private fun android.graphics.drawable.Drawable.toBitmap(): Bitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
        val canvas = android.graphics.Canvas(bmp)
        setBounds(0, 0, width, height)
        draw(canvas)
    }
}