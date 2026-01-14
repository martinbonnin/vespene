import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm").version("2.3.20-Beta1").apply(false)
  id("com.google.devtools.ksp").version("2.3.4").apply(false)
  id("com.gradleup.librarian").version("0.2.2-SNAPSHOT-b82defcba093f4db772683cd32a4e9b511faf1f2").apply(false)
}

Librarian.root(project)
