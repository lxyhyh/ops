package com.ops.permissionmanager.data.appops

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppOpsModule {

    @Binds
    @Singleton
    abstract fun bindAppOpsRepository(impl: RealAppOpsRepository): AppOpsRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(impl: RealAuditRepository): AuditRepository

    @Binds
    @Singleton
    abstract fun bindModifyModeRepository(impl: DataStoreModifyModeRepository): ModifyModeRepository

    @Binds
    @Singleton
    abstract fun bindCommandExecutor(impl: CommandExecutorRouter): CommandExecutor

    @Binds
    @Singleton
    abstract fun bindExecutionAvailability(impl: CommandExecutorRouter): ExecutionAvailability

    @Module
    @InstallIn(SingletonComponent::class)
    object CommandExecutorsModule {

        @Provides
        @Singleton
        @Named("root")
        fun provideRootExecutor(): CommandExecutor = RootCommandExecutor()

        @Provides
        @Singleton
        @Named("shizuku")
        fun provideShizukuExecutor(shizukuManager: ShizukuManager): CommandExecutor =
            ShizukuCommandExecutor(shizukuManager)
    }
}
