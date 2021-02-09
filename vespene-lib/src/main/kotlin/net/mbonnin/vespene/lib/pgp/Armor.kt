package net.mbonnin.vespene.lib.pgp

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.BCPGOutputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

fun armor(block: (OutputStream) -> Unit): String {
  val os = ByteArrayOutputStream()
  val bufferedOutput = BCPGOutputStream(ArmoredOutputStream(os))

  block(bufferedOutput)
  bufferedOutput.flush()
  bufferedOutput.close()

  return String(os.toByteArray())
}