⚡ Vespene adds [fuel](https://starcraft.fandom.com/wiki/Vespene_gas) to your [Nexus](https://www.sonatype.com/nexus/repository-oss) repositories/ 

[Maven Central](https://search.maven.org/) is a great place to host maven packages but publishing often comes with some friction. 

Vespene is a set of tools to help library maintainers upload packages to Maven Central. It is thought as a library to be used from Gradle scripts or *.main.kts scripts. 

For now, the main focuses are:
* Avoiding split repositories during upload by [creating a staging repository](https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API) before upload and uploading all files to this staging repository.
* GPG Signing without having to deal with GPG using [BouncyCastle](https://www.bouncycastle.org/) instead.  
* Helpers for md5/sha1 cheksums.
* A Nexus 2.x (OSSRH is still on 2.x) API that gathers scattered documentation. 

More to come, contributions welcome!

## Moving existing artifacts from jcenter to mavenCentral

This lib can be used 






