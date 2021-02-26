[![Maven Central](https://img.shields.io/maven-central/v/net.mbonnin.vespene/vespene-cli)](https://repo1.maven.org/maven2/net/mbonnin/vespene)

⚡ Vespene adds [fuel](https://starcraft.fandom.com/wiki/Vespene_gas) to your [Nexus](https://www.sonatype.com/nexus/repository-oss) repositories.

[Maven Central](https://search.maven.org/) is a great place to host maven packages but publishing often comes with some friction. 

Vespene is a set of tools to help library maintainers upload existing packages to Maven Central and other Nexus repositories without having to build older versions or modify their Gradle files.

For now, the main focuses are:
* Avoiding split repositories during upload by [creating a staging repository](https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API) before upload and uploading all files to this staging repository.
* GPG Signing without having to deal with GPG using [BouncyCastle](https://www.bouncycastle.org/) instead.  
* Helpers for md5/sha1 checksums.
* A Nexus 2.x (OSSRH is still on 2.x) API that gathers scattered documentation. 
* Automating (almost) everything

More to come, contributions welcome!

## Moving existing artifacts from JCenter to Maven Central

With JCenter shutting down, moving existing artifacts to Maven Central will make sure older versions will stay available in the long term. Bintray has a very handy [sync checkbox](https://www.jfrog.com/confluence/display/BT/Syncing+with+Third-Party+Platforms) that syncs artifacts to Maven Central. This works well with two limitations:

* That gives your sonatype credentials to Bintray.
* It doesn't work if your artifacts do not pass the [mavenCentral requirements](https://central.sonatype.org/pages/requirements.html)

That last point can happen relatively frequently given that JCenter is less strict than Maven Central and allows artifacts without sources/javadoc, pom files with missing information, etc..

Vespene comes with a [kscript-based](https://github.com/holgerbrandl/kscript) script to automate much of the process of re-computing checksums, signatures, etc..

To upload with the bundled [upload.kts](upload.kts) script:

```shell
# Use lftp to download your existing files
# Try not to download all of JCenter if possible 😅
brew install lftp
lftp https://jcenter.bintray.com/com/example/
> mirror . my-local-repo
# Download the script from this repo
curl -s "https://raw.githubusercontent.com/martinbonnin/vespene/main/upload.kts" > upload.kts
chmod +x upload.kts
# install kscript if you don't have it already
curl -s "https://get.sdkman.io" | bash
sdk install kscript
# Set env variables: 
export SONATYPE_NEXUS_USERNAME=... #(this is from your Sonatype jira account)
export SONATYPE_NEXUS_PASSWORD=...
# Export your private key 
export GPG_PRIVATE_KEY="$(gpg --armour --export-secret-keys KEY_ID)"
export GPG_PRIVATE_KEY_PASSWORD=...

# Read the script and **make sure you understand what it does**
# In a nutshell, it will patch .pom files, add missing .md5 and .asc files and upload everything 
# If that doesn't fit your requirements, edit the script with `kscript --idea upload.kts`

# Run it!
# During upload, it will create one staging repository and upload all files. 
# It will take time!
./upload.kts --input my-local-repo/ --scratch tmp/ --group com.example [--pom-project-url https://...]
 
# upload.kts does not release automatically (although it's easy to add it)
# To release, go to https://oss.sonatype.org/#stagingRepositories, check your contents and hit "Release" for repositories 
# that look good. 
# If there are errors, tweak the script until checks pass

# For other options about specifying versions, pom fields, etc, use --help
./upload.kts --help 
```

## Using the lib

The provided script makes some assumptions. It's going to reuse the existing JCenter signatures for an example to make as little changes as possible to the existing files but this might be inconvenient. 

If you want to tweak the script, or want to do other Nexus operations, you can also use the API directly from the `vespene-lib` artifact:

```
dependencies {
    implementation("net.mbonnin.vespene:vespene-lib:$latest")
}
```

And use NexusStagingClient:

```kotlin
  val client = NexusStagingClient(
    username = sonatypeUsername,
    password = sonatypePassword,
    stagingProfileId = sonatypeProfileId
  )

  val repositoryId = client.upload(File("/path/to/your/files"))
  client.close(repositoryId)
  // release/drop/etc...
```

Contributions/questions are welcome.



