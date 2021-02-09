plugins {
  id("maven-publish")
  `java-library`
  id("signing")
  kotlin("jvm")
  kotlin("kapt")
  id("net.mbonnin.one.eight")
}

dependencies {
  implementation("com.squareup.okhttp3:okhttp:4.9.0")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
  api("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
  implementation("org.bouncycastle:bcprov-jdk15on:1.64")
  implementation("org.bouncycastle:bcpg-jdk15on:1.64")
  implementation("com.squareup.moshi:moshi:1.11.0")

  kapt("com.squareup.moshi:moshi-kotlin-codegen:1.11.0")

  testImplementation("junit:junit:4.12")
}

configurePublishing()