plugins {
  kotlin("jvm").version("1.4.21").apply(false)
  kotlin("kapt").version("1.4.21").apply(false)
  id("net.mbonnin.one.eight").version("0.1").apply(false)
}

group = "net.mbonnin.vespene"
version = "0.1"

allprojects {
  repositories {
    mavenCentral()
  }
}

subprojects {
  apply(plugin = "maven-publish")
  apply(plugin = "signing")

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

          group = rootProject.group
          version = rootProject.version as String
          artifact(javadocJar)
          artifact(sourcesJar)

          pom {
            this.name.set("Vespene")
            this.description.set("A set of tools to work with Sonatype Nexus repositories")
            this.url.set("https://github.com/martinbonnin/vespene")
            this.scm {
              this.url.set("https://github.com/martinbonnin/vespene")
            }
            this.licenses {
              this.license {
                this.name.set("MIT License")
                this.url.set("https://github.com/martinbonnin/vespene/blob/main/LICENSE")
              }
            }
            this.developers {
              this.developer {
                this.name.set("Martin Bonnin")
              }
            }
          }
        }
      }
      repositories {
        maven {
          name = "ossStaging"
          url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
          credentials {
            username = System.getenv("SONATYPE_NEXUS_USERNAME")
            password = System.getenv("SONATYPE_NEXUS_PASSWORD")
          }
        }
      }
    }

    configure<SigningExtension> {
      // GPG_PRIVATE_KEY should contain the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
      // It can be obtained with gpg --armour --export-secret-keys KEY_ID
      useInMemoryPgpKeys(System.getenv("VESPENE_GPG_PRIVATE_KEY"), System.getenv("VESPENE_GPG_PRIVATE_KEY_PASSWORD"))
      val publicationsContainer = (this@subprojects.extensions.get("publishing") as PublishingExtension).publications
      sign(publicationsContainer)
    }
    tasks.withType<Sign> {
      isEnabled = !System.getenv("GPG_PRIVATE_KEY").isNullOrBlank()
    }
  }
}

