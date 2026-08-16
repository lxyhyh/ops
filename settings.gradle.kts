pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OpsPermissionManager"
include(":app")
include(":core:core-model")
include(":core:core-ui")
include(":data:data-appops")
include(":data:data-applist")
include(":feature:feature-applist")
include(":feature:feature-batch")
include(":feature:feature-history")
