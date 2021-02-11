package net.mbonnin.vespene.lib

import kotlinx.coroutines.delay
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * NexusStagingClient will create a staging repository and upload the given files to the given staging repository
 * Creating a staging repository avoid having split artifacts
 *
 * The process is outlined in this document (from 2014!):
 * https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API
 *
 * @param baseUrl: the url of the nexus instance, defaults to the OSSRH url
 * @param username: your Nexus username. For OSSRH, this is your Sonatype jira username
 * @param password: your Nexus password. For OSSRH, this is your Sonatype jira password
 */
class NexusStagingClient(
  private val baseUrl: String = "https://oss.sonatype.org/service/local/",
  username: String,
  password: String,
) {
  private val okHttpClient = OkHttpClient(username, password)
  private val nexusApi = NexusApi(okHttpClient, baseUrl)

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
   * @param profileId: the profileId. For OSSRH, you will see it at https://oss.sonatype.org/#stagingProfiles;${profileId}
   * If you have only one, you can also get it from [getProfiles]
   *
   * @param progress: a callback called for each file
   *
   */
  suspend fun upload(directory: File, profileId: String, progress: ((index: Int, total: Int, path: String) -> Unit)? = null): String {
    val response = nexusApi.createRepository(profileId, Data(Description("Vespene Staging Repository")))
    check(response.isSuccessful) {
      "createRepository error ${response.code()}: ${response.errorBody()?.string()} "
    }

    val repositoryId = response.body()?.data?.stagedRepositoryId
    check(repositoryId != null) {
      "no stagingRepositoryId returned"
    }
    val files = directory.walk()
      .filter {
        it.isFile
      }
      .toList()

    files.forEachIndexed { index, file ->
      val relativePath = file.relativeTo(directory).path
      progress?.invoke(index, files.size, relativePath)

      val url = "${baseUrl}staging/deployByRepositoryId/${repositoryId}/$relativePath"
      val request = Request.Builder()
        .put(file.asRequestBody("application/octet-stream".toMediaType()))
        .url(url)
        .build()

      val uploadResponse = okHttpClient.newCall(request).execute()
      check(uploadResponse.isSuccessful) {
        "Cannot put $url:\n${uploadResponse.body?.string()}"
      }
    }

    return repositoryId
  }

  /**
   * Return a list of all staging repositories
   */
  suspend fun getRepositories(): List<Repository> {
    val response = nexusApi.getRepositories()
    check(response.isSuccessful && response.body() != null) {
      "getRepositories error:\n${response.errorBody()?.string()}"
    }
    return response.body()!!.data
  }

  /**
   * Return a specific staging repository
   */
  suspend fun getRepository(repositoryId: String): Repository {
    val response = nexusApi.getRepository(repositoryId)
    check(response.isSuccessful && response.body() != null) {
      "getRepository($repositoryId) error:\n${response.errorBody()?.string()}"
    }
    return response.body()!!
  }

  /**
   * Closes the given staging repositories. Closing a repository triggers the cheks (groupId, pom, signatures, etc...)
   * It is mandatory to close a repository before it can be released.
   */
  suspend fun closeRepositories(repositoryIds: List<String>) {
    val response = nexusApi.closeRepositories(Data(TransitionRepositoryInput(repositoryIds)))
    check(response.isSuccessful) {
      "closeRepositories($repositoryIds) error:\n${response.errorBody()?.string()}"
    }
  }

  /**
   * Releases the given staging repositories. This is the big "release" button. Once a repository is released, it cannot
   * be removed. Use with care.
   */
  suspend fun releaseRepositories(repositoryIds: List<String>, dropAfterRelease: Boolean) {
    val response = nexusApi.releaseRepositories(
      Data(
        TransitionRepositoryInput(
          stagedRepositoryIds = repositoryIds,
          autoDropAfterRelease = dropAfterRelease
        )
      )
    )

    check(response.isSuccessful) {
      "releaseRepositories($repositoryIds) error:\n${response.errorBody()?.string()}"
    }
  }

  /**
   * Creates a new staging repository.
   *
   * @return the id of the created repository
   */
  suspend fun createRepository(profileId: String): String {
    val response = nexusApi.createRepository(profileId, Data(Description("Vespene Staging Repository")))
    check(response.isSuccessful && response.body() != null) {
      "createRepository($profileId) error:\n${response.errorBody()?.string()}"
    }

    return response.body()!!.data.stagedRepositoryId
  }

  /**
   * Drops the given staging repositories. This will delete the repositories and all content associated.
   */
  suspend fun dropRepositories(repositoryIds: List<String>) {
    val response = nexusApi.dropRepositories(Data(TransitionRepositoryInput(repositoryIds)))
    check(response.isSuccessful) {
      "dropRepositories($repositoryIds) error:\n${response.errorBody()?.string()}"
    }
  }

  /**
   * @return the list of all profiles associated with this account
   */
  suspend fun getProfiles(): List<Profile> {
    val response = nexusApi.getProfiles()

    check(response.isSuccessful) {
      "getProfiles error:\n${response.errorBody()?.string()}"
    }

    val data = response.body()
    check(data != null) {
      "getProfiles didn't return any data"
    }

    return data.data
  }

  /**
   * [waitForClose] is a meta API that will use [getRepositories] to check for the status of a repository
   */
  suspend fun waitForClose(repositoryId: String, pollingIntervalMillis: Int, progress: () -> Unit) {
    while (true) {
      progress()

      val response = nexusApi.getRepository(repositoryId)

      val repository = response.body()
      check(response.isSuccessful && repository != null) {
        "getRepository($repositoryId) error:\n${response.errorBody()?.string()}"
      }
      if (repository.type == "closed" && !repository.transitioning) {
        break
      }

      delay(pollingIntervalMillis.toLong())
    }
  }
}