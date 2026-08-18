package com.ops.permissionmanager.core.model

/**
 * AppOps 操作分组。
 *
 * @property displayName 分组的显示名称
 */
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