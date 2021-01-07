package com.s3dropbox.lambda

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.s3dropbox.lambda.LambdaMain._
import com.typesafe.scalalogging.LazyLogging

/**
 * LambdaMain is the AWS Lambda entry point. This AWS Lambda function reacts off S3 notifications. The file is expected
 * to be a zip file, which contains a hierarchy of PDFs and a manifest file. The manifest file is used to compare
 * what is in Dropbox and what is published as part of the payload.
 */
class LambdaMain extends RequestHandler[S3Event, Unit] with LazyLogging {
  override def handleRequest(event: S3Event, context: Context): Unit = {
    val s3entity: S3Entity = event.getRecords.get(event.getRecords.size - 1).getS3
    DossierMain.runDossier(DossierCliOpts(
      sys.env(DbxCredentialS3BucketEnvVar),
      sys.env(DbxCredentialS3KeyEnvVar),
      sys.env(DbxCredentialKmsIdEnvVar),
      s3entity.getBucket.getName,
      s3entity.getObject.getKey
    ))
  }
}

object LambdaMain {
  val DbxCredentialS3BucketEnvVar: String = "DBX_CREDENTIAL_S3_BUCKET"
  val DbxCredentialS3KeyEnvVar: String = "DBX_CREDENTIAL_S3_KEY"
  val DbxCredentialKmsIdEnvVar: String = "DBX_CREDENTIAL_KMS_ID"
}