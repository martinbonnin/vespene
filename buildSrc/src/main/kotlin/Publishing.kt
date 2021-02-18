import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import net.mbonnin.vespene.lib.NexusStagingClient
import kotlinx.coroutines.runBlocking

fun isPublishingBuild() = !System.getenv("GPG_PRIVATE_KEY").isNullOrBlank()

val stagingUrl: String by lazy {
  val client = NexusStagingClient(
    username = System.getenv("SONATYPE_NEXUS_USERNAME"),
    password = System.getenv("SONATYPE_NEXUS_PASSWORD"),
  )
  val repositoryId = runBlocking {
    client.createRepository(
      profileId = System.getenv("VESPENE_STAGING_PROFILE_ID"),
    )
  }
  println("publishing to '$repositoryId")
  "https://oss.sonatype.org/service/local/staging/deployByRepositoryId/${repositoryId}/"
}

fun Project.configurePublishing() {
  val sourcesJar = tasks.register("sourcesJar", Jar::class.java) {
    it.archiveClassifier.set("sources")
    val sourceSets = extensions.getByName("sourceSets") as SourceSetContainer
    it.from(sourceSets.getByName("main").allSource)
  }

  val javadocJar = tasks.register("javadocJar", Jar::class.java) {
    // Empty javadoc :)
    it.archiveClassifier.set("javadoc")
  }

  extensions.findByType(PublishingExtension::class.java)!!.run {
    publications {
      it.create("default", MavenPublication::class.java) {
        it.from(components.getByName("java"))

        it.groupId = rootProject.group as String
        it.version = rootProject.version as String
        it.artifact(javadocJar)
        it.artifact(sourcesJar)

        if (name == "vespene-cli") {
          val fatJar = tasks.register("fatJar", Jar::class.java) {
            it.manifest {
              it.attributes(mapOf("Main-Class" to "net.mbonnin.vespene.cli.MainKt"))
            }

            // we need flatMap here to avoid an obscure error of resolving the classpath too early
            val fileCollection = configurations.named("runtimeClasspath").flatMap {
              provider { it.files.map { if (it.isDirectory) it else zipTree(it) } }
            }
            it.from(fileCollection) {
              it.exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
            }
            it.with(tasks.getByName("jar") as CopySpec)
            it.archiveClassifier.set("all")

            it.archiveBaseName.set("vespene-cli-all")
          }
          it.artifact(fatJar)

        }
        it.pom {
          with (it) {
            this.name.set("Vespene")
            this.description.set("A set of tools to work with Sonatype Nexus repositories")
            this.url.set("https://github.com/martinbonnin/vespene")
            this.scm {
              it.url.set("https://github.com/martinbonnin/vespene")
            }
            this.licenses {
              it.license {
                it.name.set("MIT License")
                it.url.set("https://github.com/martinbonnin/vespene/blob/main/LICENSE")
              }
            }
            this.developers {
              it.developer {
                it.name.set("Martin Bonnin")
              }
            }
          }
        }
      }
    }
    repositories {
      it.maven {
        it.name = "ossStaging"
        it.setUrl(provider {
          stagingUrl
        })
        it.credentials {
          it.username = System.getenv("SONATYPE_NEXUS_USERNAME")
          it.password = System.getenv("SONATYPE_NEXUS_PASSWORD")
        }
      }
    }
  }

  if (isPublishingBuild()) {
    extensions.findByType(SigningExtension::class.java)!!.run {
      // GPG_PRIVATE_KEY should contain the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
      // It can be obtained with gpg --armour --export-secret-keys KEY_ID
      useInMemoryPgpKeys(System.getenv("VESPENE_GPG_PRIVATE_KEY"), System.getenv("VESPENE_GPG_PRIVATE_KEY_PASSWORD"))
      val publicationsContainer = (extensions.getByName("publishing") as PublishingExtension).publications
      sign(publicationsContainer)
    }
  }
}
