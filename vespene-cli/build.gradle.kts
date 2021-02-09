plugins {
  id("maven-publish")
  id("signing")
  kotlin("jvm")
  kotlin("kapt")
  id("net.mbonnin.one.eight")
  id("application")
}

dependencies {
  implementation(project(":vespene-lib"))
  implementation("com.github.ajalt.clikt:clikt:3.1.0")
  implementation("com.squareup.okio:okio:2.10.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

application {
  mainClass.set("net.mbonnin.vespene.cli.MainKt")
}

configurePublishing()
