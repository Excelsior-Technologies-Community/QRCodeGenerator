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
        google() // Optional but recommended for Android projects
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "QRCodeGenerator"
include(":app")
include(":QRCodeGenerator")

