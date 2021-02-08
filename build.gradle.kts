plugins {
  kotlin("jvm").version("1.4.21").apply(false)
  kotlin("kapt").version("1.4.21").apply(false)
  id("net.mbonnin.one.eight").version("0.1").apply(false)
}

group = "net.mbonnin.vespene"
version = "1.0-SNAPSHOT"

allprojects {
  repositories {
    mavenCentral()
  }
}

subprojects {
  apply(plugin = "maven-publish")

  afterEvaluate {
    val sourcesJar by tasks.creating(Jar::class) {
      archiveClassifier.set("sources")
      val sourceSets = this@subprojects.extensions.getByName("sourceSets") as SourceSetContainer
      from(sourceSets.getByName("main").allSource)
    }

    val javadocJar by tasks.creating(Jar::class) {
      // Empty javadoc :)
      archiveClassifier.set("javadoc")
    }

    configure<PublishingExtension> {
      publications {
        create<MavenPublication>("default") {
          from(components["java"])

          artifact(javadocJar)
          artifact(sourcesJar)

          pom {

          }
        }
      }
    }
  }
}

