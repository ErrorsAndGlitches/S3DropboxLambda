package com.s3dropbox.lambda

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import com.typesafe.scalalogging.LazyLogging
import org.json4s.{Formats, NoTypeHints}
import org.json4s.jackson.Serialization
import org.springframework.beans.factory.annotation.Autowired
import DbxFiles._
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/**
 * A DbxPdfFiles is used to update Dropbox with PDF files.
 */
@Component
@Lazy
case class DbxFiles(@Autowired var dbx: DbxClientV2) extends LazyLogging {
  implicit val formats: Formats = JsonFormat

  def uploadPdf(pdfEntry: ZipFileEntry): Unit = {
    logger.info(s"Uploading PDF file to Dbx: ${pdfEntry.filename}")
    dbx.files()
      .uploadBuilder(s"/${pdfEntry.filename}")
      .withMode(WriteMode.OVERWRITE)
      .uploadAndFinish(new ByteArrayInputStream(pdfEntry.data))
  }

  def deletePdf(texFilename: String): Unit = {
    val pdfFilename: String = "\\.tex$".r.replaceFirstIn(texFilename, ".pdf")
    logger.info(s"Deleting PDF file from Dbx: $pdfFilename")
    dbx.files().deleteV2(s"/$pdfFilename")
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
}

object DbxFiles {
  val JsonFormat: Formats = org.json4s.DefaultFormats + NoTypeHints
  val ManifestFileName: String = "/.manifest"
}