package com.ops.permissionmanager.core.model

/**
 * 权限修改方式的模式。
 *
 * @property displayName 模式的显示名称
 * @property description 模式的说明文字
 */
enum class ModifyMode(val displayName: String, val description: String) {
    AUTO("自动", "自动选择当前可用的方式"),
    ROOT("Root", "通过 Root 权限修改"),
    SHIZUKU("ADB (Shizuku)", "通过 ADB 授权 (Shizuku) 修改");

    companion object {
        /**
         * 根据枚举名查找对应的模式，未找到回退为 [AUTO]。
         */
        fun fromName(name: String?): ModifyMode =
            entries.firstOrNull { it.name == name } ?: AUTO
    }
}