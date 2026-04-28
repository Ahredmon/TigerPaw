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

rootProject.name = "TigerPaw"

include(":app")

// Core modules
include(":core:common")
include(":core:ui")
include(":core:data")

// Feature modules
include(":feature:home")
include(":feature:apps")
include(":feature:widgets")
include(":feature:settings")
include(":feature:search")
