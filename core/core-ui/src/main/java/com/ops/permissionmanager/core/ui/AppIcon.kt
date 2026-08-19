package com.ops.permissionmanager.core.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用图标缓存，避免每次重组重复加载包名对应的图标。
 *
 * 资源占用优化：改为**按字节**限制（默认 LruCache.sizeOf 按条目计，512×64KB 上限不可控）。
 * 128×128 ARGB 每标 ≈64KB，20MB ≈ 320 个，覆盖 300+ 已装应用的滚动回看。
 */
private val iconCache = object : LruCache<String, ImageBitmap>(20 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

/**
 * 将 Drawable 绘制到固定尺寸的 ARGB_8888 Bitmap 上（与原版 Drawable.toBitmap(128,128)
 * 行为一致：任意 Drawable 包括 AdaptiveIconDrawable 都会正确渲染）。
 */
private fun Drawable.toBitmapSized(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

/**
 * 记住并异步加载指定包名的应用图标（带缓存）。
 * 与反编译原版一致：缓存未命中时切换到 IO 线程加载，避免包管理 IO 阻塞主线程。
 *
 * @return 加载成功返回 [ImageBitmap]，失败或尚未加载返回 null。
 */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        iconCache.get(packageName)?.let { value = it; return@produceState }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmapSized(128, 128).asImageBitmap()
            }.getOrNull()?.also { iconCache.put(packageName, it) }
        }
    }
    return icon
}