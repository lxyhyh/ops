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
    abstract fun bindModifyModeRepository(impl: SharedPrefsModifyModeRepository): ModifyModeRepository

    /** 未限定名的 CommandExecutor 绑定到路由器（Router 内部按修改模式分派）。 */
    @Binds
    @Singleton
    abstract fun bindCommandExecutor(impl: CommandExecutorRouter): CommandExecutor

    /**
     * 提供带 @Named 限定的 Root / Shizuku 执行器，
     * 供 [CommandExecutorRouter] 的 @Named("root") / @Named("shizuku") 注入点使用。
     */
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