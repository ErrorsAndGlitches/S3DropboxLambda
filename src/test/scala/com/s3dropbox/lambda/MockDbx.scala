package com.s3dropbox.lambda

import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}
import java.util.Date

import com.dropbox.core.v2.files._
import org.mockito.ArgumentMatchers

import scala.collection.JavaConverters._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatestplus.mockito.MockitoSugar

/**
  * MockDbx is used to mock the Dbx API.
  */
class MockDbx(fileLists: List[List[FileData]]) extends MockitoSugar {

  private val dbxUserFilesRequests: DbxUserFilesRequests = mock[DbxUserFilesRequests]

  def dbxUserFileRequests(): DbxUserFilesRequests = {
    // this function requires special casing the first and the last. The last because the result should indicate that
    // continuation is false. The first, because only the call to DbxUserFilesRequests.listFolder() is valid.
    fileLists match {
      case head :: Nil =>
        new ListFolderMock(dbxUserFilesRequests, head, false, cursor(0)).setupListFolder()
      case head :: tail =>
        val nextIndex: Int = mockListsTail(tail)
        new ListFolderMock(dbxUserFilesRequests, head, true, cursor(nextIndex)).setupListFolder()
      case _ => throw new IllegalStateException(s"Unrecognized List[List[FileData]]: $fileLists")
    }

    fileLists.foreach(setupListRevisionResult)

    dbxUserFilesRequests
  }

  private def mockListsTail(tail: List[List[FileData]]): Int = {
    val nextIndex: Int = tail
      .reverse
      .foldLeft((0, false)) {
        (listData: (Int, Boolean), files: List[FileData]) => {
          new ListFolderMock(
            dbxUserFilesRequests,
            files,
            listData._2,
            cursor(listData._1)
          ).setupListFolderContinue(cursor(listData._1 - 1))

          (listData._1 + 1, true)
        }
      }
      ._1

    nextIndex - 1
  }

  private def setupListRevisionResult(files: List[FileData]): Unit = {
    files.foreach((fileData: FileData) => {
      when(dbxUserFilesRequests.listRevisions(ArgumentMatchers.eq(fileData.pathLower)))
        .thenReturn(
          new ListRevisionsResult(
            false,
            List(
              new FileMetadata(
                fileData.filename,
                MockDbx.FILE_ID,
                MockDbx.CLIENT_MOD_DATE,
                fileData.serverModTime,
                MockDbx.REV_STRING,
                MockDbx.FILE_SIZE
              )
            ).asJava
          )
        )
    })
  }

  private def cursor(index: Int): String = s"cursor_$index"
}

private class ListFolderMock(dbxUserFilesRequests: DbxUserFilesRequests, files: List[FileData],
                             doesContinue: Boolean, currentCursor: String) {

  def setupListFolderContinue(nextCursor: String): Unit = {
    when(dbxUserFilesRequests.listFolderContinue(nextCursor))
      .thenReturn(listFolderResult())
  }

  def setupListFolder(): Unit = {
    when(dbxUserFilesRequests.listFolder(anyString()))
      .thenReturn(listFolderResult())
  }

  private def listFolderResult(): ListFolderResult = {
    new ListFolderResult(
      files.map(filedata => new Metadata(filedata.filename, filedata.pathLower, filedata.filename, null)).asJava,
      currentCursor,
      doesContinue
    )
  }
}

object MockDbx {
  private val CLIENT_MOD_DATE: Date = date(2004, 5, 20)
  private val FILE_ID: String = "squamish_bouldering_rocks"
  private val REV_STRING: String = "0123456789"
  private val FILE_SIZE: Long = 42

  def date(year: Int, month: Int, day: Int): Date = Date.from(instant(year, month, day))

  def instant(year: Int, month: Int, day: Int): Instant =
    ZonedDateTime.of(LocalDateTime.of(year, month, day, 0, 0), ZoneOffset.UTC).toInstant
}

private[lambda] case class FileData(filename: String, serverModTime: Date) {
  def pathLower: String = s"/$filename"
}
