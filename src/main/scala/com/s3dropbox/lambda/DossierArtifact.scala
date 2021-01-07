package com.s3dropbox.lambda

import com.s3dropbox.lambda.DossierArtifact.{ManifestFilename, PdfSuffix}
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

/**
 * DossierArtifacts encapsulates a set of PDF artifacts and their corresponding Manifest.
 */
case class DossierArtifact(zipFileIterator: ZipFileIterator) extends LazyLogging {

  lazy val dossierZipFileContents: DossierZipFileContents = {
    val pdfFiles: mutable.ListBuffer[ZipFileEntry] = mutable.ListBuffer[ZipFileEntry]()
    var manifest: ZipFileEntry = null
    zipFileIterator.foreach(entry => {
      if (entry.filename.endsWith(PdfSuffix)) {
        pdfFiles.append(entry)
      }
      if (entry.filename == ManifestFilename) {
        manifest = entry
      }
    })

    if (manifest == null) {
      throw new IllegalStateException("Did not find '.manifest' file in zip file")
    }

    DossierZipFileContents(pdfFiles.toList, Manifest(manifest.data))
  }

  def filenamesToRemove(oldManifest: Manifest): List[String] = dossierZipFileContents.manifest.filenamesToRemove(oldManifest)

  def zipFileEntriesToUpdate(oldManifest: Manifest): List[ZipFileEntry] = {
    val filesToUpdate: List[String] = dossierZipFileContents
      .manifest
      .filesToUpdate(oldManifest)
      .map(_.filename)

    dossierZipFileContents
      .pdfFiles
      .filter(entry => filesToUpdate.contains(entry.filename))
  }

  def close(): Unit = {
    try {
      zipFileIterator.close()
    } catch {
      case e: Exception => logger.error("Unable to close zip file iterator", e)
    }
  }
}

object DossierArtifact {
  val PdfSuffix = "pdf"
  val ManifestFilename = ".manifest"
}

case class DossierZipFileContents(pdfFiles: List[ZipFileEntry], manifest: Manifest)
