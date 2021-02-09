plugins {
  id("maven-publish")
  id("signing")
  kotlin("jvm").version("1.4.30").apply(false)
  kotlin("kapt").version("1.4.30").apply(false)
  id("net.mbonnin.one.eight").version("0.1").apply(false)
}

group = "net.mbonnin.vespene"
version = "0.2"

allprojects {
  repositories {
    mavenCentral()
  }
}
