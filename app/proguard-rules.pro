# OPS 权限管家 ProGuard/R8 规则

# Hilt 依赖注入：保留生成的组件与工厂类
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# 保留 ViewModel 构造器（Hilt 通过反射实例化）
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Compose 运行时需要的元数据
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**
