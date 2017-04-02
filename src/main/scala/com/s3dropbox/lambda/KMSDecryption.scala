package com.s3dropbox.lambda

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DecryptRequest, DecryptResult}
import com.amazonaws.util.Base64

/**
  * KMSDecryption decrypts cipher text encrypted by KMS.
  */
class KMSDecryption(kmsClient: AWSKMS) {

  def decrypt(cipherText: String): String = {
    val request: DecryptRequest =
      new DecryptRequest()
        .withCiphertextBlob(ByteBuffer.wrap(Base64.decode(cipherText)))

    val response: DecryptResult = kmsClient.decrypt(request)
    new String(response.getPlaintext.array(), StandardCharsets.UTF_8)
  }
}
