val localProperties = java.util.Properties()
val localPropertiesFile = File(rootDir, "local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val githubUsername = localProperties.getProperty("github.username") ?: ""
val githubToken = localProperties.getProperty("github.token") ?: ""

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
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        mavenLocal()

        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
        }

        maven {
            name = "IA SDK repo"
            url = uri("https://maven.pkg.github.com/ihreapotheken/IA-SDK-Android")
            credentials {
                username = githubUsername
                password = githubToken
            }
        }
    }
}

rootProject.name = "IA SDK Example"
include(":example")
include(":example2")
include(":example3")
