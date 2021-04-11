package com.s3dropbox.lambda

import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files._
import com.s3dropbox.lambda.DbxFilesSpec.{DbxFileMetadata, DbxSessionId, PdfFileContents, PdfFileName, PdfZipFileEntry}
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.io.InputStream
import java.util.{Collections, Date}
import scala.collection.JavaConverters.seqAsJavaListConverter

class DbxFilesSpec extends AnyFunSpec with MockitoSugar with Matchers {

  describe("when an upload request is made") {
    it("should overwrite the existing file") {
      val mockUploadResult = mock[UploadSessionStartResult]
      when(mockUploadResult.getSessionId).thenReturn(DbxSessionId)

      val mockSessionUploader = mock[UploadSessionStartUploader]
      when(mockSessionUploader.uploadAndFinish(any())).thenReturn(mockUploadResult)

      val dbxFiles: DbxUserFilesRequests = mock[DbxUserFilesRequests]
      when(dbxFiles.uploadSessionStart(true)).thenReturn(mockSessionUploader)

      DbxFiles(mockDbxClient(dbxFiles)).uploadPdfs(List(PdfZipFileEntry, PdfZipFileEntry))

      val capturedBatchRequest: ArgumentCaptor[java.util.List[UploadSessionFinishArg]] = ArgumentCaptor.forClass(
        classOf[java.util.List[UploadSessionFinishArg]]
      )
      verify(dbxFiles).uploadSessionFinishBatch(capturedBatchRequest.capture())
      val expectedSessionFinishResult = new UploadSessionFinishArg(
        new UploadSessionCursor(DbxSessionId, PdfFileContents.length),
        CommitInfo.newBuilder("/clutch-doc.pdf").withMode(WriteMode.OVERWRITE).build()
      )
      capturedBatchRequest.getValue shouldBe List(expectedSessionFinishResult, expectedSessionFinishResult).asJava
    }
  }

  describe("when a delete request is made") {
    it("should delete the file") {
      val dbxFiles: DbxUserFilesRequests = mock[DbxUserFilesRequests]
      DbxFiles(mockDbxClient(dbxFiles)).deletePdfs(List(PdfFileName))
      verify(dbxFiles).deleteV2("/clutch-doc.pdf")
    }
  }

  describe("when a delete file request is made for a file in a folder") {
    it("should delete the file and delete the folder when it is empty") {
      val dbxFiles: DbxUserFilesRequests = mock[DbxUserFilesRequests]
      when(dbxFiles.listFolder("/folder")).thenReturn(new ListFolderResult(Collections.emptyList(), "cursor", false))
      DbxFiles(mockDbxClient(dbxFiles)).deletePdfs(List(s"folder/$PdfFileName"))
      verify(dbxFiles).deleteV2("/folder/clutch-doc.pdf")
      verify(dbxFiles).deleteV2("/folder")
    }
  }

  describe("when a delete file request is made for a non-empty folder and multiple list folder requests are needed") {
    it("should delete the file and not delete the folder") {
      val dbxFiles: DbxUserFilesRequests = mock[DbxUserFilesRequests]
      when(dbxFiles.listFolder("/folder")).thenReturn(
        new ListFolderResult(Collections.emptyList(), "cursor", true),
        new ListFolderResult(Collections.singletonList(new Metadata("/folder/other-clutch-doc.pdf")), "cursor", false)
      )
      DbxFiles(mockDbxClient(dbxFiles)).deletePdfs(List(s"folder/$PdfFileName"))
      verify(dbxFiles).deleteV2("/folder/clutch-doc.pdf")
      verify(dbxFiles, never()).deleteV2("/folder")
    }
  }

  describe("when a manifest upload request is made") {
    it("should overwrite the existing manifest") {
      val uploadBuilder = mockUploadBuilder

      val uploadedContents: Array[Byte] = new Array[Byte](1000)
      when(uploadBuilder.uploadAndFinish(any())).thenAnswer(new Answer[FileMetadata] {
        override def answer(invocation: InvocationOnMock): FileMetadata = {
          invocation.getArgument(0).asInstanceOf[InputStream].read(uploadedContents) shouldBe 58
          DbxFileMetadata
        }
      })

      val dbxFiles: DbxUserFilesRequests = mock[DbxUserFilesRequests]
      when(dbxFiles.uploadBuilder("/.manifest")).thenReturn(uploadBuilder)

      DbxFiles(mockDbxClient(dbxFiles)).uploadManifest(Manifest(List(FileState("filename", "md5sum"))))
      verify(uploadBuilder).withMode(WriteMode.OVERWRITE)
    }
  }

  private def mockDbxClient(dbxFiles: DbxUserFilesRequests): DbxClientV2 = {
    val dbx: DbxClientV2 = mock[DbxClientV2]
    when(dbx.files()).thenReturn(dbxFiles)
    dbx
  }

  private def mockUploadBuilder: UploadBuilder = {
    val uploadBuilder: UploadBuilder = mock[UploadBuilder]
    when(uploadBuilder.withMode(any())).thenReturn(uploadBuilder)
    uploadBuilder
  }
}

object DbxFilesSpec {
  val PdfFileName = "clutch-doc.pdf"
  val PdfFileContents: Array[Byte] = Array[Byte](1)
  val PdfZipFileEntry: ZipFileEntry = ZipFileEntry(PdfFileName, PdfFileContents)
  val DbxFileMetadata = new FileMetadata("name", "id", new Date(0), new Date(0), "abcdef012", 0)
  val DbxSessionId = "0123456789"
}
