import com.gradleup.librarian.gradle.librarianRoot

plugins {
  id("org.jetbrains.kotlin.jvm").version("2.0.0").apply(false)
  id("com.google.devtools.ksp").version("2.0.0-1.0.23").apply(false)
  id("com.gradleup.librarian").version("0.0.5")
}

librarianRoot()