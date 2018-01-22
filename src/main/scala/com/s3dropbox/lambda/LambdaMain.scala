package com.s3dropbox.lambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.Locale

import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.{DbxUserFilesRequests, DownloadErrorException, WriteMode}
import com.s3dropbox.lambda.LambdaMain._
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import com.typesafe.scalalogging.Logger
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.{Formats, NoTypeHints}

/**
  * LambdaMain is the AWS Lambda entry point. This AWS Lambda function reacts off S3 notifications. The file is expected
  * to be a zip file, which is unpacked and then the individual files are published to Dropbox.
  *
  * One assumption of this project is that all the PDFs and thus all the LaTeX files are unique.
  */
final class LambdaMain extends RequestHandler[S3Event, Unit] {

  implicit val formats: Formats = LambdaMain.JsonFormat
  private val logger: Logger = Logger[LambdaMain]

  override def handleRequest(event: S3Event, context: Context): Unit = {
    val dbxFileReqs: DbxUserFilesRequests = dbxClientV2(context).files()

    val s3entity: S3Entity = event.getRecords.get(event.getRecords.size - 1).getS3
    val curManifest: Manifest = manifest(dbxFileReqs)

    withS3ZipFile(s3entity, (zipFileIter: ZipFileIterator) => {
      val dossierArtifacts: DossierArtifacts = DossierArtifacts(zipFileIter)

      // check for updates
      val withUpdatesManifest: Manifest = dossierArtifacts
        .artifacts
        .filter((artifact: DossierArtifact) => {
          curManifest.requiresUpdate(artifact.texFile.filename, artifact.texFile.data)
        })
        .foldLeft(curManifest)((mani: Manifest, artifact: DossierArtifact) => {
          uploadPdfFile(dbxFileReqs, artifact.pdfFile)
          mani.updateFileState(artifact.texFile.filename, artifact.texFile.data)
        })

      // check for deletes
      val newManifest: Manifest = withUpdatesManifest.fileStates
        .fileStates
        .filter((fileState: FileState) => !dossierArtifacts.containsTexFile(fileState.filename))
        .foldLeft(withUpdatesManifest)((mani: Manifest, fileState: FileState) => {
          deletePdfFile(dbxFileReqs, fileState.filename)
          mani.removeFileState(fileState.filename)
        })

      if (curManifest != newManifest) {
        uploadManifest(dbxFileReqs, newManifest)
      }
    })
  }

  private def dbxClientV2(context: Context): DbxClientV2 = {
    val kmsDecryption: KMSDecryption = new KMSDecryption(AWSKMSClientBuilder.defaultClient())
    val dbxToken: String = kmsDecryption.decrypt(sys.env(EncryptedDbTokenEnvVar))

    val config: DbxRequestConfig =
      DbxRequestConfig
        .newBuilder(context.getFunctionName)
        .withAutoRetryEnabled()
        .withUserLocaleFrom(Locale.US)
        .build()

    new DbxClientV2(config, dbxToken)
  }

  private def withS3ZipFile(s3entity: S3Entity, callback: ZipFileIterator => Unit): Unit = {
    val s3ObjectStream: S3ObjectInputStream =
      AmazonS3ClientBuilder
        .defaultClient()
        .getObject(s3entity.getBucket.getName, s3entity.getObject.getKey)
        .getObjectContent

    val zipFileIter: ZipFileIterator = new ZipFileIterator(s3ObjectStream)
    try {
      callback(zipFileIter)
    }
    finally {
      zipFileIter.close()
    }
  }

  private def manifest(dbxFilesReqs: DbxUserFilesRequests): Manifest = {
    try {
      dbxManifest(dbxFilesReqs)
    }
    catch {
      case _: DownloadErrorException =>
        // manifest doesn't exist in Dbx, create a new one
        Manifest(FileStates(List[FileState]()))
    }
  }

  private def dbxManifest(dbxFilesReqs: DbxUserFilesRequests): Manifest = {
    val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    try {
      dbxFilesReqs
        .download(LambdaMain.ManifestFileName)
        .download(outputStream)
    }
    finally {
      outputStream.close()
    }

    JsonMethods
      .parse(outputStream.toString(StandardCharsets.UTF_8.toString))
      .extract[Manifest]
  }

  private def uploadPdfFile(dbxFilesReqs: DbxUserFilesRequests, pdfEntry: ZipFileEntry): Unit = {
    logger.info(s"Uploading PDF file to Dbx: ${pdfEntry.filename}")
    dbxFilesReqs
      .uploadBuilder(s"/${pdfEntry.filename}")
      .withMode(WriteMode.OVERWRITE)
      .uploadAndFinish(new ByteArrayInputStream(pdfEntry.data))
  }

  def deletePdfFile(dbxFileReqs: DbxUserFilesRequests, texFilename: String): Unit = {
    val pdfFilename: String = "\\.tex$".r.replaceFirstIn(texFilename, ".pdf")
    logger.info(s"Deleting PDF file from Dbx: $pdfFilename")
    dbxFileReqs
      .delete(s"/$pdfFilename")
  }

  private def uploadManifest(dbxFilesReqs: DbxUserFilesRequests, manifest: Manifest): Unit = {
    logger.info(s"Uploading manifest to Dbx: ${Serialization.write(manifest).toString}")
    dbxFilesReqs
      .uploadBuilder(s"${LambdaMain.ManifestFileName}")
      .withMode(WriteMode.OVERWRITE)
      .uploadAndFinish(new ByteArrayInputStream(
        Serialization.write(manifest).toString.getBytes(StandardCharsets.UTF_8)
      ))
  }
}

object LambdaMain {
  val EncryptedDbTokenEnvVar: String = "EncryptedDropboxToken"
  val ManifestFileName: String = "/.manifest"
  val JsonFormat: Formats = org.json4s.DefaultFormats + NoTypeHints
}
