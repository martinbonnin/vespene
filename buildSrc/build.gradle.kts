plugins {
  kotlin("jvm").version("1.4.30")
}

repositories {
  mavenCentral()
}

dependencies {
  // use ourselves :)
  implementation("net.mbonnin.vespene:vespene-lib:0.4")
}