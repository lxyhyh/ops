package com.ops.permissionmanager.core.model

/**
 * AppOps 权限模式。
 * 对应 cmd appops set 命令的 mode 参数。
 */
enum class OpMode(val commandValue: String, val displayName: String) {
    ALLOW("allow", "允许"),
    DENY("deny", "拒绝"),
    IGNORE("ignore", "忽略"),
    DEFAULT("default", "默认"),
    ASK("ask", "询问");

    companion object {
        fun fromCommandValue(value: String): OpMode? =
            entries.firstOrNull { it.commandValue == value }
    }
}
