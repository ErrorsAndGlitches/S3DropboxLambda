package com.s3dropbox.lambda

import java.util.Locale

import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity
import com.amazonaws.services.s3.model.S3Object
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.s3dropbox.lambda.LambdaMain._
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream

/**
  * LambdaMain is the AWS Lambda entry point. This AWS Lambda function reacts off S3 notifications. The file is expected
  * to be a zip file, which is unpacked and then the individual files are published to Dropbox.
  */
final class LambdaMain extends RequestHandler[S3Event, Unit] {

  override def handleRequest(event: S3Event, context: Context): Unit = {
    val kmsDecryption: KMSDecryption = new KMSDecryption(AWSKMSClientBuilder.defaultClient())
    val dbxToken: String = kmsDecryption.decrypt(sys.env(EncryptedDbTokenEnvVar))

    val config: DbxRequestConfig =
      DbxRequestConfig
        // the clientIdentifier might be the app key...
        .newBuilder(context.getFunctionName)
        .withAutoRetryEnabled()
        .withUserLocaleFrom(Locale.US)
        .build()

    val dbxClient: DbxClientV2 = new DbxClientV2(config, dbxToken)

    val s3entity: S3Entity = event.getRecords.get(event.getRecords.size - 1).getS3
    val s3obj: S3Object = AmazonS3ClientBuilder
      .defaultClient()
      .getObject(s3entity.getBucket.getName, s3entity.getObject.getKey)

    val zipFileIter: ZipFileIterator = new ZipFileIterator(s3obj.getObjectContent)
    zipFileIter.foreach((zentry: ZipFileEntry) => {
      println(s"Uploading [${zentry.filename}] to Dropbox")
      dbxClient
        .files()
        .uploadBuilder(s"/${zentry.filename}")
        .uploadAndFinish(new ByteInputStream(zentry.data, zentry.data.length))
    })
    zipFileIter.close()
  }
}

private object LambdaMain {
  var EncryptedDbTokenEnvVar: String = "EncryptedDropboxToken"
}
