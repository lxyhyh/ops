package com.ops.permissionmanager.data.appops

interface ExecutionAvailability {
    suspend fun isAnyAvailable(): Boolean
    suspend fun isRootAvailable(): Boolean
    suspend fun isShizukuAvailable(): Boolean
}
