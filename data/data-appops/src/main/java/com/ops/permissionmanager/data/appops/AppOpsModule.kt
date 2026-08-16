package com.ops.permissionmanager.data.appops

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppOpsModule {

    @Binds
    @Singleton
    abstract fun bindAppOpsRepository(impl: RealAppOpsRepository): AppOpsRepository

    @Binds
    @Singleton
    abstract fun bindRootShell(impl: RealRootShell): RootShell
}
