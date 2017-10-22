package com.s3dropbox.lambda

import java.nio.file.attribute.FileTime
import java.util.Date

import com.dropbox.core.v2.files.{DbxUserFilesRequests, FileMetadata, ListFolderResult, Metadata}

/**
  * DbxFileStatus is used to determine whether a given PDF needs to be updated in Dropbox by comparing the modified
  * date of the PDF to the PDF in Dropbox.
  */
case class DbxFileStatus(private val folder: String, private val dbxFilesReqs: DbxUserFilesRequests) {

  private lazy val dbxFiles: List[FileMetadata] = {
    val list = new DbxFileIterator(
      dbxFilesReqs,
      dbxFilesReqs.listFolder(folder)
    ).toList

    list
  }

  def shouldUpdate(filename: String, fileTime: FileTime): Boolean = {
    dbxFiles
      .filter((metadata: FileMetadata) => {
        metadata.getName.equals(filename)
      })
      .map((metadata: FileMetadata) => {
        metadata
          .getServerModified
          .before(Date.from(fileTime.toInstant))
      })
      .headOption
      .getOrElse(true)
  }
}

object DbxFileStatus {
  val ROOT_FOLDER: String = ""
}

private class DbxFileIterator(dbxFiles: DbxUserFilesRequests, listResult: ListFolderResult) extends Iterator[FileMetadata] {

  private var currentResult: ListFolderResult = listResult
  private var currentEntries: java.util.List[Metadata] = currentResult.getEntries
  private var currentIndex: Int = 0

  override def hasNext: Boolean = {
    if (currentIndex == currentEntries.size() && currentResult.getHasMore) {
      currentResult = dbxFiles.listFolderContinue(currentResult.getCursor)
      currentEntries = currentResult.getEntries
      currentIndex = 0
    }

    currentIndex < currentEntries.size()
  }

  override def next(): FileMetadata = {
    val pdfMetadata: Metadata = currentEntries.get(currentIndex)
    currentIndex += 1
    dbxFiles
      .listRevisions(pdfMetadata.getPathLower, PdfIterator.LATEST_REVISION)
      .getEntries
      .get(0)
  }
}

private object PdfIterator {
  val LATEST_REVISION: Long = 1L
}
