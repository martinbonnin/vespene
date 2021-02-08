⚡ Vespene adds [fuel](https://starcraft.fandom.com/wiki/Vespene_gas) to your [Nexus](https://www.sonatype.com/nexus/repository-oss) repositories.

[Maven Central](https://search.maven.org/) is a great place to host maven packages but publishing often comes with some friction. 

Vespene is a set of tools to help library maintainers upload packages to Maven Central. It is thought as a library to be used from Gradle scripts or *.main.kts scripts. 

For now, the main focuses are:
* Avoiding split repositories during upload by [creating a staging repository](https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API) before upload and uploading all files to this staging repository.
* GPG Signing without having to deal with GPG using [BouncyCastle](https://www.bouncycastle.org/) instead.  
* Helpers for md5/sha1 cheksums.
* A Nexus 2.x (OSSRH is still on 2.x) API that gathers scattered documentation. 

More to come, contributions welcome!

## Moving existing artifacts from jcenter to mavenCentral

With jcenter shutting down, moving existing artifacts to mavenCentral will make sure older versions will stay available after May 2021. Bintray has a very handy [sync checkbox](https://www.jfrog.com/confluence/display/BT/Syncing+with+Third-Party+Platforms) that syncs artifacts to MavenCentral. This works well with two limitations:

* That gives your sonatype credentials to Bintray.
* It doesn't work if your artifacts do not pass the [mavenCentral requirements](https://central.sonatype.org/pages/requirements.html)

That last point can happen relatively frequently given that jcenter is less strict than mavenCentral and allows artifacts without sources/javadoc, pom files with missing information, etc..

To upload with Vespene, use a `NexusUploader`:

```kotlin
  val uploader = NexusUploader(
    username = sonatypeUsername,
    password = sonatypePassword,
    stagingProfileId = sonatypeProfileId
  )

  /**
   * This will create a staging repository and upload everything in one go
   */
  uploader.upload(File("/path/to/your/files"))
```





