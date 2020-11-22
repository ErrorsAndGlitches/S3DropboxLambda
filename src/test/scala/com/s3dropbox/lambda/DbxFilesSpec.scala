package com.s3dropbox.lambda

import java.io.{ByteArrayInputStream, InputStream}
import java.util.Date

import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.{DbxUserFilesRequests, FileMetadata, UploadBuilder, WriteMode}
import com.s3dropbox.lambda.DbxFilesSpec.{DbxFileMetadata, PdfFileContents, PdfZipFileEntry, TexFileName}
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class DbxFilesSpec extends AnyFunSpec with MockitoSugar with Matchers {

  describe("when an upload request is made") {
    it("should overwrite the existing file") {
      val uploadBuilder = mockUploadBuilder

      val uploadedContents: Array[Byte] = new Array[Byte](1000)
      when(uploadBuilder.uploadAndFinish(any())).thenAnswer(new Answer[FileMetadata] {
        override def answer(invocation: InvocationOnMock): FileMetadata = {
          invocation.getArgument(0).asInstanceOf[InputStream].read(uploadedContents) shouldBe PdfFileContents.length
          DbxFileMetadata
        }
      })

      val dbxFiles: DbxUserFilesRequests = mock[DbxUserFilesRequests]
      when(dbxFiles.uploadBuilder("/clutch-doc.pdf")).thenReturn(uploadBuilder)

      DbxFiles(mockDbxClient(dbxFiles)).uploadPdf(PdfZipFileEntry)
      verify(uploadBuilder).withMode(WriteMode.OVERWRITE)
    }
  }

  describe("when a delete request is made") {
    it("should delete the file") {
      val dbxFiles: DbxUserFilesRequests = mock[DbxUserFilesRequests]
      DbxFiles(mockDbxClient(dbxFiles)).deletePdf(TexFileName)
      verify(dbxFiles).deleteV2("/clutch-doc.pdf")
    }
  }

  describe("when a manifest upload request is made") {
    it("should overwite the existing manifest") {
      val uploadBuilder = mockUploadBuilder

      val uploadedContents: Array[Byte] = new Array[Byte](1000)
      when(uploadBuilder.uploadAndFinish(any())).thenAnswer(new Answer[FileMetadata] {
        override def answer(invocation: InvocationOnMock): FileMetadata = {
          invocation.getArgument(0).asInstanceOf[InputStream].read(uploadedContents) shouldBe 73
          DbxFileMetadata
        }
      })

      val dbxFiles: DbxUserFilesRequests = mock[DbxUserFilesRequests]
      when(dbxFiles.uploadBuilder("/.manifest")).thenReturn(uploadBuilder)

      DbxFiles(mockDbxClient(dbxFiles)).uploadManifest(Manifest(FileStates(List(FileState("filename", "md5sum")))))
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
  val TexFileName = "clutch-doc.tex"
  val DbxFileMetadata = new FileMetadata("name", "id", new Date(0), new Date(0), "abcdef012", 0)
}
