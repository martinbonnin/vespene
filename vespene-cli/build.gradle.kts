import com.gradleup.librarian.gradle.librarianModule

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("application")
  id("com.gradleup.librarian")
}

dependencies {
  implementation(project(":vespene-lib"))
  implementation("com.github.ajalt.clikt:clikt:3.1.0")
  implementation("com.squareup.okio:okio:3.4.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

application {
  mainClass.set("net.mbonnin.vespene.cli.MainKt")
}

librarianModule()