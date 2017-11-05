package com.s3dropbox.lambda

import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry

/**
  * DossierArtifacts pairs the LaTeX and PDF artifacts.
  */
case class DossierArtifacts(zipFileIterator: ZipFileIterator) {

  lazy val artifacts: List[DossierArtifact] = {
    zipFileIterator
      .toList
      .sortBy(_.filename)
      .grouped(2)
      .map((artifact: List[ZipFileEntry]) => artifact match {
        case pdfEntry :: texEntry :: Nil =>
          if (!isValidPair(pdfEntry.filename, texEntry.filename)) {
            throw new IllegalStateException(s"${pdfEntry.filename} and ${texEntry.filename} is not a valid pair")
          }

          DossierArtifact(pdfEntry, texEntry)
        case _ => throw new IllegalStateException(s"Unknown PDF/LaTeX pairing: $artifact")
      })
      .toList
  }

  private def isValidPair(pdfFileName: String, texFileName: String): Boolean = {
    hasSameBase(pdfFileName, texFileName) && isPdf(pdfFileName) && isTex(texFileName)
  }

  private def hasSameBase(pdfFileName: String, texFileName: String): Boolean = {
    filebase(pdfFileName) == filebase(texFileName)
  }

  private def filebase(filename: String): String = {
    filename.split('.').reverse.tail.head
  }

  private def isPdf(filename: String): Boolean = checkExtension(filename, DossierArtifacts.PdfExtension)

  private def isTex(filename: String): Boolean = checkExtension(filename, DossierArtifacts.TexExtension)

  private def checkExtension(filename: String, expectedExt: String): Boolean = {
    filename.split('.').reverse.headOption match {
      case Some(ext) => ext == expectedExt
      case None => false
    }
  }
}

case class DossierArtifact(pdfFile: ZipFileEntry, texFile: ZipFileEntry)

private object DossierArtifacts {
  val PdfExtension: String = "pdf"
  val TexExtension: String = "tex"
}
