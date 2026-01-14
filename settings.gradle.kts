rootProject.name = "vespene"

pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
            google()
            gradlePluginPortal()
        }
    }
    repositories {
        maven("https://storage.googleapis.com/gradleup/m2")
    }
}

include(":vespene-lib")
include(":vespene-cli")

//includeBuild("../librarian")