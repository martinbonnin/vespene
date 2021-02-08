package net.mbonnin.vespene

import okio.BufferedSource
import okio.ByteString.Companion.toByteString
import java.security.MessageDigest

/**
 *
 */
private fun BufferedSource.digest(name: String): String {
  val md = MessageDigest.getInstance(name)

  val scratch = ByteArray(1024)
  var read: Int = read(scratch)
  while (read > 0) {
    md.update(scratch, 0, read)
    read = read(scratch)
  }

  val digest = md.digest()

  return digest.toByteString().hex()
}

fun BufferedSource.md5() = digest("MD5")
fun BufferedSource.sha1() = digest("SHA1")
