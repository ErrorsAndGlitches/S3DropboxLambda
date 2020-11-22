package com.s3dropbox.lambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.Locale

import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, SSEAwsKeyManagementParams}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DownloadErrorException
import com.s3dropbox.lambda.DbxFiles.ManifestFileName
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Named
import org.json4s.jackson.JsonMethods
import org.json4s.{Formats, NoTypeHints}
import org.springframework.context.annotation.{Bean, Configuration, Lazy}

/**
 * Contains Spring beans.
 */
@Configuration
@Lazy
class DossierConfig extends LazyLogging {
  implicit val formats: Formats = DossierConfig.JsonFormat

  @Bean
  def dbxCredential(
    s3: AmazonS3,
    @Named("dbxCredBucket") dbxCredBucket: String,
    @Named("dbxCredKey") dbxCredKey: String,
    @Named("kmsId") kmsId: String
  ): DbxCredential = {
    logger.info("Fetching Dropbox credentials from S3")
    val creds: DbxCredential = DbxCredential.Reader.readFully(
      s3.getObject(dbxCredBucket, dbxCredKey).getObjectContent
    )

    logger.info("Refreshing Dropbox credentials")
    creds.refresh(dbxRequestConfig)

    val newCredBytes = new ByteArrayOutputStream()
    DbxCredential.Writer.writeToStream(creds, newCredBytes)
    logger.info("Updating Dropbox credentials in S3")
    s3.putObject(
      new PutObjectRequest(
        dbxCredBucket,
        dbxCredKey,
        new ByteArrayInputStream(newCredBytes.toByteArray),
        new ObjectMetadata()
      ).withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams().withAwsKmsKeyId(kmsId))
    )

    creds
  }

  @Bean
  def dbxClient(dbxCred: DbxCredential): DbxClientV2 = new DbxClientV2(dbxRequestConfig, dbxCred)

  @Bean
  def dbxRequestConfig: DbxRequestConfig = DbxRequestConfig
    .newBuilder(getClass.getSimpleName)
    .withAutoRetryEnabled()
    .withUserLocaleFrom(Locale.US)
    .build()

  @Bean
  def s3Client(@Named("awsRegion") region: String): AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).build()

  @Bean
  def dbxManifest(dbx: DbxClientV2): Manifest = {
    try {
      dbxManifestDownload(dbx)
    }
    catch {
      case _: DownloadErrorException =>
        // manifest doesn't exist in Dbx, create a new one
        Manifest()
    }
  }

  private def dbxManifestDownload(dbx: DbxClientV2): Manifest = {
    val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    try {
      dbx.files()
        .download(ManifestFileName)
        .download(outputStream)
    }
    finally {
      outputStream.close()
    }

    JsonMethods
      .parse(outputStream.toString(StandardCharsets.UTF_8.toString))
      .extract[Manifest]
  }
}

object DossierConfig {
  val JsonFormat: Formats = org.json4s.DefaultFormats + NoTypeHints
}