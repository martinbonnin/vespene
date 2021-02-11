package net.mbonnin.vespene.lib

import com.squareup.moshi.JsonClass
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/* Nexus service definition based on:
 * - Nexus STAGING API: https://oss.sonatype.org/nexus-staging-plugin/default/docs/index.html
 * - Nexus CORE API: https://repository.sonatype.org/nexus-restlet1x-plugin/default/docs/index.html
 * - Staging upload: https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API
 * - https://github.com/Codearte/gradle-nexus-staging-plugin
 * - https://github.com/marcphilipp/nexus-publish-plugin
 *
 * OSSRH is using a 2.x version of Nexus so a lot of the more recent 3.x docs do not apply
 */
interface NexusStagingApi {
  @GET("staging/profile_repositories")
  suspend fun getRepositories(): Response<Data<List<Repository>>>

  @GET("staging/repository/{repositoryId}")
  suspend fun getRepository(@Path("repositoryId") repositoryId: String): Response<Repository>

  @POST("staging/bulk/close")
  suspend fun closeRepositories(@Body input: Data<TransitionRepositoryInput>): Response<Unit>

  @POST("staging/bulk/promote")
  suspend fun releaseRepositories(@Body input: Data<TransitionRepositoryInput>): Response<Unit>

  @POST("staging/bulk/drop")
  suspend fun dropRepositories(@Body input: Data<TransitionRepositoryInput>): Response<Unit>

  @GET("staging/profiles")
  suspend fun getProfiles(): Response<Data<List<Profile>>>

  @POST("staging/profiles/{stagingProfileId}/start")
  suspend fun createRepository(
    @Path("stagingProfileId") stagingProfileId: String,
    @Body description: Data<Description>
  ): Response<Data<CreatedRepository>>
}

@JsonClass(generateAdapter = true)
class Data<T>(val data: T)

@JsonClass(generateAdapter = true)
class Profile(val id: String, val name: String)

@JsonClass(generateAdapter = true)
class Description(val description: String)

@JsonClass(generateAdapter = true)
class CreatedRepository(var stagedRepositoryId: String)

@JsonClass(generateAdapter = true)
class Repository(val repositoryId: String, val transitioning: Boolean, val type: String)

@JsonClass(generateAdapter = true)
class TransitionRepositoryInput(val stagedRepositoryIds: List<String>, val autoDropAfterRelease: Boolean? = null)

fun NexusApi(
  username: String,
  password: String,
  baseUrl: String = "https://oss.sonatype.org/service/local/",
) = NexusApi(OkHttpClient(username, password), baseUrl)

fun NexusApi(
  okHttpClient: OkHttpClient,
  baseUrl: String = "https://oss.sonatype.org/service/local/",
): NexusStagingApi {
  val retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(okHttpClient)
    .addConverterFactory(MoshiConverterFactory.create())
    .build()

  return retrofit.create(NexusStagingApi::class.java)
}

fun OkHttpClient(username: String, password: String) = OkHttpClient.Builder().addInterceptor { chain ->
  val builder = chain.request().newBuilder()
  builder.addHeader("Authorization", Credentials.basic(username, password))
  builder.addHeader("Accept", "application/json")
  builder.addHeader("Content-Type", "application/json")
  builder.addHeader("User-Agent", "vespene")
  chain.proceed(builder.build())
}
  .readTimeout(600, TimeUnit.SECONDS) // Opening a staging repository can take 2-3 minutes
  .connectTimeout(600, TimeUnit.SECONDS)
  .build()