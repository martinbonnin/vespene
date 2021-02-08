package net.mbonnin.vespene

import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * NexusUploader will create a staging repository and upload the given files to the given staging repository
 * Creating a staging repository avoid having split artifacts
 *
 * The process is outlined in this document (from 2014!):
 * https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API
 *
 * @param baseUrl: the url of the nexus instance, defaults to the OSSRH url
 * @param stagingProfileId: the staging profile used to upload. If you own several groupIds on the Nexus instance
 * you'll have multiple stagingProfileId. You can see them at https://oss.sonatype.org/#stagingProfiles
 * The name will be like "com.example" and the id will appear in the url bar when you click one
 */
class NexusUploader(
  private val baseUrl: String = "https://oss.sonatype.org/service/local/",
  private val username: String,
  private val password: String,
  private val stagingProfileId: String
) {

  /**
   * @param directory: the root of a directory containing a maven hierarchy like below.
   *
   * $directory/com/example/module1/version/module1-version.jar
   * $directory/com/example/module1/version/module1-version.jar.md5
   * $directory/com/example/module1/version/module1-version.jar.asc
   * $directory/com/example/module1/version/module1-version.jar.asc.md5
   * $directory/com/example/module1/version/module1-version.pom
   * $directory/com/example/module1/version/module1-version.pom.md5
   * $directory/com/example/module1/version/module1-version.pom.asc
   * $directory/com/example/module1/version/module1-version.pom.asc.md5
   * $directory/com/example/module1/version/module1-version.jar
   * $directory/com/example/module2/version/module2-version.jar
   * etc...
   *
   * The directory can contain several modules/versions
   *
   */
  fun upload(directory: File) {
    val client = OkHttpClient(username, password)
    val nexusApi = NexusApi(client, baseUrl)

    runBlocking {
      println("creating staging repository...")
      val response = nexusApi.startStagingRepository(stagingProfileId, Data(Description("Staging Profile")))
      check(response.isSuccessful) {
        "cannot start stagingRepository ${response.code()}: ${response.errorBody()?.string()} "
      }
      check(response.body()?.data != null) {
        "no body"
      }

      val stagingRepositoryId = response.body()?.data?.stagedRepositoryId
      println("staging repository id: $stagingRepositoryId")

      val files = directory.walk()
        .filter {
          it.isFile
        }
        .toList()

        files.forEachIndexed { index, file ->
          val relativePath = file.relativeTo(directory).path
          println(String.format("%4d/%4d $relativePath", index, files.size))

          val url = "${baseUrl}staging/deployByRepositoryId/${stagingRepositoryId}/$relativePath"
          val request = Request.Builder()
            .put(file.asRequestBody("application/octet-stream".toMediaType()))
            .url(url)
            .build()

          val uploadResponse = client.newCall(request).execute()
          check(uploadResponse.isSuccessful) {
            "Cannot put $url:\n${uploadResponse.body?.string()}"
          }
        }

      println("all files uploaded successfully to staging repository $stagingRepositoryId")

    }
  }
}