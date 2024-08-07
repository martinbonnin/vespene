import com.gradleup.librarian.gradle.librarianModule

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.librarian")
}

dependencies {
  implementation("com.squareup.okhttp3:okhttp:4.10.0")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  api("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
  implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
  implementation("org.bouncycastle:bcpg-jdk18on:1.78.1")
  implementation("com.squareup.moshi:moshi:1.15.1")

  ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

  testImplementation("junit:junit:4.13.2")
}

librarianModule()