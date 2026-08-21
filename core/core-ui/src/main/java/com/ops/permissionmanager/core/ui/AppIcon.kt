package com.ops.permissionmanager.core.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用图标缓存，避免每次重组重复加载包名对应的图标。
 *
 * 自实现 LruCache + IO 异步（决策记录见 docs/REUSE-DECISIONS.md D3）：
 * Coil 对本地应用图标场景无优势且传递引入 okhttp（+0.5MB 包体），故回退自实现。
 *
 * 性能：按条目数（256）而非字节计——128×128 ARGB_8888 约 64KB/个，256 项约 16MB 上限；
 * 对滚动列表足够（可见项 10~15 个），且避免字节计算开销与缓存抖动。
 */
private val iconCache = object : LruCache<String, ImageBitmap>(256) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

/** 加载失败的包名集合：失败后不再重复发起 IO 加载（滚动回来直接显示占位）。 */
private val failedIcons = ConcurrentHashMap.newKeySet<String>()

/** 正在加载中的包名集合：同一包名并发重组时只发起一次 IO 任务。 */
private val loadingIcons = ConcurrentHashMap.newKeySet<String>()

/**
 * 将 Drawable 绘制到固定尺寸的 ARGB_8888 Bitmap 上（与原版 Drawable.toBitmap(128,128)
 * 行为一致：任意 Drawable 包括 AdaptiveIconDrawable 都会正确渲染）。
 */
private fun android.graphics.drawable.Drawable.toBitmapSized(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

/**
 * 记住并异步加载指定包名的应用图标（带缓存）。
 * 缓存未命中时切换到 IO 线程加载，避免包管理 IO 阻塞主线程。
 *
 * @return 加载成功返回 [ImageBitmap]，失败或尚未加载返回 null。
 */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        iconCache.get(packageName)?.let { value = it; return@produceState }
        if (packageName in failedIcons) return@produceState
        // 去重：同一包名正在加载时跳过本次（完成后缓存可命中，下次重组即显示）
        if (!loadingIcons.add(packageName)) return@produceState
        try {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    drawable.toBitmapSized(128, 128).asImageBitmap()
                }.getOrNull()?.also { iconCache.put(packageName, it) }
            }
            if (value == null) failedIcons.add(packageName)
        } finally {
            loadingIcons.remove(packageName)
        }
    }
    return icon
}