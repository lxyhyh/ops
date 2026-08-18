package com.ops.permissionmanager.core.ui

import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/** 应用图标缓存，避免每次重组重复加载包名对应的图标。 */
private val iconCache = LruCache<String, ImageBitmap>(64)

/**
 * 记住并异步加载指定包名的应用图标（带缓存）。
 *
 * @return 加载成功返回 [ImageBitmap]，失败或尚未加载返回 null。
 */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        iconCache.get(packageName)?.let { value = it; return@produceState }
        value = runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            (drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
        }.getOrNull()?.also { iconCache.put(packageName, it) }
    }
    return icon
}