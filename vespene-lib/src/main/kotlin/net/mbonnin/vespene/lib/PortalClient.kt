package net.mbonnin.vespene.lib

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.InputStream


/**
 * A Client that uploads a zip file to the Maven Central portal using the publisher API
 *
 * See https://central.sonatype.org/publish/publish-portal-api/
 *
 * @param username
 * @param password
 */
class PortalClient(
  private val username: String,
  private val password: String,
) {
  /**
   * @param zip a function that returns an input stream containing a zip with the publication contents.
   * [zip] may be called several times if the request has to be retried or if [zipLength] is -1.
   * The [InputStream] returned by [zip] is always closed.
   *
   * @param zipLength the size of the zip as it is sent over the wire.
   *
   * @param publicationName a display name for this publication.
   *
   * @param publicationType whether to release the publication automatically or not.
   */
  fun upload(zip: () -> InputStream, zipLength: Long = -1, publicationName: String, publicationType: PublicationType = PublicationType.AUTOMATIC) {
    check(username.isNotBlank()) {
      "username must not be empty"
    }
    check(password.isNotBlank()) {
      "password must not be empty"
    }

    val token = "$username:$password".let {
      Buffer().writeUtf8(it).readByteString().base64()
    }
    val zipBody = object: RequestBody() {
      override fun contentLength(): Long {
        return zipLength
      }

      override fun contentType(): MediaType {
        return "application/zip".toMediaType()
      }

      override fun writeTo(sink: BufferedSink) {
        zip().source().use {
          sink.writeAll(it)
        }
      }
    }

    val multipartBody = MultipartBody.Builder()
      .addFormDataPart(
        "bundle",
        publicationName,
        zipBody
      )
      .build()

    Request.Builder()
      .post(multipartBody)
      .addHeader("Authorization", "UserToken $token")
      .url("https://central.sonatype.com/api/v1/publisher/upload?publishingType=${publicationType.name}")
      .build()
      .let {
        OkHttpClient.Builder()
          .build()
          .newCall(it).execute()
      }.use {
        if (!it.isSuccessful) {
          error("Cannot publish to maven central (status='${it.code}'): ${it.body?.string()}")
        }
      }
  }

  fun upload(zip: File, publicationName: String, publicationType: PublicationType = PublicationType.AUTOMATIC) {
    upload(
      zip = { zip.inputStream() },
      zipLength =  zip.length(),
      publicationName = publicationName,
      publicationType = publicationType
    )
  }
}

enum class PublicationType {
  USER_MANAGED,
  AUTOMATIC
}