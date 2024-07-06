rootProject.name = "vespene"

pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            mavenCentral()
            google()
            gradlePluginPortal()
        }
    }
}

include(":vespene-lib")
include(":vespene-cli")
includeBuild("../librarian")