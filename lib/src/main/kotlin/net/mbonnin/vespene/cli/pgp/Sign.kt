package net.mbonnin.vespene

import net.mbonnin.vespene.cli.pgp.armor
import okio.BufferedSource
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRing
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider

/**
 * Creates a PGP signature from the given BufferedSource
 *
 * Heavily inspired by https://github.com/gradle/gradle/blob/124712713a77a6813e112ae1b68f248deca6a816/subprojects/security/src/main/java/org/gradle/plugins/signing/signatory/pgp/PgpSignatory.java
 */
fun BufferedSource.sign(key: String, keyPassword: String): String {
  val inputStream = PGPUtil.getDecoderStream(key.byteInputStream())
  val secretKey: PGPSecretKey = JcaPGPSecretKeyRing(inputStream).secretKey
  val decryptor = BcPBESecretKeyDecryptorBuilder(BcPGPDigestCalculatorProvider()).build(keyPassword.toCharArray())
  val privateKey = secretKey.extractPrivateKey(decryptor)

  val generator = PGPSignatureGenerator(BcPGPContentSignerBuilder(secretKey.publicKey.algorithm, PGPUtil.SHA512))
  generator.init(PGPSignature.BINARY_DOCUMENT, privateKey)

  val scratch = ByteArray(1024)
  var read: Int = read(scratch)
  while (read > 0) {
    generator.update(scratch, 0, read)
    read = read(scratch)
  }

  generator.update(readByteArray())
  val signature = generator.generate()

  return armor {
    signature.encode(it)
  }
}