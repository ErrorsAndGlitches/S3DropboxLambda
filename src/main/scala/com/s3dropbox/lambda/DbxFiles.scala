package com.s3dropbox.lambda

import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files._
import com.s3dropbox.lambda.DbxFiles._
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import com.typesafe.scalalogging.LazyLogging
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

import java.io.{ByteArrayInputStream, File}
import java.nio.charset.StandardCharsets
import java.time.Duration
import scala.collection.JavaConverters.{asScalaBufferConverter, seqAsJavaListConverter}
import scala.collection.mutable

/**
 * A DbxPdfFiles is used to update Dropbox with PDF files.
 */
@Component
@Lazy
case class DbxFiles(@Autowired var dbx: DbxClientV2) extends LazyLogging {
  implicit val formats: Formats = JsonFormat

  def uploadPdfs(pdfEntries: List[ZipFileEntry]): Unit = {
    dbx.files.uploadSessionFinishBatch(
      pdfEntries.map(entry => {
        logger.info(s"Uploading PDF file to Dbx: ${entry.filename}")
        val uploadResult = dbx.files.uploadSessionStart(true).uploadAndFinish(new ByteArrayInputStream(entry.data))
        new UploadSessionFinishArg(
          new UploadSessionCursor(uploadResult.getSessionId, entry.data.length),
          CommitInfo.newBuilder(s"/${entry.filename}").withMode(WriteMode.OVERWRITE).build()
        )
      }).asJava
    )
    logger.info(s"Finished uploading PDF files to Dbx")
  }

  def deletePdfs(pdfFilenames: List[String]): Unit = {
    // deletion must be done serially due to deletion of folders logic
    pdfFilenames.foreach(deletePdf)
  }

  def uploadManifest(manifest: Manifest): Unit = {
    logger.info(s"Uploading manifest to Dbx: ${Serialization.write(manifest)}")
    dbx.files()
      .uploadBuilder(s"$ManifestFileName")
      .withMode(WriteMode.OVERWRITE)
      .uploadAndFinish(new ByteArrayInputStream(
        Serialization.write(manifest).getBytes(StandardCharsets.UTF_8)
      ))
  }

  private def deletePdf(pdfFilename: String): Unit = {
    logger.info(s"Deleting PDF file from Dbx: $pdfFilename")
    val fullPath = s"/$pdfFilename"
    dbx.files().deleteV2(fullPath)
    deleteFolderRecursive(new File(fullPath).getParent)
  }

  private def deleteFolderRecursive(folder: String): Unit = {
    if (folder.equals(RootFolder)) {
      logger.info("Not deleting root folder");
    } else if (allFolderContents(folder).isEmpty) {
      logger.info(s"Deleting folder in Dbx: $folder")
      dbx.files().deleteV2(folder)
      deleteFolderRecursive(new File(folder).getParent)
    } else {
      logger.info(s"Folder contains contents. Not deleting: $folder")
    }
  }

  private def allFolderContents(folder: String): List[Metadata] = {
    val entries: mutable.ArrayBuffer[Metadata] = mutable.ArrayBuffer[Metadata]()

    var hasMore: Boolean = true
    while (hasMore) {
      val result: ListFolderResult = dbx.files().listFolder(folder)
      hasMore = result.getHasMore
      result.getEntries.asScala.foreach(entries.append(_))
    }

    entries.toList
  }
}

object DbxFiles {
  val JsonFormat: Formats = org.json4s.DefaultFormats + NoTypeHints
  val ManifestFileName: String = "/.manifest"
  val RootFolder: String = "/"
  val UploadWaitTime: Duration = Duration.ofMinutes(10L)
}