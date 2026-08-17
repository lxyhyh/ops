package com.ops.permissionmanager.core.model

/**
 * AppOps 操作标识。
 * 对应 cmd appops 命令中的 op 名称（如 RUN_IN_BACKGROUND、READ_CLIPBOARD 等）。
 */
data class AppOp(
    val name: String,
    val displayName: String,
    val group: OpGroup
)

/** AppOps 权限分组，用于界面分类展示。 */
enum class OpGroup(val displayName: String) {
    BACKGROUND("后台运行"),
    PRIVACY("隐私"),
    LOCATION("定位"),
    CONTACTS("联系人"),
    PHONE("电话"),
    SMS("短信"),
    MEDIA("媒体"),
    AUDIO("音频"),
    NOTIFICATION("通知"),
    SENSOR("传感器"),
    BATTERY("电量"),
    STORAGE("存储"),
    BLUETOOTH("蓝牙"),
    SYSTEM("系统"),
    OTHER("其他")
}
