package com.ops.permissionmanager.core.model

/**
 * AppOps 操作模式。
 *
 * @property commandValue 对应的命令行值
 * @property displayName 面向用户的显示名称
 */
enum class OpMode(val commandValue: String, val displayName: String) {
    ALLOW("allow", "允许"),
    DENY("deny", "拒绝"),
    IGNORE("ignore", "忽略"),
    DEFAULT("default", "默认"),
    ASK("ask", "询问");

    companion object {
        /**
         * 根据命令行值查找对应的模式，未找到返回 [null]。
         */
        fun fromCommandValue(value: String): OpMode? =
            entries.firstOrNull { it.commandValue == value }
    }
}